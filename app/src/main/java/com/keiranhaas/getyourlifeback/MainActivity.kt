
package com.keiranhaas.getyourlifeback

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.keiranhaas.getyourlifeback.data.LifeBackPreferences
import com.keiranhaas.getyourlifeback.ui.LifeBackApp
import com.keiranhaas.getyourlifeback.ui.LifeBackTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LifeBackTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LifeBackApp(
                        onRequestOverlay = {
                            startActivity(
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            )
                        },
                        onRequestAccessibility = {
                            startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            )
                        },
                        onRequestUsageAccess = {
                            startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            )
                        }
                    )
                }
            }
        }
    }
}
