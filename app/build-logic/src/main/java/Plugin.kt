
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Properties
import java.util.Random

// Set non-zero value here to fix the random seed for reproducible builds
// CI builds are always reproducible
val RAND_SEED = if (System.getenv("CI") != null) 42 else 0
lateinit var RANDOM: Random

private val props = Properties()
private const val NATIVE_RELEASE_CACHE_KEY = "magisk.resolvedNativeRelease"
private const val DEFAULT_NATIVE_RELEASE_ROOT = "prebuilt/native/releases"
private const val ANDROID_PACKAGE_VERSION_BASE = 1_000_000L
private val SUPPORTED_NATIVE_ABIS = setOf(
    "armeabi-v7a",
    "arm64-v8a",
    "x86",
    "x86_64",
    "riscv64",
)
private val REQUIRED_NATIVE_BINARIES = setOf(
    "magiskboot",
    "magiskinit",
    "magiskpolicy",
    "magisk",
    "init-ld",
    "busybox",
)

private data class NativeRelease(
    val directory: File,
    val releaseId: String,
    val version: String,
    val versionCode: Int,
    val stubVersion: String,
    val abis: List<String>,
)

object Config {
    operator fun get(key: String): String? {
        val v = props[key] as? String ?: return null
        return v.ifBlank { null }
    }

    fun contains(key: String) = get(key) != null

    val version: String get() = get("version")!!
    val versionCode: Int get() = get("magisk.versionCode")!!.toInt()
    val stubVersion: String get() = get("magisk.stubVersion")!!
    val applicationId: String get() = get("magisk.applicationId") ?: "io.github.vvb2060.magisk"
    val nativeBinariesDir: String get() = get("magisk.nativeBinariesDir")!!
    val mbeVersionName: String get() = get("magisk.mbeVersionName") ?: version
    val mbeVersionCode: Int get() = get("magisk.mbeVersionCode")?.toInt() ?: versionCode
    val packageVersionName: String get() = get("magisk.mbeVersionName") ?: version
    val packageVersionCode: Int
        get() {
            val raw = get("magisk.mbeVersionCode") ?: return versionCode
            val mbeCode = raw.toIntOrNull()
                ?: throw GradleException("magisk.mbeVersionCode must be an integer")
            val packageCode = ANDROID_PACKAGE_VERSION_BASE + mbeCode
            if (mbeCode <= 0 || packageCode > Int.MAX_VALUE) {
                throw GradleException("magisk.mbeVersionCode is outside the Android package range")
            }
            return packageCode.toInt()
        }
    val updateUrl: String get() = get("magisk.updateUrl")
        ?: "https://raw.githubusercontent.com/Anto426/Magisk-deploy/main/update.json"
    val abiList: List<String> get() = get("abiList")!!.split(",")
}

fun Project.rootFile(path: String): File {
    val file = File(path)
    return if (file.isAbsolute) file
    else File(rootProject.file(".."), path)
}

private fun manifestError(manifest: File, message: String): Nothing =
    throw GradleException("Invalid native manifest ${manifest.path}: $message")

private fun JsonObject.requiredString(manifest: File, name: String): String {
    val value = get(name)?.takeUnless { it.isJsonNull }?.runCatching { asString }?.getOrNull()
        ?.trim()
    if (value.isNullOrEmpty()) manifestError(manifest, "missing non-empty '$name'")
    return value
}

private fun JsonObject.requiredPositiveInt(manifest: File, name: String): Int {
    val value = get(name)?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull()
    if (value == null || value <= 0) manifestError(manifest, "'$name' must be a positive integer")
    return value
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return HexFormat.of().formatHex(digest.digest())
}

