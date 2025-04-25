package com.h2o.store.ViewModels.User

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.model.LatLng
import com.h2o.store.data.models.AddressData
import com.h2o.store.data.models.LocationData
import com.h2o.store.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SignUpViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _whatsapp = MutableStateFlow("")
    val whatsapp = _whatsapp.asStateFlow()

    private val _district = MutableStateFlow("")
    val district = _district.asStateFlow()

    // Updated: Changed from LatLng to LocationData
    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData = _locationData.asStateFlow()

    // New: Added structured address data
    private val _addressData = MutableStateFlow<AddressData?>(null)
    val addressData = _addressData.asStateFlow()

    // Keep for backward compatibility
    private val _address = MutableStateFlow<LatLng?>(null)
    val address = _address.asStateFlow()

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState = _signUpState.asStateFlow()

    // Add after other private val declarations
    private val _accountType = MutableStateFlow("Home") // Default to "Home"
    val accountType = _accountType.asStateFlow()

    fun onNameChanged(newName: String) { _name.value = newName }
    fun onPhoneChanged(newPhone: String) { _phone.value = newPhone }
    fun onEmailChanged(newEmail: String) { _email.value = newEmail }
    fun onWhatsAppChanged(newWhatsApp: String) { _whatsapp.value = newWhatsApp }
    fun onDistrictChanged(newDistrict: String) { _district.value = newDistrict }
    fun onAddressChanged(newAddress: LatLng) { _address.value = newAddress }
    fun onPasswordChanged(newPassword: String) { _password.value = newPassword }
    // Add after other onXChanged functions
    fun onAccountTypeChanged(newAccountType: String) { _accountType.value = newAccountType }
    // New methods for location and address data
    fun updateLocationAndAddress(locationData: LocationData, addressData: AddressData) {
        _locationData.value = locationData
        _addressData.value = addressData

        // Update the legacy address field for backward compatibility
        _address.value = LatLng(locationData.latitude, locationData.longitude)
    }

    // Name Validation
    fun isValidName(name: String): Boolean {
        return name.length >= 2 && name.all { it.isLetter() || it.isWhitespace() }
    }

    // Phone Number Validation (Starts with + and contains only digits after)
    fun isValidPhoneNumber(phone: String): Boolean {
        return phone.startsWith("+") && phone.drop(1).all { it.isDigit() }
    }

    // Email Validation
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Password Validation (Minimum 6 characters, at least one letter and one number)
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6 && password.any { it.isLetter() } && password.any { it.isDigit() }
    }

    // Address Validation - Check both LocationData and AddressData
    fun isValidAddress(): Boolean {
        return _locationData.value != null && _addressData.value != null
    }

    fun signUp() {
        if (!isValidName(_name.value)) {
            _signUpState.value = SignUpState.Error("Enter a valid name.")
            return
        }
        if (!isValidPhoneNumber(_phone.value)) {
            _signUpState.value = SignUpState.Error("Enter a valid phone number starting with '+'.")
            return
        }
        if (!isValidEmail(_email.value)) {
            _signUpState.value = SignUpState.Error("Invalid email format.")
            return
        }
        if (!isValidPassword(_password.value)) {
            _signUpState.value =
                SignUpState.Error("Password must be at least 6 characters and contain a letter and a number.")
            return
        }
        if (_district.value.isEmpty()) {
            _signUpState.value = SignUpState.Error("Select a valid district.")
            return
        }
        if (_locationData.value == null || _addressData.value == null) {
            _signUpState.value =
                SignUpState.Error("Please select a valid address before proceeding.")
            return
        }

        _signUpState.value = SignUpState.Loading

        // Use the new data structure
        val locationData = _locationData.value!!
        val addressData = _addressData.value!!

        authRepository.signUpUser(
            name = _name.value,
            phone = _phone.value,
            email = _email.value,
            password = _password.value,
            whatsapp = _whatsapp.value,
            district = _district.value,
            locationData = locationData,
            addressData = addressData,
            accountType = _accountType.value, // Add this parameter

            onResult = { success, error ->
                if (success) {
                    _signUpState.value = SignUpState.Success
                } else {
                    _signUpState.value = SignUpState.Error(error ?: "Sign-up failed.")
                }
            }
        )
    }

    // Sealed class for Sign-Up State
    sealed class SignUpState {
        object Idle : SignUpState()
        object Loading : SignUpState()
        object Success : SignUpState()
        data class Error(val message: String) : SignUpState()
    }
}