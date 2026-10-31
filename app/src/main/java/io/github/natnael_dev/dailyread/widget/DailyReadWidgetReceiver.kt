package io.github.natnael_dev.dailyread.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailyReadWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = DailyReadGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.i(TAG, "onReceive: action=$action thread=${Thread.currentThread().name}")

        when (action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            ACTION_TRIGGER_ROLLOVER,
            ACTION_FORCE_UPDATE,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Triggering Glance widget recomputation for action: $action")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        DailyReadGlanceWidget().updateAll(context)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update widget: ${e.message}")
                    }
                }
                // Ensure rollover worker is scheduled
                WidgetRolloverScheduler.scheduleNextRollover(context)
            }
        }
    }

    companion object {
        const val TAG = "DailyReadWidgetReceiver"
        const val ACTION_TRIGGER_ROLLOVER = "io.github.natnael_dev.dailyread.ACTION_TRIGGER_ROLLOVER"
        const val ACTION_FORCE_UPDATE = "io.github.natnael_dev.dailyread.ACTION_FORCE_UPDATE"
    }
}