private fun parseNativeRelease(directory: File): NativeRelease {
    val releaseDir = directory.canonicalFile
    val manifestFile = File(releaseDir, "manifest.json")
    if (!manifestFile.isFile) manifestError(manifestFile, "file does not exist")

    val manifest = try {
        manifestFile.reader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
    } catch (e: Exception) {
        throw GradleException("Invalid native manifest ${manifestFile.path}: malformed JSON", e)
    }
    if (manifest.has("sourceApk")) {
        manifestError(manifestFile, "machine-local 'sourceApk' is not allowed")
    }

    val releaseId = manifest.requiredString(manifestFile, "releaseId")
    if (releaseId != releaseDir.name) {
        manifestError(manifestFile, "releaseId '$releaseId' must match directory '${releaseDir.name}'")
    }
    val version = manifest.requiredString(manifestFile, "version")
    val versionCode = manifest.requiredPositiveInt(manifestFile, "versionCode")
    val stubVersion = manifest.requiredPositiveInt(manifestFile, "stubVersion").toString()

    val abiArray = manifest["abiList"]?.takeIf { it.isJsonArray }?.asJsonArray
        ?: manifestError(manifestFile, "missing 'abiList' array")
    val abis = abiArray.map { element ->
        element.takeUnless { it.isJsonNull }?.runCatching { asString }?.getOrNull()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: manifestError(manifestFile, "abiList contains an invalid ABI")
    }
    if (abis.isEmpty() || abis.size != abis.toSet().size) {
        manifestError(manifestFile, "abiList must be non-empty and contain unique entries")
    }
    val unsupportedAbis = abis.filterNot { it in SUPPORTED_NATIVE_ABIS }
    if (unsupportedAbis.isNotEmpty()) {
        manifestError(manifestFile, "unsupported ABIs: ${unsupportedAbis.joinToString()}")
    }

    val unexpectedRootEntries = releaseDir.listFiles().orEmpty().filter { entry ->
        if (entry.isDirectory) entry.name !in abis else entry.name != "manifest.json"
    }
    if (unexpectedRootEntries.isNotEmpty()) {
        manifestError(
            manifestFile,
            "unexpected release entries: ${unexpectedRootEntries.joinToString { it.name }}"
        )
    }

    val expectedKeys = abis.flatMap { abi ->
        REQUIRED_NATIVE_BINARIES.map { name -> abi to name }
    }.toSet()
    for (abi in abis) {
        val abiDir = File(releaseDir, abi)
        if (!abiDir.isDirectory) manifestError(manifestFile, "missing ABI directory '$abi'")
        val entries = abiDir.listFiles().orEmpty()
        val actualNames = entries.filter { it.isFile }.map { it.name }.toSet()
        val invalidEntries = entries.filter { !it.isFile || it.name !in REQUIRED_NATIVE_BINARIES }
        if (actualNames != REQUIRED_NATIVE_BINARIES || invalidEntries.isNotEmpty()) {
            manifestError(
                manifestFile,
                "ABI '$abi' must contain exactly ${REQUIRED_NATIVE_BINARIES.sorted().joinToString()}"
            )
        }
    }

    val filesArray = manifest["files"]?.takeIf { it.isJsonArray }?.asJsonArray
        ?: manifestError(manifestFile, "missing 'files' array")
    val declaredKeys = mutableSetOf<Pair<String, String>>()
    for (element in filesArray) {
        val entry = element.takeIf { it.isJsonObject }?.asJsonObject
            ?: manifestError(manifestFile, "files contains a non-object entry")
        if (entry.has("path")) {
            manifestError(manifestFile, "machine/repository-relative 'path' fields are not allowed")
        }
        val abi = entry.requiredString(manifestFile, "abi")
        val name = entry.requiredString(manifestFile, "name")
        val key = abi to name
        if (!declaredKeys.add(key)) manifestError(manifestFile, "duplicate file entry '$abi/$name'")
        if (key !in expectedKeys) manifestError(manifestFile, "unexpected file entry '$abi/$name'")

        val expectedHash = entry.requiredString(manifestFile, "sha256").lowercase()
        if (!expectedHash.matches(Regex("[0-9a-f]{64}"))) {
            manifestError(manifestFile, "invalid SHA-256 for '$abi/$name'")
        }
        val expectedSize = entry["size"]?.takeUnless { it.isJsonNull }
            ?.runCatching { asLong }?.getOrNull()
            ?: manifestError(manifestFile, "missing integer size for '$abi/$name'")
        if (expectedSize < 0) manifestError(manifestFile, "negative size for '$abi/$name'")

        val binary = File(File(releaseDir, abi), name)
        if (!binary.isFile) manifestError(manifestFile, "missing binary '$abi/$name'")
        if (binary.length() != expectedSize) {
            manifestError(manifestFile, "size mismatch for '$abi/$name'")
        }
        if (sha256(binary) != expectedHash) {
            manifestError(manifestFile, "SHA-256 mismatch for '$abi/$name'")
        }
    }
    if (declaredKeys != expectedKeys) {
        val missing = (expectedKeys - declaredKeys).sortedBy { "${it.first}/${it.second}" }
        manifestError(manifestFile, "missing file entries: ${missing.joinToString { "${it.first}/${it.second}" }}")
    }

    return NativeRelease(releaseDir, releaseId, version, versionCode, stubVersion, abis)
}

