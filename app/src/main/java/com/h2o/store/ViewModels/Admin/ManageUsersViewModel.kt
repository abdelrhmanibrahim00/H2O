package com.h2o.store.ViewModels.Admin


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.h2o.store.data.User.UserData
import com.h2o.store.data.models.AddressData
import com.h2o.store.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageUsersViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<UserData>>(emptyList())
    val allUsers: StateFlow<List<UserData>> = _allUsers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _editUserResult = MutableStateFlow<Boolean?>(null)
    val editUserResult: StateFlow<Boolean?> = _editUserResult

    private val TAG = "ManageUsersViewModel"

    // Fetch all users
    fun fetchAllUsers() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                userRepository.getAllUsersFlow()
                    .catch { e ->
                        Log.e(TAG, "Error fetching users: ${e.message}")
                        _errorMessage.value = "Failed to load users: ${e.message}"
                        _isLoading.value = false
                    }
                    .collectLatest { users ->
                        _allUsers.value = users
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchAllUsers: ${e.message}")
                _errorMessage.value = "Failed to load users: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Get user details
    suspend fun getUserDetails(userId: String): UserData? {
        return try {
            userRepository.getUserData(userId)
        } catch (e: Exception) {
            _errorMessage.value = "Error fetching user details: ${e.message}"
            null
        }
    }

    // Update user
    fun updateUser(user: UserData, originalUser: UserData): Boolean {
        // Do a proper comparison by checking each field that we allow editing
        val hasChanges = !(
                user.name == originalUser.name &&
                        user.email == originalUser.email &&
                        user.phone == originalUser.phone &&
                        user.whatsapp == originalUser.whatsapp &&
                        user.city == originalUser.city &&
                        user.district == originalUser.district &&
                        user.role == originalUser.role &&
                        compareAddresses(user.address, originalUser.address)
                )

        // If nothing has changed, return false immediately
        if (!hasChanges) {
            Log.d(TAG, "No changes detected in user ${user.id}")
            return false
        }

        // Log what changed for debugging
        Log.d(TAG, "Changes detected in user ${user.id}:")
        if (user.name != originalUser.name)
            Log.d(TAG, "Name changed: ${originalUser.name} -> ${user.name}")
        if (user.email != originalUser.email)
            Log.d(TAG, "Email changed: ${originalUser.email} -> ${user.email}")
        if (user.phone != originalUser.phone)
            Log.d(TAG, "Phone changed: ${originalUser.phone} -> ${user.phone}")
        if (user.whatsapp != originalUser.whatsapp)
            Log.d(TAG, "WhatsApp changed: ${originalUser.whatsapp} -> ${user.whatsapp}")
        if (user.city != originalUser.city)
            Log.d(TAG, "City changed: ${originalUser.city} -> ${user.city}")
        if (user.district != originalUser.district)
            Log.d(TAG, "District changed: ${originalUser.district} -> ${user.district}")
        if (user.role != originalUser.role)
            Log.d(TAG, "Role changed: ${originalUser.role} -> ${user.role}")
        if (!compareAddresses(user.address, originalUser.address))
            Log.d(TAG, "Address changed")

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _editUserResult.value = null

            try {
                val success = userRepository.updateUser(user)
                _editUserResult.value = success

                if (success) {
                    Log.d(TAG, "Successfully updated user ${user.id}")
                } else {
                    _errorMessage.value = "Failed to update user"
                    Log.e(TAG, "Failed to update user ${user.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user: ${e.message}")
                _errorMessage.value = "Error updating user: ${e.message}"
                _editUserResult.value = false
            } finally {
                _isLoading.value = false
            }
        }

        return true
    }

    // Filter users by text search
    fun filterUsersByText(query: String) {
        if (query.isBlank()) {
            fetchAllUsers()
            return
        }

        viewModelScope.launch {
            try {
                // We'll implement client-side filtering
                val allUsersList = _allUsers.value
                val filteredUsers = allUsersList.filter { user ->
                    user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true) ||
                            user.phone.contains(query, ignoreCase = true) ||
                            user.city.contains(query, ignoreCase = true) ||
                            user.district.contains(query, ignoreCase = true)
                }

                // Update the state with filtered users
                _allUsers.value = filteredUsers
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering users: ${e.message}")
                _errorMessage.value = "Error filtering users: ${e.message}"
            }
        }
    }

    // Reset edit result state
    fun resetEditUserResult() {
        _editUserResult.value = null
    }

    // Helper method to compare addresses
    private fun compareAddresses(address1: AddressData?, address2: AddressData?): Boolean {
        if (address1 == null && address2 == null) return true
        if (address1 == null || address2 == null) return false

        return address1.street == address2.street &&
                address1.city == address2.city &&
                address1.state == address2.state &&
                address1.country == address2.country &&
                address1.postalCode == address2.postalCode
    }

    // Factory for creating this ViewModel with dependencies
    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ManageUsersViewModel::class.java)) {
                return ManageUsersViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}