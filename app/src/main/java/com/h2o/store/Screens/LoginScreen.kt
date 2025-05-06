package com.h2o.store.Screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.h2o.store.R
import com.h2o.store.ViewModels.User.LoginState
import com.h2o.store.ViewModels.User.LoginViewModel
import com.h2o.store.repositories.AuthRepository
import com.h2o.store.reusableComposable.PasswordTextField

@Composable
fun LoginScreen(
    navController: NavHostController,
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    prefilledEmail: String? = null,
    loginViewModel: LoginViewModel
) {
    val context = LocalContext.current
    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()

    // State for forgot password dialog
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isResetEmailSending by remember { mutableStateOf(false) }

    // Get the string resource outside of the LaunchedEffect
    val accountCreatedMessage = stringResource(R.string.account_created_success)

    // Now use the pre-loaded string in LaunchedEffect
    LaunchedEffect(prefilledEmail) {
        if (!prefilledEmail.isNullOrEmpty()) {
            loginViewModel.onEmailChanged(prefilledEmail)
            resetEmail = prefilledEmail

            // Use the pre-loaded string
            Toast.makeText(
                context,
                accountCreatedMessage,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text(stringResource(R.string.forgot_password_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.forgot_password_instructions))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (isResetEmailSending) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotEmpty()) {
                            isResetEmailSending = true
                            val authRepository = AuthRepository()
                            authRepository.resetPassword(resetEmail) { success, message ->
                                isResetEmailSending = false
                                Toast.makeText(
                                    context,
                                    message ?: if (success) {
                                        context.getString(R.string.password_reset_email_sent)
                                    } else {
                                        context.getString(R.string.password_reset_failed)
                                    },
                                    Toast.LENGTH_LONG
                                ).show()
                                if (success) {
                                    showForgotPasswordDialog = false
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.email_required),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isResetEmailSending
                ) {
                    Text(stringResource(R.string.send_reset_link))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isResetEmailSending
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                loginViewModel.onEmailChanged(it)
                resetEmail = it // Also update reset email field
            },
            label = { Text(stringResource(R.string.login_email_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordTextField(
            password = password,
            onPasswordChange = { loginViewModel.onPasswordChanged(it) },
            label = stringResource(R.string.login_password_label)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { loginViewModel.login() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.login_button))
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onNavigateToSignUp() }) {
            Text(stringResource(R.string.login_signup_prompt))
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Forgot Password Button
        TextButton(
            onClick = { showForgotPasswordDialog = true }
       ) {
            Text(stringResource(R.string.forgot_password))
        }

        when (loginState) {
            is LoginState.Idle -> { /* Do nothing */ }
            is LoginState.Loading -> CircularProgressIndicator()
            is LoginState.Success -> {
                val role = (loginState as LoginState.Success).role
                LaunchedEffect(role) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.login_success_message, role.capitalize()),
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