private fun Project.selectNativeRelease(): NativeRelease {
    val explicitDirectory = Config["magisk.nativeBinariesDir"]
    val explicitReleaseId = Config["magisk.nativeReleaseId"]
    if (explicitDirectory != null && explicitReleaseId != null) {
        throw GradleException(
            "Set only one of magisk.nativeBinariesDir or magisk.nativeReleaseId, not both"
        )
    }

    val releasesRoot = rootFile(
        Config["magisk.nativeBinariesRoot"] ?: DEFAULT_NATIVE_RELEASE_ROOT
    ).canonicalFile
    if (explicitDirectory != null) return parseNativeRelease(rootFile(explicitDirectory))
    if (explicitReleaseId != null) {
        if (explicitReleaseId.contains('/') || explicitReleaseId.contains('\\')) {
            throw GradleException("magisk.nativeReleaseId must be a directory name, not a path")
        }
        return parseNativeRelease(File(releasesRoot, explicitReleaseId))
    }
    if (!releasesRoot.isDirectory) {
        throw GradleException("Native release root does not exist: ${releasesRoot.path}")
    }

    val rejected = mutableListOf<String>()
    val releases = releasesRoot.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { dir ->
        try {
            parseNativeRelease(dir)
        } catch (e: GradleException) {
            rejected += "${dir.name}: ${e.message}"
            null
        }
    }
    if (releases.isEmpty()) {
        val details = rejected.takeIf { it.isNotEmpty() }?.joinToString("\n  ", "\n  ").orEmpty()
        throw GradleException("No valid native releases found in ${releasesRoot.path}$details")
    }
    val maxVersionCode = releases.maxOf { it.versionCode }
    val latest = releases.filter { it.versionCode == maxVersionCode }
    if (latest.size != 1) {
        throw GradleException(
            "Multiple valid native releases have versionCode $maxVersionCode: " +
                latest.joinToString { it.releaseId }
        )
    }
    rejected.forEach { logger.warn("Ignoring invalid native release: $it") }
    return latest.single()
}

private fun Project.applyNativeRelease() {
    val extras = rootProject.extensions.extraProperties
    val release = if (extras.has(NATIVE_RELEASE_CACHE_KEY)) {
        extras.get(NATIVE_RELEASE_CACHE_KEY) as NativeRelease
    } else {
        selectNativeRelease().also { extras.set(NATIVE_RELEASE_CACHE_KEY, it) }
    }
    val repositoryRoot = rootProject.file("..").canonicalFile
    val directory = runCatching {
        release.directory.relativeTo(repositoryRoot).invariantSeparatorsPath
    }.getOrElse { release.directory.invariantSeparatorsPath }

    props["version"] = release.version
    props["magisk.versionCode"] = release.versionCode.toString()
    props["magisk.stubVersion"] = release.stubVersion
    props["abiList"] = release.abis.joinToString(",")
    props["magisk.nativeBinariesDir"] = directory
    logger.lifecycle(
        "Using native release ${release.releaseId}: Magisk ${release.version} " +
            "(${release.versionCode}), ABIs ${release.abis.joinToString()}"
    )
}

class MagiskPlugin : Plugin<Project> {
    override fun apply(project: Project) = project.applyPlugin()

    private fun Project.applyPlugin() {
        initRandom(rootProject.file("dict.txt"))
        props.clear()

        // Get gradle properties relevant to Magisk
        props.putAll(properties.filter { (key, _) -> key.startsWith("magisk.") })

        // Load config.prop
        val configPath: String? by this
        val configFile = rootFile(configPath ?: "config.prop")
        if (configFile.exists()) {
            configFile.inputStream().use {
                val config = Properties()
                config.load(it)
                props.putAll(config)
            }
        }

        // Load flags.prop, generated by build.py
        val flagsProp = rootProject.layout.buildDirectory.file("flags.prop").get().asFile
        if (flagsProp.exists()) {
            flagsProp.inputStream().use {
                val flags = Properties()
                flags.load(it)
                props.putAll(flags)
            }
        }

        // The selected immutable manifest is authoritative for all native/core values.
        applyNativeRelease()
    }
}
