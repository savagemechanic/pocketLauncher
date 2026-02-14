package com.github.codeworkscreativehub.mlauncher.data.repository

import com.github.codeworkscreativehub.mlauncher.data.ContactListItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages contact list fetching, caching, and scroll index.
 * Replaces the contact portion of MainViewModel.
 */
interface ContactRepository {
    val contactList: StateFlow<List<ContactListItem>>
    val contactScrollMap: StateFlow<Map<String, Int>>

    fun refreshContactList(includeHiddenContacts: Boolean = true)
    fun invalidateCache()
}
