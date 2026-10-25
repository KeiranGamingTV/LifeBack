
package com.keiranhaas.getyourlifeback.data

import android.content.Context

object LifeBackPreferences {
    private const val FILE = "lifeback_settings"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isOnboarded(context: Context) =
        prefs(context).getBoolean("onboarded", false)

    fun setOnboarded(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("onboarded", value).apply()
    }

    fun selectedApps(context: Context): Set<String> =
        prefs(context).getStringSet("selected_apps", emptySet())
            ?.toSet() ?: emptySet()

    fun setSelectedApps(context: Context, apps: Set<String>) {
        prefs(context).edit()
            .putStringSet("selected_apps", apps.toSet())
            .apply()
    }

    fun breathCount(context: Context) =
        prefs(context).getInt("breath_count", 3)

    fun setBreathCount(context: Context, count: Int) {
        prefs(context).edit()
            .putInt("breath_count", count.coerceIn(1, 10))
            .apply()
    }

    fun sessionMinutes(context: Context) =
        prefs(context).getInt("session_minutes", 5)

    fun setSessionMinutes(context: Context, minutes: Int) {
        prefs(context).edit()
            .putInt("session_minutes", minutes.coerceIn(1, 60))
            .apply()
    }

    fun intervention(context: Context) =
        prefs(context).getString("intervention", "breathing")
            ?: "breathing"

    fun setIntervention(context: Context, value: String) {
        prefs(context).edit().putString("intervention", value).apply()
    }

    fun offHoursEnabled(context: Context) =
        prefs(context).getBoolean("off_hours_enabled", false)

    fun setOffHoursEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean("off_hours_enabled", enabled)
            .apply()
    }

    fun offHoursStart(context: Context) =
        prefs(context).getInt("off_hours_start", 22 * 60)

    fun offHoursEnd(context: Context) =
        prefs(context).getInt("off_hours_end", 7 * 60)

    fun setOffHours(context: Context, start: Int, end: Int) {
        prefs(context).edit()
            .putInt("off_hours_start", start.coerceIn(0, 1439))
            .putInt("off_hours_end", end.coerceIn(0, 1439))
            .apply()
    }

    fun recordBypass(context: Context) {
        val p = prefs(context)
        p.edit()
            .putInt("bypasses", p.getInt("bypasses", 0) + 1)
            .apply()
    }

    fun bypassCount(context: Context) =
        prefs(context).getInt("bypasses", 0)

    fun addFocusMinutes(context: Context, minutes: Int) {
        val p = prefs(context)
        p.edit()
            .putInt(
                "focus_minutes",
                p.getInt("focus_minutes", 0) + minutes
            )
            .apply()
    }

    fun focusMinutes(context: Context) =
        prefs(context).getInt("focus_minutes", 0)

    fun sessionStart(context: Context, packageName: String): Long =
        prefs(context).getLong("session_$packageName", 0L)

    fun setSessionStart(
        context: Context,
        packageName: String,
        timestamp: Long
    ) {
        prefs(context).edit()
            .putLong("session_$packageName", timestamp)
            .apply()
    }

    fun clearSession(context: Context, packageName: String) {
        prefs(context).edit().remove("session_$packageName").apply()
    }
}
