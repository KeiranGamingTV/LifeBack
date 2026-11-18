package com.keiranhaas.getyourlifeback.steps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.keiranhaas.getyourlifeback.MainActivity
import com.keiranhaas.getyourlifeback.data.StepChallenge
import com.keiranhaas.getyourlifeback.data.StepChallengeStore
import com.keiranhaas.getyourlifeback.widget.FocusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepTrackingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var listening = false
    private var lastUiUpdate = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROGRESS, "Step challenge",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE, "Challenge complete",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always enter the foreground first; Android requires it quickly
        // after startForegroundService().
        try {
            startForeground(
                NOTIF_PROGRESS,
                progressNotification(StepChallengeStore.snapshot(this)),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (!StepChallengeStore.isActive(this) || sensor == null) {
            stopTracking()
            return START_NOT_STICKY
        }

        if (!listening) {
            listening = sensorManager.registerListener(
                this, sensor, SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values.firstOrNull()?.toInt() ?: return

        if (!StepChallengeStore.isActive(this)) {
            stopTracking()
            return
        }

        val challenge = StepChallengeStore.update(this, total)

        if (challenge.completed) {
            onCompleted()
            return
        }

        // Throttle widget + notification refreshes to every 5 seconds.
        val now = SystemClock.elapsedRealtime()
        if (now - lastUiUpdate >= 5_000L) {
            lastUiUpdate = now
            refreshWidget()
            getSystemService(NotificationManager::class.java)
                .notify(NOTIF_PROGRESS, progressNotification(challenge))
        }
    }

    private fun onCompleted() {
        stopListening()
        refreshWidget()

        // If the challenge screen is open it launches the app itself.
        // Otherwise tell the user their unlock is ready.
        if (!StepChallengeActivity.visible) {
            val pkg = StepChallengeStore.targetPackage(this)
            val launch = if (pkg.isNotBlank())
                packageManager.getLaunchIntentForPackage(pkg) else null
            val open = (launch ?: Intent(this, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val pi = PendingIntent.getActivity(
                this, 2, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            getSystemService(NotificationManager::class.java).notify(
                NOTIF_DONE,
                NotificationCompat.Builder(this, CHANNEL_DONE)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Challenge complete")
                    .setContentText(
                        if (launch != null) "Tap to open your app."
                        else "Nice walking."
                    )
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
            )
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopTracking() {
        stopListening()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
        listening = false
    }

    private fun refreshWidget() {
        val app = applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            FocusWidget().updateAll(app)
        }
    }

    private fun progressNotification(c: StepChallenge): Notification {
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, StepChallengeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Walking challenge")
            .setContentText("${c.current} / ${c.target} steps")
            .setProgress(c.target, c.current, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_PROGRESS = "step_progress"
        private const val CHANNEL_DONE = "step_done"
        private const val NOTIF_PROGRESS = 4101
        private const val NOTIF_DONE = 4102

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, StepTrackingService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StepTrackingService::class.java))
        }
    }
}