package com.topjohnwu.magisk.core.model.module

import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.model.ModuleJson
import com.topjohnwu.magisk.core.utils.RootUtils
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.ExtendedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocalModule(
    val base: ExtendedFile,
) : Module() {
    private val svc get() = ServiceLocator.networkService

    override var id: String = ""
    override var name: String = ""
    override var version: String = ""
    override var versionCode: Int = -1
    var author: String = ""
    var description: String = ""
    internal var updateSource: String = ""
        private set

    private val removeFile = base.getChildFile("remove")
    private val disableFile = base.getChildFile("disable")
    private val updateFile = base.getChildFile("update")
    val zygiskFolder = base.getChildFile("zygisk")

    val updated get() = updateFile.exists()
    val isRiru = (id == "riru-core") || base.getChildFile("riru").exists()
    val isZygisk = zygiskFolder.exists()
    val zygiskUnloaded = zygiskFolder.getChildFile("unloaded").exists()
    val hasAction = base.getChildFile("action.sh").exists()

    var enable: Boolean
        get() = !disableFile.exists()
        set(enable) {
            if (enable) {
                disableFile.delete()
                Shell.cmd("copy_preinit_files").submit()
            } else {
                !disableFile.createNewFile()
                Shell.cmd("copy_preinit_files").submit()
            }
        }

    var remove: Boolean
        get() = removeFile.exists()
        set(remove) {
            if (remove) {
                if (updateFile.exists()) return
                removeFile.createNewFile()
                Shell.cmd("copy_preinit_files").submit()
            } else {
                removeFile.delete()
                Shell.cmd("copy_preinit_files").submit()
            }
        }

    @Throws(NumberFormatException::class)
    private fun parseProps(props: List<String>) {
        for (line in props) {
            val prop = line.split("=".toRegex(), 2).map { it.trim() }
            if (prop.size != 2)
                continue

            val key = prop[0]
            val value = prop[1]
            if (key.isEmpty() || key[0] == '#')
                continue

            when (key) {
                "id" -> id = value
                "name" -> name = value
                "version" -> version = value
                "versionCode" -> versionCode = value.toInt()
                "author" -> author = value
                "description" -> description = value
                "updateJson" -> updateSource = value
            }
        }
    }

    init {
        runCatching {
            parseProps(Shell.cmd("dos2unix < $base/module.prop").exec().out)
        }

        if (id.isEmpty()) {
            id = base.name
        }

        if (name.isEmpty()) {
            name = id
        }
    }

    internal val hasUpdateSource: Boolean get() = updateSource.isNotBlank()

    internal suspend fun fetchUpdateMetadata(): ModuleJson {
        return svc.fetchModuleJson(updateSource)
    }

    internal suspend fun fetchUpdateChangelog(url: String): String {
        return svc.fetchString(url)
    }

    companion object {

        fun loaded() = RootUtils.fs.getFile(Const.MODULE_PATH).exists()

        suspend fun installed() = withContext(Dispatchers.IO) {
            val localFiles = RootUtils.fs.getFile(Const.MODULE_PATH)
                .listFiles()
                .orEmpty()
                .filter { !it.isFile && !it.isHidden }
            val localModules = localFiles.map { LocalModule(it) }
            localModules.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
    }
}
