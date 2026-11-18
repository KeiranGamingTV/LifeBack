package com.keiranhaas.getyourlifeback.steps

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.keiranhaas.getyourlifeback.data.StepChallengeStore

class StepBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!StepChallengeStore.isActive(context)) return
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // If Android refuses a background start, the challenge resumes
        // the next time the challenge screen is opened.
        try {
            StepTrackingService.start(context)
        } catch (_: Exception) {
        }
    }
}