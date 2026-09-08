package lk.novalink.zerotrace.core

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background worker that checks for new ZeroTrace releases and configuration updates
 * even when the app is completely closed or device was rebooted.
 */
class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing background periodic update check...")
        return try {
            val updateInfo = UpdateManager.checkForUpdates(applicationContext, isManualCheck = false)
            if (updateInfo != null) {
                Log.d(TAG, "Background update check detected new version: ${updateInfo.versionName} (posted notification)")
            } else {
                Log.d(TAG, "Background update check completed: app is up to date.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background update check encountered error", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UpdateCheckWorker"
        const val WORK_NAME = "ZeroTrace_Periodic_Update_Check"

        /**
         * Schedules periodic background update checks using battery-optimized WorkManager.
         * Runs every 6 hours only when network connectivity is available.
         */
        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            Log.d(TAG, "Registered periodic background update check (every 6h)")
        }
    }
}
