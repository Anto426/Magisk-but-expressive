package com.topjohnwu.magisk.runtime

import com.topjohnwu.magisk.core.BuildConfig
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.ktx.await
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MagiskInstallState {
    NotInstalled,
    Installed,
    Outdated,
    Unsupported
}

data class MagiskRuntimeState(
    val isRooted: Boolean,
    val installState: MagiskInstallState,
    val envVersionName: String,
    val envVersionCode: Int,
    val envDebug: Boolean,
    val managerVersionName: String,
    val managerVersionCode: Int,
    val managerDebug: Boolean,
    val isEmulator: Boolean,
    val isSystemAsRoot: Boolean,
    val isFileBasedEncrypted: Boolean,
    val hasRamdisk: Boolean,
    val isAB: Boolean,
    val isZygiskEnabled: Boolean,
    val canShowSuperuser: Boolean,
    val canMigrateApp: Boolean,
    val canShowMagiskSettings: Boolean,
    val canShowMagiskAdvancedSettings: Boolean,
    val canShowDenyListConfig: Boolean,
    val canInstallToInactiveSlot: Boolean,
    val shouldSkipInstallOptions: Boolean
) {
    val isInstalled: Boolean get() = installState != MagiskInstallState.NotInstalled
    val isOutdated: Boolean get() = installState == MagiskInstallState.Outdated
    val isUnsupported: Boolean get() = installState == MagiskInstallState.Unsupported

    val installedMagiskVersion: String
        get() = if (isInstalled) {
            "$envVersionName ($envVersionCode)" + if (envDebug) " (D)" else ""
        } else {
            ""
        }

    val installedManagerVersion: String
        get() = "$managerVersionName ($managerVersionCode)" + if (managerDebug) " (D)" else ""
}

object MagiskRuntimeEngine {

    fun snapshot(): MagiskRuntimeState {
        val env = Info.env
        val installState = when {
            env.isUnsupported -> MagiskInstallState.Unsupported
            !env.isActive -> MagiskInstallState.NotInstalled
            env.versionCode < BuildConfig.APP_VERSION_CODE -> MagiskInstallState.Outdated
            else -> MagiskInstallState.Installed
        }
        val isRooted = Info.isRooted
        val canInstallToInactiveSlot = isRooted && Info.isAB && !Info.isEmulator

        return MagiskRuntimeState(
            isRooted = isRooted,
            installState = installState,
            envVersionName = env.versionString,
            envVersionCode = env.versionCode,
            envDebug = env.isDebug,
            managerVersionName = BuildConfig.MBE_VERSION_NAME,
            managerVersionCode = BuildConfig.MBE_VERSION_CODE,
            managerDebug = BuildConfig.DEBUG,
            isEmulator = Info.isEmulator,
            isSystemAsRoot = Info.isSAR,
            isFileBasedEncrypted = Info.isFDE,
            hasRamdisk = Info.ramdisk,
            isAB = Info.isAB,
            isZygiskEnabled = Info.isZygiskEnabled,
            canShowSuperuser = Info.showSuperUser,
            canMigrateApp = env.isActive && Const.USER_ID == 0,
            canShowMagiskSettings = env.isActive,
            canShowMagiskAdvancedSettings = env.isActive && Const.Version.atLeast_24_0(),
            canShowDenyListConfig = Const.Version.atLeast_24_0(),
            canInstallToInactiveSlot = canInstallToInactiveSlot,
            shouldSkipInstallOptions = Info.isEmulator || (Info.isSAR && !Info.isFDE && Info.ramdisk)
        )
    }

    suspend fun hasRootShell(): Boolean = withContext(Dispatchers.IO) {
        Shell.getShell().isRoot
    }

    suspend fun checkEnvironment(state: MagiskRuntimeState = snapshot()): Int {
        if (!state.isInstalled || state.isUnsupported) return 0
        val cmd = "env_check ${state.envVersionName} ${state.envVersionCode}"
        return withContext(Dispatchers.IO) {
            runCatching { Shell.cmd(cmd).await().code }.getOrDefault(0)
        }
    }

    fun requiresRoot(action: String): Boolean {
        return action == Const.Value.FLASH_ZIP ||
                action == Const.Value.FLASH_MULTIPLE_ZIPS ||
                action == Const.Value.UNINSTALL ||
                action == Const.Value.FLASH_MAGISK ||
                action == Const.Value.FLASH_INACTIVE_SLOT
    }

}
