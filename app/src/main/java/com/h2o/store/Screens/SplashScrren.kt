package com.h2o.store.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.h2o.store.R

@Composable
fun SplashScreen (
    navController: NavHostController,
    onTimeout : () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Optional background color
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_screen_image),
            contentDescription = "Splash Logo",
            modifier = Modifier.fillMaxWidth(), // Makes the image fill the screen
            contentScale = ContentScale.Crop // Ensures the image scales appropriately
        )
    }

    // Navigate after delay
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000) // 3 seconds delay
        onTimeout()
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    //SplashScreen(onTimeout = {})
}

