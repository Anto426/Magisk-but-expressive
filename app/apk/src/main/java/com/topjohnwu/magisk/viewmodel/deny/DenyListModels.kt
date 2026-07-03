package com.topjohnwu.magisk.viewmodel.deny

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.pm.PackageManager.GET_PROVIDERS
import android.content.pm.PackageManager.GET_RECEIVERS
import android.content.pm.PackageManager.GET_SERVICES
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.core.os.ProcessCompat
import com.topjohnwu.magisk.arch.MATCH_UNINSTALLED_PACKAGES_COMPAT
import com.topjohnwu.magisk.core.AppContext
import com.topjohnwu.magisk.core.ktx.getLabel
import java.util.Locale
import java.util.TreeSet
import com.topjohnwu.magisk.core.R as CoreR

class CmdlineListItem(line: String) {
    val packageName: String
    val process: String
    val key: String

    init {
        packageName = line.substringBefore('|')
        process = line.substringAfter('|', packageName)
        key = denyListKey(packageName, process)
    }
}

const val ISOLATED_MAGIC = "isolated"

internal fun denyListKey(packageName: String, process: String): String = "$packageName\n$process"

@SuppressLint("InlinedApi")
class AppProcessInfo(
    private val info: ApplicationInfo, pm: PackageManager, private val denyListKeys: Set<String>
) : Comparable<AppProcessInfo> {

    val label = info.getLabel(pm)
    val packageName: String get() = info.packageName
    var firstInstallTime: Long = 0L
        private set
    var lastUpdateTime: Long = 0L
        private set
    val processes = fetchProcesses(pm)

    override fun compareTo(other: AppProcessInfo) = comparator.compare(this, other)

    fun isSystemApp() = info.flags and ApplicationInfo.FLAG_SYSTEM != 0

    fun isApp() = ProcessCompat.isApplicationUid(info.uid)

    private fun createProcess(name: String, pkg: String = info.packageName) =
        ProcessInfo(name, pkg, denyListKeys.contains(denyListKey(pkg, name)))

    private fun ComponentInfo.getProcName(): String =
        processName ?: applicationInfo.processName ?: applicationInfo.packageName

    private val ServiceInfo.isIsolated get() = (flags and ServiceInfo.FLAG_ISOLATED_PROCESS) != 0
    private val ServiceInfo.useAppZygote get() = (flags and ServiceInfo.FLAG_USE_APP_ZYGOTE) != 0

    private fun Array<out ComponentInfo>?.toProcessList() =
        orEmpty().map { createProcess(it.getProcName()) }

    private fun Array<ServiceInfo>?.toProcessList() = orEmpty().map {
        if (it.isIsolated) {
            if (it.useAppZygote) {
                val proc = info.processName ?: info.packageName
                createProcess("${proc}_zygote")
            } else {
                val proc =
                    if (SDK_INT >= Build.VERSION_CODES.Q) "${it.getProcName()}:${it.name}" else it.getProcName()
                createProcess(proc, ISOLATED_MAGIC)
            }
        } else {
            createProcess(it.getProcName())
        }
    }

    private fun fetchProcesses(pm: PackageManager): Collection<ProcessInfo> {
        val flag =
            MATCH_DISABLED_COMPONENTS or MATCH_UNINSTALLED_PACKAGES_COMPAT or GET_ACTIVITIES or GET_SERVICES or GET_RECEIVERS or GET_PROVIDERS
        val packageInfo = try {
            pm.getPackageInfo(info.packageName, flag)
        } catch (_: Exception) {
            pm.getPackageArchiveInfo(info.sourceDir, flag) ?: return emptyList()
        }

        firstInstallTime = packageInfo.firstInstallTime
        lastUpdateTime = packageInfo.lastUpdateTime

        val processSet = TreeSet<ProcessInfo>(compareBy({ it.name }, { it.isIsolated }))
        processSet += packageInfo.activities.toProcessList()
        processSet += packageInfo.services.toProcessList()
        processSet += packageInfo.receivers.toProcessList()
        processSet += packageInfo.providers.toProcessList()
        return processSet
    }

    companion object {
        private val comparator =
            compareBy<AppProcessInfo>({ it.label.lowercase(Locale.ROOT) }, { it.packageName })
    }
}

data class ProcessInfo(
    val name: String, val packageName: String, val isEnabled: Boolean
) {
    val isIsolated = packageName == ISOLATED_MAGIC
    val isAppZygote = name.endsWith("_zygote")
}

data class DenyListProcessUi(
    val name: String,
    val packageName: String,
    val isIsolated: Boolean,
    val isAppZygote: Boolean,
    val defaultSelection: Boolean,
    val enabled: Boolean
) {
    val displayName: String =
        if (isIsolated) AppContext.getString(CoreR.string.isolated_process_label, name) else name
}

data class DenyListAppUi(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isAppUid: Boolean,
    val expanded: Boolean,
    val processes: List<DenyListProcessUi>,
    val checkedCount: Int,
    val selectionState: SelectionState,
    val sortKey: String,
    val searchKey: String
) {
    val checkedPercent: Float
        get() = if (processes.isEmpty()) 0f else checkedCount.toFloat() / processes.size

    enum class SelectionState {
        Checked, Unchecked, Indeterminate
    }

    companion object {
        fun create(
            packageName: String,
            label: String,
            isSystem: Boolean,
            isAppUid: Boolean,
            expanded: Boolean,
            processes: List<DenyListProcessUi>
        ): DenyListAppUi {
            val sortKey = label.lowercase(Locale.ROOT)
            val searchKey = buildString {
                append(label.lowercase(Locale.ROOT))
                append('\n')
                append(packageName.lowercase(Locale.ROOT))
                processes.forEach {
                    append('\n')
                    append(it.name.lowercase(Locale.ROOT))
                }
            }
            return DenyListAppUi(
                packageName = packageName,
                label = label,
                isSystem = isSystem,
                isAppUid = isAppUid,
                expanded = expanded,
                processes = processes,
                checkedCount = processes.count { it.enabled },
                selectionState = deriveSelectionState(processes),
                sortKey = sortKey,
                searchKey = searchKey
            )
        }

        fun deriveSelectionState(processes: List<DenyListProcessUi>): SelectionState {
            val checked = processes.count { it.enabled }
            return when {
                checked == 0 -> SelectionState.Unchecked
                checked == processes.size -> SelectionState.Checked
                else -> SelectionState.Indeterminate
            }
        }
    }
}
