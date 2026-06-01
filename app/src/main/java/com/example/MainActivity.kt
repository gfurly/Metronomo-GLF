package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.SongRepository
import com.example.ui.MetronomeScreen
import com.example.ui.MetronomeViewModel
import com.example.ui.MetronomeViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local SQLite Database and Repository
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = SongRepository(database.songDao())

        // Retrieve/Instantiate ViewModel with Custom Factory Pattern
        val viewModel = ViewModelProvider(
            this, 
            MetronomeViewModelFactory(repository)
        )[MetronomeViewModel::class.java]

        setContent {
            val darkThemePref by viewModel.darkThemePreferenceState.collectAsState()
            val useDarkTheme = darkThemePref ?: isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MetronomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
