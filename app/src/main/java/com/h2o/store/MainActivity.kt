package com.h2o.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.h2o.store.Graph.Graph
import com.h2o.store.Navigation.AppNavHost
import com.h2o.store.ui.theme.H2OTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Initialize Graph before setContent
            Graph.provide(applicationContext)
            val navController = rememberNavController()
            val context = LocalContext.current
            H2OTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(navController,this)
                   // HomeScreen(navController)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    H2OTheme {
        Greeting("zahi")
    }
}