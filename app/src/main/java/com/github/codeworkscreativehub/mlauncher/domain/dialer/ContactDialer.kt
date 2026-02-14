package com.github.codeworkscreativehub.mlauncher.domain.dialer

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem

/**
 * Handles contact dialing. Extracted from MainViewModel
 * to follow single-responsibility principle.
 */
class ContactDialer(private val context: Context) {

    fun dial(contact: ContactListItem) {
        if (contact.phoneNumber.isBlank()) {
            AppLogger.e("ContactDialer", "No phone number for ${contact.displayName}")
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:${contact.phoneNumber}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
