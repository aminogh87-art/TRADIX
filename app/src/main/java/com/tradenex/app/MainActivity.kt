package com.tradenex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tradenex.app.data.AppState
import com.tradenex.app.data.WeeklyPlannerStore
import com.tradenex.app.nav.AppNav
import com.tradenex.app.ui.theme.TradenexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppState.initialize(applicationContext)
        WeeklyPlannerStore.initialize(applicationContext)
        setContent {
            TradenexTheme {
                AppNav()
            }
        }
    }
    override fun onPause() {
        AppState.persist()
        WeeklyPlannerStore.persist()
        super.onPause()
    }
}
