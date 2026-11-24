package com.amverge.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.amverge.android.app.navigation.AMVergeNavHost
import com.amverge.android.frontend.theme.AMVergeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AMVergeTheme {
                val navController = rememberNavController()
                AMVergeNavHost(navController = navController)
            }
        }
    }
}
