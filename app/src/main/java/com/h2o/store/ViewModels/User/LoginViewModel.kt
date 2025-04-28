package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.h2o.store.R
import com.h2o.store.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    // For handling specific error message resource IDs
    private val _errorMessageResId = MutableStateFlow<Int?>(null)
    val errorMessageResId = _errorMessageResId.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
        // Clear error state when user types
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
            _errorMessageResId.value = null
        }
    }

    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
        // Clear error state when user types
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
            _errorMessageResId.value = null
        }
    }

    fun login() {
        if (_email.value.isEmpty() || _password.value.isEmpty()) {
            _loginState.value = LoginState.Error(R.string.login_empty_fields_error.toString())
            _errorMessageResId.value = R.string.login_empty_fields_error
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                // Call Firebase Authentication with the updated API
                authRepository.loginUser(_email.value, _password.value) { result ->
                    if (result.success) {
                        // Set the login state with the role information
                        _loginState.value = LoginState.Success(result.role ?: "user")
                        _errorMessageResId.value = null
                    } else {
                        _loginState.value = LoginState.Error(result.errorMessage ?: "Login failed.")

                        // Set the appropriate error message resource ID
                        _errorMessageResId.value = when {
                            result.errorMessage?.contains("password", ignoreCase = true) == true ->
                                R.string.auth_error_incorrect_password
                            result.errorMessage?.contains("no account", ignoreCase = true) == true ->
                                R.string.auth_error_no_account
                            result.errorMessage?.contains("blocked", ignoreCase = true) == true ->
                                R.string.auth_error_account_blocked
                            result.errorMessage?.contains("verify", ignoreCase = true) == true ->
                                R.string.email_verification_required
                            else -> R.string.auth_error_generic
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle any unexpected exceptions
                _loginState.value = LoginState.Error("Login error: ${e.message}")
                _errorMessageResId.value = R.string.auth_error_generic
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
        _errorMessageResId.value = null
    }

    companion object {
        fun Factory(authRepository: AuthRepository) = viewModelFactory {
            initializer { LoginViewModel(authRepository) }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val role: String) : LoginState()
    data class Error(val message: String) : LoginState()
}