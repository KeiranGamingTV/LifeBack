
package com.keiranhaas.getyourlifeback.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keiranhaas.getyourlifeback.data.LifeBackPreferences
import com.keiranhaas.getyourlifeback.data.UsageRepository

private enum class Page(val label: String) {
    HOME("Today"),
    APPS("Apps"),
    FOCUS("Focus"),
    HISTORY("History"),
    SETTINGS("Settings")
}

@Composable
fun LifeBackApp(
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestUsageAccess: () -> Unit
) {
    val context = LocalContext.current
    var onboarded by remember {
        mutableStateOf(LifeBackPreferences.isOnboarded(context))
    }
    var page by remember { mutableStateOf(Page.HOME) }

    if (!onboarded) {
        OnboardingScreen(
            onRequestOverlay = onRequestOverlay,
            onRequestAccessibility = onRequestAccessibility,
            onRequestUsageAccess = onRequestUsageAccess,
            onFinish = {
                LifeBackPreferences.setOnboarded(context, true)
                onboarded = true
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Page.entries.forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = {
                                Icon(
                                    when (item) {
                                        Page.HOME -> Icons.Default.Home
                                        Page.APPS -> Icons.Default.Apps
                                        Page.FOCUS -> Icons.Default.Timer
                                        Page.HISTORY -> Icons.Default.BarChart
                                        Page.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.HOME -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    onFocus = { page = Page.FOCUS }
                )
                Page.APPS -> AppsScreen(Modifier.padding(padding))
                Page.FOCUS -> FocusScreen(Modifier.padding(padding))
                Page.HISTORY -> HistoryScreen(Modifier.padding(padding))
                Page.SETTINGS -> SettingsScreen(
                    Modifier.padding(padding),
                    onRequestOverlay,
                    onRequestAccessibility,
                    onRequestUsageAccess
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var goal by remember { mutableStateOf("Reduce doomscrolling") }
    var intervention by remember { mutableStateOf("breathing") }
    var minutes by remember { mutableIntStateOf(5) }
    var breaths by remember { mutableIntStateOf(3) }
    var offHours by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    val installedApps = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getInstalledApplications(
            PackageManager.GET_META_DATA
        )
            .filter { it.packageName != context.packageName }
            .filter {
                context.packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedBy {
                context.packageManager.getApplicationLabel(it).toString()
                    .lowercase()
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("L I F E B A C K", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { (step + 1) / 5f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            when (step) {
                0 -> "What would you like to change?"
                1 -> "Choose your trap apps"
                2 -> "Choose your pause"
                3 -> "Set your limits"
                else -> "Enable LifeBack"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        when (step) {
            0 -> {
                listOf(
                    "Reduce doomscrolling",
                    "Improve my sleep",
                    "Reclaim lost hours",
                    "Improve my focus"
                ).forEach { item ->
                    Card(
                        onClick = { goal = item },
                        colors = CardDefaults.cardColors(
                            containerColor = if (goal == item)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(item, Modifier.padding(18.dp))
                    }
                }
            }

            1 -> {
                Text("Select apps that tend to pull you into scrolling.")
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val label = context.packageManager
                            .getApplicationLabel(app).toString()
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = app.packageName in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) {
                                        selected + app.packageName
                                    } else {
                                        selected - app.packageName
                                    }
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(label)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                listOf(
                    "breathing" to "Mindful breathing",
                    "typing" to "Intentional typing",
                    "steps" to "Walking challenge",
                    "reps" to "Exercise repetitions"
                ).forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = intervention == key,
                            onClick = { intervention = key }
                        )
                        Text(label)
                    }
                }
                if (intervention == "steps" || intervention == "reps") {
                    Text(
                        "This challenge requires the corresponding sensor or " +
                            "camera module to be configured before it can unlock apps.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            3 -> {
                Text("Maximum session allowance: $minutes minutes")
                Slider(
                    value = minutes.toFloat(),
                    onValueChange = { minutes = it.toInt().coerceAtLeast(1) },
                    valueRange = 1f..30f,
                    steps = 28
                )
                Text("Required breaths: $breaths")
                Slider(
                    value = breaths.toFloat(),
                    onValueChange = { breaths = it.toInt().coerceIn(1, 10) },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = offHours,
                        onCheckedChange = { offHours = it }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Bedtime off-hours")
                        Text(
                            "10:00 PM – 7:00 AM",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            else -> {
                Text(
                    "LifeBack needs permissions to monitor app usage, " +
                        "detect selected apps, and display a pause screen."
                )
                Button(
                    onClick = onRequestUsageAccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. Allow usage access")
                }
                Button(
                    onClick = onRequestAccessibility,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("2. Enable accessibility service")
                }
                Button(
                    onClick = onRequestOverlay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("3. Allow display over other apps")
                }
                Text(
                    "After enabling each permission, return here to finish setup.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.weight(1f)
                ) { Text("Back") }
            }
            Button(
                onClick = {
                    if (step < 4) {
                        step++
                    } else {
                        LifeBackPreferences.setSelectedApps(context, selected)
                        LifeBackPreferences.setIntervention(context, intervention)
                        LifeBackPreferences.setSessionMinutes(context, minutes)
                        LifeBackPreferences.setBreathCount(context, breaths)
                        LifeBackPreferences.setOffHoursEnabled(context, offHours)
                        LifeBackPreferences.setOffHours(context, 22 * 60, 7 * 60)
                        onFinish()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (step == 4) "Finish setup" else "Continue")
            }
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onFocus: () -> Unit
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val usage = remember(refresh) { UsageRepository.todayUsage(context) }
    val total = usage.sumOf { it.foregroundMillis }
    val minutes = total / 60_000
    val focusMinutes = LifeBackPreferences.focusMinutes(context)
    val bypasses = LifeBackPreferences.bypassCount(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LifeBack", style = MaterialTheme.typography.headlineMedium)
                Text("Make room for what matters.")
            }
            IconButton(onClick = { refresh++ }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh usage")
            }
        }

        Card {
            Column(Modifier.padding(20.dp)) {
                Text("Today's recorded screen time")
                Text(
                    "${minutes / 60}h ${minutes % 60}m",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (UsageRepository.hasUsageAccess(context))
                        "Based on Android usage events"
                    else
                        "Enable Usage Access to view real statistics"
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Focus minutes", "$focusMinutes", Modifier.weight(1f))
            MetricCard("Pause bypasses", "$bypasses", Modifier.weight(1f))
        }

        Button(
            onClick = onFocus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Timer, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start a focus session")
        }

        Text("Most-used apps", style = MaterialTheme.typography.titleLarge)
        if (usage.isEmpty()) {
            Text("No usage data available yet. Grant Usage Access in Settings.")
        } else {
            usage.take(5).forEach { item ->
                val label = try {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(
                            item.packageName, 0
                        )
                    ).toString()
                } catch (_: Exception) {
                    item.packageName
                }
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = { Text(item.packageName) },
                    trailingContent = {
                        Text("${item.foregroundMillis / 60_000} min")
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun AppsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val selected = remember(refresh) {
        LifeBackPreferences.selectedApps(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Your trap apps", style = MaterialTheme.typography.headlineMedium)
        Text("Selected packages are monitored by the blocking service.")
        selected.forEach { pkg ->
            ListItem(
                headlineContent = { Text(pkg) },
                trailingContent = {
                    IconButton(onClick = {
                        LifeBackPreferences.setSelectedApps(
                            context, selected - pkg
                        )
                        refresh++
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            )
        }
        if (selected.isEmpty()) {
            Text("No apps selected. Complete onboarding to choose apps.")
        }
    }
}

@Composable
private fun FocusScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var minutes by remember {
        mutableIntStateOf(LifeBackPreferences.sessionMinutes(context))
    }
    var running by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            kotlinx.coroutines.delay(60_000)
            remaining--
        }
        if (running && remaining == 0) {
            LifeBackPreferences.addFocusMinutes(context, minutes)
            running = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Focus time", style = MaterialTheme.typography.headlineMedium)
        Text("Choose a short period to put your phone aside.")
        Text("$minutes minutes", style = MaterialTheme.typography.displaySmall)
        Slider(
            value = minutes.toFloat(),
            onValueChange = { minutes = it.toInt().coerceAtLeast(1) },
            valueRange = 1f..60f,
            steps = 58,
            enabled = !running
        )
        if (running) {
            Text("$remaining minutes remaining")
            Button(onClick = { running = false }) { Text("End focus") }
        } else {
            Button(
                onClick = {
                    remaining = minutes
                    running = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Begin focus") }
        }
        Text(
            "This is an in-app focus timer. Background completion notifications " +
                "will be connected through the reminder worker in a later implementation step.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val usage = remember(refresh) { UsageRepository.todayUsage(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Usage history",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { refresh++ }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        Text("Today's app usage")
        usage.forEach {
            ListItem(
                headlineContent = { Text(it.packageName) },
                trailingContent = {
                    Text("${it.foregroundMillis / 60_000} min")
                }
            )
        }
        Text(
            "Weekly and monthly aggregation is not yet implemented in this initial version.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestUsageAccess: () -> Unit
) {
    val context = LocalContext.current
    var breaths by remember {
        mutableIntStateOf(LifeBackPreferences.breathCount(context))
    }
    var session by remember {
        mutableIntStateOf(LifeBackPreferences.sessionMinutes(context))
    }
    var offHours by remember {
        mutableStateOf(LifeBackPreferences.offHoursEnabled(context))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Default breath count: $breaths")
        Slider(
            value = breaths.toFloat(),
            onValueChange = {
                breaths = it.toInt().coerceIn(1, 10)
                LifeBackPreferences.setBreathCount(context, breaths)
            },
            valueRange = 1f..10f,
            steps = 8
        )

        Text("Session allowance: $session minutes")
        Slider(
            value = session.toFloat(),
            onValueChange = {
                session = it.toInt().coerceIn(1, 60)
                LifeBackPreferences.setSessionMinutes(context, session)
            },
            valueRange = 1f..60f,
            steps = 58
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = offHours,
                onCheckedChange = {
                    offHours = it
                    LifeBackPreferences.setOffHoursEnabled(context, it)
                }
            )
            Spacer(Modifier.width(12.dp))
            Text("Enforce bedtime off-hours (10 PM–7 AM)")
        }

        HorizontalDivider()

        Button(onClick = onRequestUsageAccess, modifier = Modifier.fillMaxWidth()) {
            Text("Usage Access settings")
        }
        Button(onClick = onRequestAccessibility, modifier = Modifier.fillMaxWidth()) {
            Text("Accessibility settings")
        }
        Button(onClick = onRequestOverlay, modifier = Modifier.fillMaxWidth()) {
            Text("Display over other apps")
        }

        OutlinedButton(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("App information")
        }
    }
}
