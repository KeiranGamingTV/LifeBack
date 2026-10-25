package com.keiranhaas.getyourlifeback.steps

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
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

class StepChallengeActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var sensorReady by mutableStateOf(false)
    private var sensorUnavailable by mutableStateOf(false)
    private var permissionMissing by mutableStateOf(false)
    private var sensorTotal by mutableIntStateOf(0)
    private var challenge by mutableStateOf<StepChallenge?>(null)
		private var awaitingBaseline = false

    private val targetPackage: String
        get() = intent.getStringExtra("target_package").orEmpty()

    private val permissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionMissing = !granted
            if (granted) prepareSensor()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        setContent {
            LifeBackTheme {
                StepChallengeScreen(
                    target = StepChallengeStore.target(this),
                    challenge = challenge,
                    sensorReady = sensorReady,
                    sensorUnavailable = sensorUnavailable,
                    permissionMissing = permissionMissing,
                    onRequestPermission = { requestActivityPermission() },
                    onStart = { startChallenge() },
                    onCancel = {
                        StepChallengeStore.finish(this)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }

        if (hasActivityPermission()) {
            prepareSensor()
        } else {
            permissionMissing = true
        }
    }

    private fun hasActivityPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            prepareSensor()
        }
    }

    private fun prepareSensor() {
        permissionMissing = false
        val sensor = stepSensor

        if (sensor == null) {
            sensorUnavailable = true
            sensorReady = false
            return
        }

        sensorUnavailable = false
        sensorReady = true
    }

    private fun startChallenge() {
	    if (!hasActivityPermission()) {
	        requestActivityPermission()
	        return
	    }
	
	    val sensor = stepSensor
	    if (sensor == null) {
	        sensorUnavailable = true
	        return
	    }
	
	    awaitingBaseline = true
	    challenge = null
	
	    if (!sensorManager.registerListener(
	            this, sensor, SensorManager.SENSOR_DELAY_NORMAL
	        )
	    ) {
	        awaitingBaseline = false
	        sensorUnavailable = true
	    }
	}

    override fun onResume() {
        super.onResume()
        if (hasActivityPermission()) prepareSensor()
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent?) {
	    if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
	
	    val total = event.values.firstOrNull()?.toInt() ?: return
	    sensorTotal = total
	
	    if (awaitingBaseline) {
	        awaitingBaseline = false
	        StepChallengeStore.start(this, total)
	        challenge = StepChallengeStore.update(this, total)
	        return
	    }
	
	    if (!StepChallengeStore.isActive(this)) return
	
	    challenge = StepChallengeStore.update(this, total)
	
	    if (challenge?.completed == true) {
	        sensorManager.unregisterListener(this)
	        unlockTargetApp()
	    }
	}

	private fun unlockTargetApp() {
	    val pkg = targetPackage
	
	    if (pkg.isNotBlank()) {
	        val allowance = getSharedPreferences(
	            "lifeback_settings", MODE_PRIVATE
	        )
	
	        val minutes = com.keiranhaas.getyourlifeback.data
	            .LifeBackPreferences.sessionMinutes(this)
	
	        allowance.edit()
	            .putLong(
	                "allowed_until_$pkg",
	                System.currentTimeMillis() + minutes * 60_000L
	            )
	            .apply()
	
	        com.keiranhaas.getyourlifeback.data.LifeBackPreferences
	            .setSessionStart(this, pkg, System.currentTimeMillis())
	
	        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
	        if (launchIntent != null) {
	            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
	            startActivity(launchIntent)
	        }
	    }
	
	    setResult(Activity.RESULT_OK)
	    finish()
	}

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun StepChallengeScreen(
    target: Int,
    challenge: StepChallenge?,
    sensorReady: Boolean,
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
                        "Try a supported step data source instead."
                )
            }

            !sensorReady -> {
                Text("Preparing step counter…")
            }

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
                    if (challenge.completed) "Challenge complete!"
                    else "${challenge.remaining} steps to go"
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
            Text("Cancel")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Keep LifeBack open while walking. Step counting pauses when " +
                "the challenge screen is not active.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
