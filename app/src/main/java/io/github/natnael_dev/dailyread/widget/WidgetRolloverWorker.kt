package io.github.natnael_dev.dailyread.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetRolloverWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "WidgetRolloverWorker executing near 03:00 logical rollover on thread=${Thread.currentThread().name}")

        try {
            DailyReadGlanceWidget().updateAll(applicationContext)
            Log.i(TAG, "Successfully refreshed Glance widget at logical day rollover")
        } catch (e: Exception) {
            Log.w(TAG, "Error refreshing widget during rollover: ${e.message}")
        }

        // Schedule the next day's 03:00 rollover
        WidgetRolloverScheduler.scheduleNextRollover(applicationContext)
        return Result.success()
    }

    companion object {
        const val TAG = "WidgetRolloverWorker"
    }
}
