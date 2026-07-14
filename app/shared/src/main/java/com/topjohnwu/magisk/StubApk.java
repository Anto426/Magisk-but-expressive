package com.topjohnwu.magisk;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.channels.OverlappingFileLockException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StubApk {
    private static final String MBE_VERSION_CODE_META =
            "com.topjohnwu.magisk.MBE_VERSION_CODE";
    private static File dynDir;
    private static Method addAssetPath;

    private static File getDynDir(ApplicationInfo info) {
        if (dynDir == null) {
            final String dataDir;
            if (SDK_INT >= Build.VERSION_CODES.N) {
                // Use device protected path to allow directBootAware
                dataDir = info.deviceProtectedDataDir;
            } else {
                dataDir = info.dataDir;
            }
            dynDir = new File(dataDir, "dyn");
            dynDir.mkdirs();
        }
        return dynDir;
    }

    public static File current(Context c) {
        return new File(getDynDir(c.getApplicationInfo()), "current.apk");
    }

    public static File current(ApplicationInfo info) {
        return new File(getDynDir(info), "current.apk");
    }

    public static File update(Context c) {
        return new File(getDynDir(c.getApplicationInfo()), "update.apk");
    }

    public static File update(ApplicationInfo info) {
        return new File(getDynDir(info), "update.apk");
    }

    /** Create a process-private candidate path that is never observed by the dynamic loader. */
    public static File createUpdateTemp(Context context) throws IOException {
        var dir = getDynDir(context.getApplicationInfo());
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Unable to create dynamic APK directory");
        }
        return File.createTempFile("update-", ".apk.tmp", dir);
    }

    /**
     * Validate a downloaded full APK before it is installed or used for dynamic loading.
     *
     * <p>The trusted APK is normally the last known-good dynamically loaded APK. For a normal
     * installation, or when that file is unavailable, the installed canonical package is used as
     * the signing identity. A signing-certificate rotation is accepted only when Android exposes
     * the previous signer in the candidate's verified signing history.</p>
     */
    public static void verifyUpdate(
            Context context, File candidate, File trustedApk, String expectedPackage,
            int expectedMbeVersionCode)
            throws IOException {
        verifyZip(candidate);

        var pm = context.getPackageManager();
        var candidateInfo = parseArchive(pm, candidate);
        if (!expectedPackage.equals(candidateInfo.packageName)) {
            throw new IOException("Unexpected APK package: " + candidateInfo.packageName);
        }

        PackageInfo trustedInfo = null;
        // The installed package is the strongest reference for a normal (non-hidden) app.
        if (expectedPackage.equals(context.getPackageName())) {
            trustedInfo = getInstalledPackage(pm, expectedPackage);
        }
        if (trustedInfo == null && trustedApk != null && trustedApk.isFile()
                && !sameFile(candidate, trustedApk)) {
            try {
                var archiveInfo = parseArchive(pm, trustedApk);
                if (expectedPackage.equals(archiveInfo.packageName)) {
                    trustedInfo = archiveInfo;
                }
            } catch (IOException ignored) {
                // Fall back to an installed canonical package, if one still exists.
            }
        }
        if (trustedInfo == null) {
            trustedInfo = getInstalledPackage(pm, expectedPackage);
        }
        if (trustedInfo == null) {
            throw new IOException("No trusted signing certificate is available");
        }
        if (!signaturesCompatible(candidateInfo, trustedInfo)) {
            throw new IOException("APK signing certificate does not match the trusted app");
        }
        verifyMbeVersionCodes(
                mbeVersionCode(candidateInfo),
                mbeVersionCode(trustedInfo),
                expectedMbeVersionCode
        );
    }

    /** Verify and publish a complete candidate for the next stub process as one locked operation. */
    public static void verifyAndStageUpdate(
            Context context, File candidate, File trustedApk, String expectedPackage,
            int expectedMbeVersionCode)
            throws IOException {
        withUpdateLock(context, () -> {
            verifyUpdate(
                    context,
                    candidate,
                    trustedApk,
                    expectedPackage,
                    expectedMbeVersionCode
            );

            var pending = update(context);
            if (pending.isFile() && !sameFile(candidate, pending)) {
                boolean pendingIsTrusted = false;
                try {
                    verifyUpdate(context, pending, trustedApk, expectedPackage, -1);
                    pendingIsTrusted = true;
                } catch (IOException ignored) {
                    // An invalid stale candidate must not block a verified replacement.
                    pending.delete();
                }
                if (pendingIsTrusted) {
                    var pm = context.getPackageManager();
                    verifyMbeVersionCodes(
                            mbeVersionCode(parseArchive(pm, candidate)),
                            mbeVersionCode(parseArchive(pm, pending)),
                            expectedMbeVersionCode
                    );
                }
            }
            atomicReplace(candidate, pending);
        });
    }

    /** Verify and replace the known-good APK as one cross-process locked operation. */
    public static void verifyAndPromoteCandidate(
            Context context, File candidate, File trustedApk, String expectedPackage,
            int expectedMbeVersionCode)
            throws IOException {
        withUpdateLock(context, () -> {
            verifyUpdate(
                    context,
                    candidate,
                    trustedApk,
                    expectedPackage,
                    expectedMbeVersionCode
            );
            atomicReplace(candidate, current(context));
        });
    }

    /** Verify and promote a staged update as one cross-process locked operation. */
    public static void verifyAndPromoteUpdate(Context context, String expectedPackage)
            throws IOException {
        withUpdateLock(context, () -> {
            var pending = update(context);
            var trusted = current(context);
            verifyUpdate(context, pending, trusted, expectedPackage, -1);
            atomicReplace(pending, trusted);
        });
    }

    private static synchronized void withUpdateLock(Context context, LockedOperation operation)
            throws IOException {
        var dir = getDynDir(context.getApplicationInfo());
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Unable to create dynamic APK directory");
        }
        var lockFile = new File(dir, "update.lock");
        try (var file = new RandomAccessFile(lockFile, "rw");
             var channel = file.getChannel();
             var ignored = channel.lock()) {
            operation.run();
        } catch (OverlappingFileLockException e) {
            throw new IOException("Unable to acquire the APK update lock", e);
        }
    }

    private static void atomicReplace(File source, File destination) throws IOException {
        if (!source.isFile() || source.length() == 0) {
            throw new IOException("Verified update APK is missing");
        }

        // Flush the completed download before the same-filesystem atomic rename.
        try (var out = new FileOutputStream(source, true)) {
            out.getFD().sync();
        }
        try {
            Os.rename(source.getPath(), destination.getPath());
        } catch (ErrnoException e) {
            throw new IOException("Unable to promote verified update", e);
        }
    }

    static void verifyZip(File apk) throws IOException {
        if (!apk.isFile() || apk.length() == 0) {
            throw new IOException("Downloaded APK is empty");
        }
        try (var zip = new ZipFile(apk)) {
            var manifest = zip.getEntry("AndroidManifest.xml");
            var dex = zip.getEntry("classes.dex");
            if (!isReadableFile(manifest) || !isReadableFile(dex)) {
                throw new IOException("Downloaded file is not a complete APK");
            }
            // Force both essential entries through the ZIP inflater before package parsing.
            try (var manifestIn = zip.getInputStream(manifest);
                 var dexIn = zip.getInputStream(dex)) {
                if (manifestIn.read() < 0 || dexIn.read() < 0) {
                    throw new IOException("Downloaded APK contains empty essential entries");
                }
            }
        }
    }

    private static boolean isReadableFile(ZipEntry entry) {
        return entry != null && !entry.isDirectory() && entry.getSize() != 0;
    }

    private static PackageInfo parseArchive(PackageManager pm, File apk) throws IOException {
        try {
            var info = pm.getPackageArchiveInfo(
                    apk.getPath(), signingFlags() | PackageManager.GET_META_DATA);
            if (info == null) {
                throw new IOException("Android rejected the downloaded APK");
            }
            if (currentSigners(info).length == 0) {
                throw new IOException("Downloaded APK has no verified signing certificate");
            }
            return info;
        } catch (RuntimeException e) {
            throw new IOException("Unable to parse downloaded APK", e);
        }
    }

    private static PackageInfo getInstalledPackage(PackageManager pm, String packageName)
            throws IOException {
        try {
            var info = pm.getPackageInfo(
                    packageName, signingFlags() | PackageManager.GET_META_DATA);
            if (currentSigners(info).length == 0) {
                throw new IOException("Installed app has no signing certificate");
            }
            return info;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        } catch (RuntimeException e) {
            throw new IOException("Unable to read installed app signing certificate", e);
        }
    }

    @SuppressWarnings("deprecation")
    private static int signingFlags() {
        return SDK_INT >= Build.VERSION_CODES.P
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
    }

    @SuppressWarnings("deprecation")
    private static Signature[] currentSigners(PackageInfo info) {
        if (SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null) {
            var signers = info.signingInfo.getApkContentsSigners();
            return signers != null ? signers : new Signature[0];
        }
        return info.signatures != null ? info.signatures : new Signature[0];
    }

    private static boolean signaturesCompatible(PackageInfo candidate, PackageInfo trusted) {
        var expected = currentSigners(trusted);
        var candidateCurrent = currentSigners(candidate);
        if (expected.length == 0 || candidateCurrent.length == 0) {
            return false;
        }

        if (SDK_INT >= Build.VERSION_CODES.P
                && candidate.signingInfo != null
                && trusted.signingInfo != null
                && !candidate.signingInfo.hasMultipleSigners()
                && !trusted.signingInfo.hasMultipleSigners()) {
            var history = candidate.signingInfo.getSigningCertificateHistory();
            return history != null && containsAll(history, expected);
        }
        return sameSignatureSet(candidateCurrent, expected);
    }

    private static int mbeVersionCode(PackageInfo info) {
        Bundle metadata = info.applicationInfo == null ? null : info.applicationInfo.metaData;
        if (metadata == null) return -1;
        Object value = metadata.get(MBE_VERSION_CODE_META);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    static void verifyMbeVersionCodes(int candidate, int trusted, int expected)
            throws IOException {
        if (candidate <= 0) {
            throw new IOException("Updated APK has no valid MBE version identity");
        }
        if (expected > 0 && candidate != expected) {
            throw new IOException(
                    "Updated APK does not match deployment metadata: " + candidate + " != " + expected);
        }
        if (trusted > 0 && candidate < trusted) {
            throw new IOException(
                    "Updated APK would roll back MBE from " + trusted + " to " + candidate);
        }
    }

    private static boolean containsAll(Signature[] candidates, Signature[] expected) {
        for (var signature : expected) {
            boolean found = false;
            for (var candidate : candidates) {
                if (signature.equals(candidate)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameSignatureSet(Signature[] first, Signature[] second) {
        return first.length == second.length
                && containsAll(first, second)
                && containsAll(second, first);
    }

    private static boolean sameFile(File first, File second) throws IOException {
        return first.getCanonicalFile().equals(second.getCanonicalFile());
    }

    @FunctionalInterface
    private interface LockedOperation {
        void run() throws IOException;
    }

    @TargetApi(Build.VERSION_CODES.R)
    private static ResourcesLoader getResourcesLoader(File path) throws IOException {
        var loader = new ResourcesLoader();
        ResourcesProvider provider;
        if (path.isDirectory()) {
            provider = ResourcesProvider.loadFromDirectory(path.getPath(), null);
        } else {
            var fd = ParcelFileDescriptor.open(path, MODE_READ_ONLY);
            provider = ResourcesProvider.loadFromApk(fd);
        }
        loader.addProvider(provider);
        return loader;
    }

    public static void addAssetPath(Resources res, String path) {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                res.addLoaders(getResourcesLoader(new File(path)));
            } catch (IOException ignored) {}
        } else {
            AssetManager asset = res.getAssets();
            try {
                if (addAssetPath == null)
                    addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
                addAssetPath.invoke(asset, path);
            } catch (Exception ignored) {}
        }
    }

    public static void restartProcess(Activity activity) {
        Intent intent = activity.getPackageManager()
                .getLaunchIntentForPackage(activity.getPackageName());
        activity.finishAffinity();
        activity.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    public static class Data {
        // Indices of the object array
        private static final int STUB_VERSION = 0;
        private static final int CLASS_COMPONENT_MAP = 1;
        private static final int ROOT_SERVICE = 2;
        private static final int ARR_SIZE = 3;

        private final Object[] arr;

        public Data() { arr = new Object[ARR_SIZE]; }
        public Data(Object o) { arr = (Object[]) o; }
        public Object getObject() { return arr; }

        public int getVersion() { return (int) arr[STUB_VERSION]; }
        public void setVersion(int version) { arr[STUB_VERSION] = version; }
        public Map<String, String> getClassToComponent() {
            // noinspection unchecked
            return (Map<String, String>) arr[CLASS_COMPONENT_MAP];
        }
        public void setClassToComponent(Map<String, String> map) {
            arr[CLASS_COMPONENT_MAP] = map;
        }
        public Class<?> getRootService() { return (Class<?>) arr[ROOT_SERVICE]; }
        public void setRootService(Class<?> service) { arr[ROOT_SERVICE] = service; }
    }
}
