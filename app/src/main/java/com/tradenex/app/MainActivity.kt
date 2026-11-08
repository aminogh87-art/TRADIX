package com.tradenex.app

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.tradenex.app.data.AppState
import com.tradenex.app.data.WeeklyPlannerStore
import com.tradenex.app.nav.AppNav
import com.tradenex.app.reminders.ReminderScheduler
import com.tradenex.app.ui.theme.TradenexTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppState.initialize(applicationContext)
        WeeklyPlannerStore.initialize(applicationContext)
        ReminderScheduler.schedule(applicationContext, AppState.settings.reminderOn, AppState.settings.reminderHour)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            TradenexTheme {
                AppNav()
            }
        }
    }
    override fun onPause() {
        AppState.persist()
        WeeklyPlannerStore.persist()
        ReminderScheduler.schedule(applicationContext, AppState.settings.reminderOn, AppState.settings.reminderHour)
        super.onPause()
    }
}
