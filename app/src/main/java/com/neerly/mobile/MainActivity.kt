package com.neerly.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.neerly.mobile.core.design.NeerlyTheme
import com.neerly.mobile.core.design.Role
import com.neerly.mobile.navigation.NeerlyNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeerlyApp()
        }
    }
}

@Composable
fun NeerlyApp() {
    NeerlyTheme(role = Role.CUSTOMER) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NeerlyNavHost(navController)
        }
    }
}
