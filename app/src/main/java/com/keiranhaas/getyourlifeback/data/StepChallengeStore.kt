
package com.keiranhaas.getyourlifeback.data

import android.content.Context

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

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun target(context: Context): Int =
        prefs(context).getInt("target", 100)

    fun setTarget(context: Context, target: Int) {
        prefs(context).edit()
            .putInt("target", target.coerceIn(10, 10000))
            .apply()
    }

    fun baseline(context: Context): Int =
        prefs(context).getInt("baseline", -1)

    fun current(context: Context): Int =
        prefs(context).getInt("current", 0)

    fun completed(context: Context): Boolean =
        prefs(context).getBoolean("completed", false)

    fun start(context: Context, sensorTotal: Int) {
        prefs(context).edit()
            .putInt("baseline", sensorTotal)
            .putInt("current", 0)
            .putBoolean("completed", false)
            .putBoolean("active", true)
            .apply()
    }

    fun isActive(context: Context): Boolean =
        prefs(context).getBoolean("active", false)

    fun update(context: Context, sensorTotal: Int): StepChallenge {
        val p = prefs(context)
        val baseline = p.getInt("baseline", sensorTotal)
        val target = p.getInt("target", 100)

        // A sensor reset, such as a device reboot, must not create
        // negative progress or accidentally complete the challenge.
        val current = (sensorTotal - baseline).coerceAtLeast(0)
            .coerceAtMost(target)

        val completed = current >= target

        p.edit()
            .putInt("current", current)
            .putBoolean("completed", completed)
            .putBoolean("active", !completed)
            .apply()

        return StepChallenge(target, baseline, current, completed)
    }

    fun finish(context: Context) {
        prefs(context).edit()
            .putBoolean("active", false)
            .apply()
    }
}
