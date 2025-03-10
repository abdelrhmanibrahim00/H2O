package com.h2o.store.Models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.h2o.store.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChanged(newPassword: String) {
        _password.value = newPassword
    }

    fun login() {
        if (_email.value.isEmpty() || _password.value.isEmpty()) {
            _loginState.value = LoginState.Error("Email and password cannot be empty.")
            return
        }

        _loginState.value = LoginState.Loading

        // Call Firebase Authentication
        authRepository.loginUser(_email.value, _password.value) { success, error ->
            if (success) {
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.Error(error ?: "Login failed.")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
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
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
