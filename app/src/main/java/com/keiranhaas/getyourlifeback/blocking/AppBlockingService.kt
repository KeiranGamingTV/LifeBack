
package com.keiranhaas.getyourlifeback.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.keiranhaas.getyourlifeback.data.LifeBackPreferences
import java.util.Calendar

class AppBlockingService : AccessibilityService() {

    private var lastPackage = ""
    private var lastInterception = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == lastPackage) return

        lastPackage = pkg

        val selected = LifeBackPreferences.selectedApps(this)
        if (pkg !in selected) return

        val now = System.currentTimeMillis()
        if (now - lastInterception < 1200L) return

        val prefs = getSharedPreferences("lifeback_settings", MODE_PRIVATE)
        val allowedUntil = prefs.getLong("allowed_until_$pkg", 0L)

        val offHours = LifeBackPreferences.offHoursEnabled(this) &&
            isWithinOffHours()

        val sessionStart = LifeBackPreferences.sessionStart(this, pkg)
        val maxMillis = LifeBackPreferences.sessionMinutes(this) * 60_000L
        val sessionExpired = sessionStart > 0L &&
            now - sessionStart >= maxMillis

        if (!offHours && !sessionExpired && now < allowedUntil) {
            return
        }

        lastInterception = now

        val intent = Intent(this, PauseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("target_package", pkg)
            putExtra("off_hours", offHours)
        }

        startActivity(intent)
    }

    private fun isWithinOffHours(): Boolean {
        val start = LifeBackPreferences.offHoursStart(this)
        val end = LifeBackPreferences.offHoursEnd(this)

        val calendar = Calendar.getInstance()
        val current = calendar.get(Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(Calendar.MINUTE)

        return if (start == end) {
            true
        } else if (start < end) {
            current in start until end
        } else {
            current >= start || current < end
        }
    }

    override fun onInterrupt() = Unit
}
