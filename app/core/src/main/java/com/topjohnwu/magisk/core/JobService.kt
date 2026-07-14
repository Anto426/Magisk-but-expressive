package com.topjohnwu.magisk.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import com.topjohnwu.magisk.core.base.BaseJobService
import com.topjohnwu.magisk.core.di.ServiceLocator
import com.topjohnwu.magisk.core.download.DownloadEngine
import com.topjohnwu.magisk.core.download.DownloadSession
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class JobService : BaseJobService() {

    private var mSession: Session? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var updateJob: Job? = null

    @TargetApi(value = 34)
    inner class Session(
        private var params: JobParameters
    ) : DownloadSession {

        override val context get() = this@JobService
        val engine = DownloadEngine(this)

        fun updateParams(params: JobParameters) {
            this.params = params
            engine.reattach()
        }

        override fun attachNotification(id: Int, builder: Notification.Builder) {
            setNotification(params, id, builder.build(), JOB_END_NOTIFICATION_POLICY_REMOVE)
        }

        override fun onDownloadComplete() {
            jobFinished(params, false)
        }
    }

    @SuppressLint("NewApi")
    override fun onStartJob(params: JobParameters): Boolean {
        return when (params.jobId) {
            Const.ID.CHECK_UPDATE_JOB_ID -> checkUpdate(params)
            Const.ID.DOWNLOAD_JOB_ID -> downloadFile(params)
            else -> false
        }
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        if (params?.jobId == Const.ID.CHECK_UPDATE_JOB_ID) {
            updateJob?.cancel()
            updateJob = null
            return true
        }
        return false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    @TargetApi(value = 34)
    private fun downloadFile(params: JobParameters): Boolean {
        params.transientExtras.classLoader = Subject::class.java.classLoader
        val subject = params.transientExtras
            .getParcelable(DownloadEngine.SUBJECT_KEY, Subject::class.java) ?:
            return false

        val session = mSession?.also {
            it.updateParams(params)
        } ?: run {
            Session(params).also { mSession = it }
        }

        session.engine.download(subject)
        return true
    }

    private fun checkUpdate(params: JobParameters): Boolean {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            var shouldReschedule = false
            try {
                val refreshed = UpdateManager.refreshAll(
                    service = ServiceLocator.networkService,
                    installedModules = LocalModule.installed(),
                    force = true,
                    publishNotifications = true
                )
                shouldReschedule = !refreshed
                if (!refreshed) {
                    Timber.w("Background update check was incomplete; requesting a retry")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Background update check failed")
                shouldReschedule = true
            } finally {
                if (isActive) {
                    jobFinished(params, shouldReschedule)
                }
                updateJob = null
            }
        }
        return true
    }

    companion object {
        private val UPDATE_INTERVAL_MS = TimeUnit.HOURS.toMillis(12)

        fun schedule(context: Context) {
            val scheduler = context.getSystemService<JobScheduler>() ?: return
            if (Config.checkUpdate) {
                val cmp = JobService::class.java.cmp(context.packageName)
                val existing = scheduler.allPendingJobs.firstOrNull {
                    it.id == Const.ID.CHECK_UPDATE_JOB_ID
                }
                if (existing?.matchesUpdateSchedule(cmp) == true) return

                val info = JobInfo.Builder(Const.ID.CHECK_UPDATE_JOB_ID, cmp)
                    .setPeriodic(UPDATE_INTERVAL_MS)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()
                val result = scheduler.schedule(info)
                if (result != JobScheduler.RESULT_SUCCESS) {
                    Timber.w("Unable to schedule the periodic update job")
                }
            } else {
                scheduler.cancel(Const.ID.CHECK_UPDATE_JOB_ID)
            }
        }

        @Suppress("DEPRECATION")
        private fun JobInfo.matchesUpdateSchedule(component: android.content.ComponentName): Boolean {
            return service == component &&
                isPeriodic &&
                networkType == JobInfo.NETWORK_TYPE_ANY &&
                isPersisted &&
                !isRequireDeviceIdle &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || intervalMillis == UPDATE_INTERVAL_MS)
        }
    }
}
