package com.keiranhaas.getyourlifeback.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Button
import com.keiranhaas.getyourlifeback.MainActivity
import com.keiranhaas.getyourlifeback.data.LifeBackPreferences
import com.keiranhaas.getyourlifeback.data.StepChallengeStore

class FocusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val focusMinutes = LifeBackPreferences.focusMinutes(context)
        val steps = StepChallengeStore.current(context)
        val target = StepChallengeStore.target(context)
        val completed = StepChallengeStore.completed(context)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(android.graphics.Color.rgb(27, 38, 29)))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "LifeBack",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.WHITE)
                    )
                )

                Spacer(GlanceModifier.height(6.dp))

                Text(
                    "Focus time: $focusMinutes min",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.LTGRAY)
                    )
                )

                Spacer(GlanceModifier.height(4.dp))

                Text(
                    if (completed) "Steps: challenge complete"
                    else "Steps: $steps / $target",
                    style = TextStyle(
                        color = ColorProvider(android.graphics.Color.WHITE)
                    )
                )

                Spacer(GlanceModifier.height(12.dp))

                Button(
                    text = "Open LifeBack",
                    onClick = actionStartActivity<MainActivity>()
                )
            }
        }
    }
}

class FocusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FocusWidget()
}
