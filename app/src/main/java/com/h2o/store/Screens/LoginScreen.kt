package com.h2o.store.Screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.h2o.store.ViewModels.User.LoginState
import com.h2o.store.ViewModels.User.LoginViewModel
import com.h2o.store.repositories.AuthRepository


@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: (String) -> Unit, // Modified to take role as parameter
    onNavigateToSignUp: () -> Unit,
    prefilledEmail: String? = null,
    loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(AuthRepository()))
) {
    val context = LocalContext.current
    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()

    // Effect to set the prefilled email if provided
    LaunchedEffect(prefilledEmail) {
        if (!prefilledEmail.isNullOrEmpty()) {
            loginViewModel.onEmailChanged(prefilledEmail)

            // Show a success message for account creation
            Toast.makeText(
                context,
                "Account created successfully! Please login to continue.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Login", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { loginViewModel.onEmailChanged(it) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { loginViewModel.onPasswordChanged(it) },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                loginViewModel.login()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onNavigateToSignUp() }) {
            Text("Don't have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (loginState) {
            is LoginState.Idle -> { /* Do nothing */ }
            is LoginState.Loading -> CircularProgressIndicator()
            is LoginState.Success -> {
                val role = (loginState as LoginState.Success).role
                LaunchedEffect(role) {
                    Toast.makeText(
                        context,
                        "Login Successful as ${role.capitalize()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLoginSuccess(role)
                }
            }
            is LoginState.Error -> {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}