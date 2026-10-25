
package com.keiranhaas.getyourlifeback.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.app.usage.UsageEvents
import java.util.Calendar

data class AppUsage(
    val packageName: String,
    val foregroundMillis: Long
)

object UsageRepository {

    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE)
            as AppOpsManager

        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun todayUsage(context: Context): List<AppUsage> {
        if (!hasUsageAccess(context)) return emptyList()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()

        val manager = context.getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()

        val starts = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    starts[event.packageName] = event.timeStamp
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val opened = starts.remove(event.packageName)
                    if (opened != null) {
                        totals[event.packageName] =
                            (totals[event.packageName] ?: 0L) +
                            (event.timeStamp - opened).coerceAtLeast(0L)
                    }
                }
            }
        }

        starts.forEach { (pkg, opened) ->
            totals[pkg] = (totals[pkg] ?: 0L) +
                (end - opened).coerceAtLeast(0L)
        }

        return totals.map { (pkg, millis) ->
            AppUsage(pkg, millis)
        }.sortedByDescending { it.foregroundMillis }
    }
}
