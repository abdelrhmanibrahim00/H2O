package com.h2o.store.ViewModels.User

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.h2o.store.repositories.ProfileRepository
import com.h2o.store.repositories.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: ProfileRepository) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState = _profileState.asStateFlow()

    // Optional: editable fields if you want to add editing capability
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _whatsapp = MutableStateFlow("")
    val whatsapp = _whatsapp.asStateFlow()

    private val _district = MutableStateFlow("")
    val district = _district.asStateFlow()

    private val _city = MutableStateFlow("")
    val city = _city.asStateFlow()

    // Firebase auth state listener
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser != null) {
            if (_profileState.value is ProfileState.Error &&
                (_profileState.value as? ProfileState.Error)?.message?.contains("logged in", ignoreCase = true) == true) {
                loadProfile()
            }
        } else {
            _profileState.value = ProfileState.Error("User not logged in")
        }
    }

    init {
        // Add auth state listener
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

        // Check if user is already logged in before loading profile
        if (userRepository.isUserLoggedIn()) {
            loadProfile()
        } else {
            _profileState.value = ProfileState.Error("User not logged in")
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    fun loadProfile() {
        // Do nothing if not logged in
        if (!userRepository.isUserLoggedIn()) {
            _profileState.value = ProfileState.Error("User not logged in")
            return
        }

        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            // Try to refresh token first
            try {
                val refreshed = userRepository.refreshUserToken()
                if (!refreshed) {
                    // Token couldn't be refreshed, but we'll try anyway
                    // This is just a warning log, not an error
                }
            } catch (e: Exception) {
                // Handle possible auth exceptions
                if (e is FirebaseAuthInvalidUserException) {
                    _profileState.value =
                        ProfileState.Error("Your session has expired. Please login again.")
                    return@launch
                }
            }

            userRepository.getUserData().collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _profileState.value = ProfileState.Success(user)

                        // Populate editable fields
                        _name.value = user.name
                        _phone.value = user.phone
                        _whatsapp.value = user.whatsapp
                        _district.value = user.district
                        _city.value = user.city
                    },
                    onFailure = { e ->
                        _profileState.value =
                            ProfileState.Error(e.message ?: "Unknown error occurred")
                    }
                )
            }
        }
    }

    // Handle field changes
    fun onNameChanged(newName: String) {
        _name.value = newName
    }

    fun onPhoneChanged(newPhone: String) {
        _phone.value = newPhone
    }

    fun onWhatsappChanged(newWhatsapp: String) {
        _whatsapp.value = newWhatsapp
    }

    fun onDistrictChanged(newDistrict: String) {
        _district.value = newDistrict
    }

    fun onCityChanged(newCity: String) {
        _city.value = newCity
    }

    // Update profile
    fun updateProfile() {
        if (!userRepository.isUserLoggedIn()) {
            _profileState.value = ProfileState.Error("User not logged in")
            return
        }

        _profileState.value = ProfileState.Loading

        viewModelScope.launch {
            userRepository.updateUserData(
                name = _name.value,
                phone = _phone.value,
                whatsapp = _whatsapp.value,
                district = _district.value,
                city = _city.value
            ) { success, error ->
                if (success) {
                    loadProfile() // Reload profile after update
                } else {
                    _profileState.value = ProfileState.Error(error ?: "Failed to update profile")
                }
            }
        }
    }

    fun resetProfileState() {
        _profileState.value = ProfileState.Idle
    }

    // Navigate to login function
    fun signOut() {
        userRepository.signOut()
        _profileState.value = ProfileState.Error("User not logged in")
    }

    companion object {
        fun Factory(userRepository: ProfileRepository) = viewModelFactory {
            initializer { ProfileViewModel(userRepository) }
        }
    }
}

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}