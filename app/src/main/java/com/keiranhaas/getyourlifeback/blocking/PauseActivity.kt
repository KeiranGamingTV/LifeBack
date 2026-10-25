
package com.keiranhaas.getyourlifeback.blocking

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.keiranhaas.getyourlifeback.data.LifeBackPreferences
import com.keiranhaas.getyourlifeback.ui.LifeBackTheme
import com.keiranhaas.getyourlifeback.steps.StepChallengeActivity
import kotlinx.coroutines.delay

class PauseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val target = intent.getStringExtra("target_package").orEmpty()
        val offHours = intent.getBooleanExtra("off_hours", false)

        setContent {
            LifeBackTheme {
                PauseScreen(
                    targetPackage = target,
                    offHours = offHours,
                    onContinue = { minutes ->
                        val prefs = getSharedPreferences(
                            "lifeback_settings", MODE_PRIVATE
                        )

                        prefs.edit()
                            .putLong(
                                "allowed_until_$target",
                                System.currentTimeMillis() + minutes * 60_000L
                            )
                            .apply()

                        LifeBackPreferences.setSessionStart(
                            this, target, System.currentTimeMillis()
                        )

                        finish()
                    },
                    onReturn = { finish() }
                )
            }
        }
    }
}

@Composable
private fun PauseScreen(
    targetPackage: String,
    offHours: Boolean,
    onContinue: (Int) -> Unit,
    onReturn: () -> Unit
) {
    var breaths by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(5) }
    var reason by remember { mutableStateOf("") }
    var breathing by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val requiredBreaths = LifeBackPreferences.breathCount(context)
    val intervention = LifeBackPreferences.intervention(context)
    val completed = when (intervention) {
        "typing" -> reason.trim().length >= 20
        "breathing" -> breaths >= requiredBreaths
        else -> false
    }

    LaunchedEffect(breathing) {
        if (breathing) {
            while (seconds > 0) {
                delay(1000)
                seconds--
            }
            breaths++
            seconds = 5
            breathing = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("L I F E B A C K", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(28.dp))
            Text(
                if (offHours) "Your off-hours have started"
                else "Take a moment",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (offHours) {
                    "This app is blocked during your scheduled rest time."
                } else {
                    "You opened $targetPackage. Is this how you want to spend your time?"
                },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(28.dp))

            if (!offHours) {
                when (intervention) {
                    "typing" -> {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Why do you need this app?") },
                            supportingText = { Text("Write at least 20 characters.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    "breathing" -> {
                        Text(
                            if (breathing) "Breathe slowly… $seconds"
                            else "$breaths of $requiredBreaths breaths completed",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { if (!breathing && breaths < requiredBreaths) breathing = true },
                            enabled = !breathing && breaths < requiredBreaths
                        ) {
                            Text(if (breathing) "Breathe…" else "Complete a breath")
                        }
                    }

										"steps" -> {
											  Text(
											      "Walk ${LifeBackPreferences.sessionMinutes(context) * 20} steps " +
											          "to earn your app time.",
											      style = MaterialTheme.typography.bodyLarge
											  )
											  Spacer(Modifier.height(12.dp))
											  Button(
											      onClick = {
											          context.startActivity(
											              android.content.Intent(
											                  context,
											                  StepChallengeActivity::class.java
											              ).putExtra("target_package", targetPackage)
											          )
											      },
											      modifier = Modifier.fillMaxWidth()
											  ) {
											      Text("Start step challenge")
											  }
										}

                    else -> {
                        Text(
                            "Complete your chosen challenge before continuing.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "This challenge needs its matching sensor or camera module enabled.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onContinue(LifeBackPreferences.sessionMinutes(context)) },
                    enabled = completed && intervention != "steps",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue mindfully")
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onReturn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go back")
            }
        }
    }
}
