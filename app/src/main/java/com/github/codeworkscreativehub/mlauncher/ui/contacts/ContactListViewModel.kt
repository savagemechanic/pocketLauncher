package com.github.codeworkscreativehub.mlauncher.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem
import com.github.codeworkscreativehub.mlauncher.data.RepositoryProvider
import com.github.codeworkscreativehub.mlauncher.data.repository.ContactRepository
import com.github.codeworkscreativehub.mlauncher.domain.dialer.ContactDialer
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for contact list display and dialing.
 */
class ContactListViewModel(
    application: Application,
    private val contactRepo: ContactRepository,
    private val dialer: ContactDialer
) : AndroidViewModel(application) {

    val contactList: StateFlow<List<ContactListItem>> = contactRepo.contactList
    val contactScrollMap: StateFlow<Map<String, Int>> = contactRepo.contactScrollMap

    fun refreshContacts(includeHidden: Boolean = true) {
        contactRepo.refreshContactList(includeHidden)
    }

    fun selectedContact(contact: ContactListItem) {
        dialer.dial(contact)
        AppLogger.d("ContactListVM", "Contact selected: ${contact.displayName}")
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val p = RepositoryProvider
            return ContactListViewModel(
                application, p.contactRepository, p.contactDialer
            ) as T
        }
    }
}
