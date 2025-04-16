package com.h2o.store.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * A reusable logout button with confirmation dialog that maintains the same structure as IconButton
 *
 * @param onClick Function to execute when logout is confirmed
 * @param modifier Optional modifier for the IconButton
 * @param confirmTitle Dialog title (defaults to "Confirm Logout")
 * @param confirmMessage Dialog message (defaults to "Are you sure you want to logout?")
 * @param confirmButtonText Confirm button text (defaults to "Logout")
 * @param cancelButtonText Cancel button text (defaults to "Cancel")
 * @param content The content to be displayed inside the button (typically an Icon)
 */
@Composable
fun LogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTitle: String = "Confirm Logout",
    confirmMessage: String = "Are you sure you want to logout?",
    confirmButtonText: String = "Logout",
    cancelButtonText: String = "Cancel",
    content: @Composable () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Logout Icon Button with the same structure as IconButton
    IconButton(
        onClick = { showLogoutDialog = true },
        modifier = modifier
    ) {
        // Use the provided content (Icon)
        content()
    }

    // Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = confirmTitle,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = confirmMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onClick()
                    }
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(cancelButtonText)
                }
            }
        )
    }
}