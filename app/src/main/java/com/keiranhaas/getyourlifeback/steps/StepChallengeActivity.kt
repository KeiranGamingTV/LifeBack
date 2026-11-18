package com.keiranhaas.getyourlifeback.steps

import android.Manifest
import android.app.Activity
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.keiranhaas.getyourlifeback.data.StepChallenge
import com.keiranhaas.getyourlifeback.data.StepChallengeStore
import com.keiranhaas.getyourlifeback.ui.LifeBackTheme
import com.keiranhaas.getyourlifeback.util.goHome

class StepChallengeActivity : ComponentActivity() {

    private var permissionMissing by mutableStateOf(false)
    private var sensorUnavailable by mutableStateOf(false)
    private var challenge by mutableStateOf<StepChallenge?>(null)

    // True once this screen has shown a running challenge, so a finished
    // one is only reported to the screen that was watching it.
    private var watching = false
    private var handledCompletion = false

    private val targetPackage: String
        get() = intent.getStringExtra("target_package").orEmpty()

    // SharedPreferences keeps only a weak reference, so hold it in a field.
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }

    private val permissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionMissing = !granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sensors = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorUnavailable =
            sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null
        permissionMissing = !hasActivityPermission()

        setContent {
            LifeBackTheme {
                StepChallengeScreen(
                    target = challenge?.target ?: StepChallengeStore.target(this),
                    challenge = challenge,
                    sensorUnavailable = sensorUnavailable,
                    permissionMissing = permissionMissing,
                    onRequestPermission = {
                        permissionRequest.launch(
                            Manifest.permission.ACTIVITY_RECOGNITION
                        )
                    },
                    onStart = { beginChallenge() },
                    onCancel = { cancelOrClose() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        visible = true
        StepChallengeStore.registerListener(this, prefListener)
        reload()
    }

    override fun onResume() {
        super.onResume()
        permissionMissing = !hasActivityPermission()
    }

    override fun onStop() {
        visible = false
        StepChallengeStore.unregisterListener(this, prefListener)
        super.onStop()
    }

    private fun hasActivityPermission() =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

    private fun beginChallenge() {
        if (!hasActivityPermission()) {
            permissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            return
        }
        watching = true
        handledCompletion = false
        StepChallengeStore.start(this, targetPackage)
        StepTrackingService.start(this)
        reload()
    }

    private fun reload() {
        val stored = StepChallengeStore.targetPackage(this)
        val sameTarget = targetPackage.isBlank() || targetPackage == stored
        val active = StepChallengeStore.isActive(this) && sameTarget

        if (active) {
            watching = true
            challenge = StepChallengeStore.snapshot(this)
        } else if (watching) {
            val snap = StepChallengeStore.snapshot(this)
            challenge = snap
            if (snap.completed) onCompleted()
        } else {
            challenge = null
        }
    }

    private fun onCompleted() {
        if (handledCompletion) return
        handledCompletion = true

        val pkg = StepChallengeStore.targetPackage(this)
        if (pkg.isBlank()) return // started from the Focus tab; stay on "complete"

        // AppBlockingService turns the earned unlock into an allowed
        // session when this app comes to the foreground.
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun cancelOrClose() {
        val done = challenge?.completed == true
        if (!done) {
            StepChallengeStore.finish(this)
            StepTrackingService.stop(this)
            setResult(Activity.RESULT_CANCELED)
        }
        // Cancelling a challenge that was blocking an app sends the user
        // home instead of back into the blocked app.
        if (!done && targetPackage.isNotBlank()) goHome()
        finish()
    }

    companion object {
        @Volatile
        var visible = false
            private set
    }
}

@Composable
private fun StepChallengeScreen(
    target: Int,
    challenge: StepChallenge?,
    sensorUnavailable: Boolean,
    permissionMissing: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("L I F E B A C K", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(24.dp))
        Text("Walk to unlock", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Take $target steps to complete this challenge.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(28.dp))

        when {
            challenge != null -> {
                Text(
                    "${challenge.current} / ${challenge.target}",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { challenge.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        challenge.completed -> "Challenge complete!"
                        challenge.baseline < 0 -> "Waiting for the first step reading…"
                        else -> "${challenge.remaining} steps to go"
                    }
                )
            }

            permissionMissing -> {
                Text("LifeBack needs permission to recognize physical activity.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRequestPermission) {
                    Text("Grant activity permission")
                }
            }

            sensorUnavailable -> {
                Text(
                    "This device does not expose a compatible step-counter sensor. " +
                        "Try a different challenge type instead."
                )
            }

            else -> {
                Text("Ready when you are.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStart) {
                    Text("Start walking challenge")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onCancel) {
            Text(if (challenge?.completed == true) "Done" else "Cancel")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "You can leave LifeBack or turn off the screen. Your steps keep " +
                "counting in the background and you'll get a notification when " +
                "you're done.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}