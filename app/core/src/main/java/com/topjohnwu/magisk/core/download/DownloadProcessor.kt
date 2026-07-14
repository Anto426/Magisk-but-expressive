package com.topjohnwu.magisk.core.download

import android.net.Uri
import com.topjohnwu.magisk.StubApk
import com.topjohnwu.magisk.core.BuildConfig.APP_PACKAGE_NAME
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.isRunningAsStub
import com.topjohnwu.magisk.core.ktx.cachedFile
import com.topjohnwu.magisk.core.ktx.copyAll
import com.topjohnwu.magisk.core.ktx.copyAndClose
import com.topjohnwu.magisk.core.ktx.withInOut
import com.topjohnwu.magisk.core.ktx.writeTo
import com.topjohnwu.magisk.core.tasks.AppMigration
import com.topjohnwu.magisk.core.utils.MediaStoreUtils.outputStream
import com.topjohnwu.magisk.utils.APKInstall
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DownloadProcessor(notifier: DownloadNotifier) : DownloadNotifier by notifier {

    suspend fun handle(stream: InputStream, subject: Subject) {
        when (subject) {
            is Subject.App -> handleApp(stream, subject)
            is Subject.Module -> handleModule(stream, subject.file)
            else -> stream.copyAndClose(subject.file.outputStream())
        }
    }

    suspend fun handleApp(stream: InputStream, subject: Subject.App) {
        if (subject.expectedMbeVersionCode <= 0) {
            throw IOException("Invalid MBE version in update metadata")
        }
        if (isRunningAsStub) {
            val candidateApk = StubApk.createUpdateTemp(context)
            try {
                val external = subject.file.outputStream()
                // Keep partial bytes on a unique path that no stub process will ever load.
                stream.copyAndClose(TeeOutputStream(external, candidateApk.outputStream()))
                StubApk.verifyUpdate(
                    context,
                    candidateApk,
                    StubApk.current(context),
                    APP_PACKAGE_NAME,
                    subject.expectedMbeVersionCode
                )
                // Also upgrade stub
                notifyUpdate(subject.notifyId) {
                    it.setProgress(0, 0, true)
                        .setContentTitle(context.getString(R.string.hide_app_title))
                        .setContentText("")
                }

                // Extract stub
                val apk = context.cachedFile("stub.apk")
                try {
                    ZipFile.Builder().setFile(candidateApk).get().use { zf ->
                        apk.delete()
                        val entry = zf.getEntry("assets/stub.apk")
                            ?: throw IOException("Updated APK is missing assets/stub.apk")
                        zf.getInputStream(entry).writeTo(apk)
                    }

                    // Patch and install before publishing the full APK to another stub process.
                    subject.intent = AppMigration.upgradeStub(context, apk)
                        ?: throw IOException("HideAPK patch error")
                } finally {
                    apk.delete()
                }

                // Atomic handoff: update.apk becomes visible only after download and verification.
                StubApk.verifyAndStageUpdate(
                    context,
                    candidateApk,
                    StubApk.current(context),
                    APP_PACKAGE_NAME,
                    subject.expectedMbeVersionCode
                )
            } catch (e: Exception) {
                candidateApk.delete()
                throw e
            }
        } else {
            val candidateApk = StubApk.createUpdateTemp(context)
            try {
                val external = subject.file.outputStream()
                stream.copyAndClose(TeeOutputStream(external, candidateApk.outputStream()))
                StubApk.verifyUpdate(
                    context,
                    candidateApk,
                    null,
                    APP_PACKAGE_NAME,
                    subject.expectedMbeVersionCode
                )

                val session = APKInstall.startSession(context)
                candidateApk.inputStream().copyAndClose(session.openStream(context))
                subject.intent = session.waitIntent()
            } finally {
                candidateApk.delete()
            }
        }
    }

    suspend fun handleModule(src: InputStream, file: Uri) {
        val tmp = context.cachedFile("module.zip")
        try {
            // First download the entire zip into cache so we can process it
            src.writeTo(tmp)

            val input = ZipFile.Builder().setFile(tmp).get()
            val output = ZipArchiveOutputStream(file.outputStream())
            withInOut(input, output) { zin, zout ->
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/"))
                zout.closeArchiveEntry()
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/"))
                zout.closeArchiveEntry()
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/"))
                zout.closeArchiveEntry()
                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/android/"))
                zout.closeArchiveEntry()

                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/android/update-binary"))
                context.assets.open("module_installer.sh").use { it.copyAll(zout) }
                zout.closeArchiveEntry()

                zout.putArchiveEntry(ZipArchiveEntry("META-INF/com/google/android/updater-script"))
                zout.write("#MAGISK\n".toByteArray())
                zout.closeArchiveEntry()

                // Then simply copy all entries to output
                zin.copyRawEntries(zout) { entry -> !entry.name.startsWith("META-INF") }
            }
        } finally {
            tmp.delete()
        }
    }

    private class TeeOutputStream(
        private val o1: OutputStream,
        private val o2: OutputStream
    ) : OutputStream() {
        override fun write(b: Int) {
            o1.write(b)
            o2.write(b)
        }
        override fun write(b: ByteArray?, off: Int, len: Int) {
            o1.write(b, off, len)
            o2.write(b, off, len)
        }
        override fun close() {
            var failure: Throwable? = null
            try {
                o1.close()
            } catch (e: Throwable) {
                failure = e
            }
            try {
                o2.close()
            } catch (e: Throwable) {
                failure?.addSuppressed(e) ?: run { failure = e }
            }
            failure?.let { throw it }
        }
    }
}
