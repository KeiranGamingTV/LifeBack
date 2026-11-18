package com.keiranhaas.getyourlifeback.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

data class StepChallenge(
    val target: Int,
    val baseline: Int,
    val current: Int,
    val completed: Boolean
) {
    val progress: Float
        get() = (current.toFloat() / target.coerceAtLeast(1))
            .coerceIn(0f, 1f)

    val remaining: Int
        get() = (target - current).coerceAtLeast(0)
}

object StepChallengeStore {
    private const val FILE = "lifeback_steps"

    // How long an earned unlock stays claimable after the challenge ends.
    private const val UNLOCK_VALID_MS = 30 * 60_000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(
            context.contentResolver, Settings.Global.BOOT_COUNT, 0
        )

    // The target the running challenge started with, so changing the
    // setting mid-challenge can't move the goalposts.
    private fun challengeTarget(p: SharedPreferences): Int =
        p.getInt("challenge_target", p.getInt("target", 100))

    // ---- settings ----

    fun target(context: Context): Int =
        prefs(context).getInt("target", 100)

    fun setTarget(context: Context, target: Int) {
        prefs(context).edit()
            .putInt("target", target.coerceIn(10, 10000))
            .apply()
    }

    // ---- state ----

    fun baseline(context: Context): Int =
        prefs(context).getInt("baseline", -1)

    fun current(context: Context): Int =
        prefs(context).getInt("current", 0)

    fun completed(context: Context): Boolean =
        prefs(context).getBoolean("completed", false)

    fun isActive(context: Context): Boolean =
        prefs(context).getBoolean("active", false)

    fun targetPackage(context: Context): String =
        prefs(context).getString("target_package", "").orEmpty()

    fun snapshot(context: Context): StepChallenge {
        val p = prefs(context)
        val target = challengeTarget(p)
        return StepChallenge(
            target = target,
            baseline = p.getInt("baseline", -1),
            current = p.getInt("current", 0).coerceAtMost(target),
            completed = p.getBoolean("completed", false)
        )
    }

    /**
     * Starts a challenge. The baseline is taken from the first sensor
     * reading the tracking service receives (baseline = -1 until then).
     */
    fun start(context: Context, targetPackage: String) {
        prefs(context).edit()
            .putInt("challenge_target", target(context))
            .putInt("baseline", -1)
            .putInt("carried", 0)
            .putInt("current", 0)
            .putInt("boot_count", bootCount(context))
            .putBoolean("completed", false)
            .putBoolean("active", true)
            .putString("target_package", targetPackage)
            .remove("unlock_pkg")
            .remove("unlock_at")
            .apply()
    }

    fun update(context: Context, sensorTotal: Int): StepChallenge {
        val p = prefs(context)
        if (!p.getBoolean("active", false)) return snapshot(context)

        val target = challengeTarget(p)
        val boot = bootCount(context)
        var baseline = p.getInt("baseline", -1)
        var carried = p.getInt("carried", 0)

        if (baseline < 0) {
            baseline = sensorTotal
        } else if (boot != p.getInt("boot_count", boot) ||
            sensorTotal < baseline
        ) {
            // The phone rebooted and the hardware counter restarted at 0.
            // Keep the progress already earned and count from 0 again.
            carried = p.getInt("current", 0)
            baseline = 0
        }

        val current = (carried + (sensorTotal - baseline)).coerceIn(0, target)
        val completed = current >= target

        val edit = p.edit()
            .putInt("baseline", baseline)
            .putInt("carried", carried)
            .putInt("boot_count", boot)
            .putInt("current", current)
            .putBoolean("completed", completed)
            .putBoolean("active", !completed)

        if (completed) {
            val pkg = p.getString("target_package", "").orEmpty()
            if (pkg.isNotBlank()) {
                edit.putString("unlock_pkg", pkg)
                    .putLong("unlock_at", System.currentTimeMillis())
            }
        }
        edit.apply()

        return StepChallenge(target, baseline, current, completed)
    }

    fun finish(context: Context) {
        prefs(context).edit()
            .putBoolean("active", false)
            .apply()
    }

    /** True once if [packageName] has an earned, unexpired unlock. */
    fun consumeUnlock(context: Context, packageName: String): Boolean {
        val p = prefs(context)
        val pending = p.getString("unlock_pkg", "").orEmpty()
        val at = p.getLong("unlock_at", 0L)

        if (pending != packageName ||
            System.currentTimeMillis() - at > UNLOCK_VALID_MS
        ) return false

        p.edit().remove("unlock_pkg").remove("unlock_at").apply()
        return true
    }

    // ---- change observation (used by the challenge screen) ----

    fun registerListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = prefs(context).registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
}