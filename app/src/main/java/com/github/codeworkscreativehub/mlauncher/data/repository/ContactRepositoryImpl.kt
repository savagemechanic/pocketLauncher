package com.github.codeworkscreativehub.mlauncher.data.repository

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import com.github.codeworkscreativehub.common.AppLogger
import com.github.codeworkscreativehub.mlauncher.data.ContactCategory
import com.github.codeworkscreativehub.mlauncher.data.ContactListItem
import com.github.codeworkscreativehub.mlauncher.data.cache.AppCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class ContactRepositoryImpl(
    private val context: Context,
    private val appMgmt: AppManagementRepository,
    private val cacheManager: AppCacheManager,
    private val scope: CoroutineScope
) : ContactRepository {

    private val _contactList = MutableStateFlow<List<ContactListItem>>(emptyList())
    override val contactList: StateFlow<List<ContactListItem>> = _contactList.asStateFlow()

    private val _contactScrollMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val contactScrollMap: StateFlow<Map<String, Int>> = _contactScrollMap.asStateFlow()

    private var memoryCache: MutableList<ContactListItem>? = null
    private val refreshing = AtomicBoolean(false)

    private val contactsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            memoryCache = null
            refreshContactList()
        }
    }

    init {
        runCatching {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, contactsObserver
            )
        }.onFailure { AppLogger.e("ContactRepo", "Failed to register observer", it) }
        refreshContactList()
    }

    override fun refreshContactList(includeHiddenContacts: Boolean) {
        memoryCache?.let { _contactList.value = it }
            ?: cacheManager.loadContacts()?.let { cached ->
                memoryCache = cached.toMutableList()
                _contactList.value = cached
            }

        if (refreshing.compareAndSet(false, true)) {
            scope.launch {
                try {
                    val fresh = fetchContacts(includeHiddenContacts)
                    memoryCache = fresh
                    cacheManager.saveContacts(fresh)
                    _contactList.value = fresh
                } finally {
                    refreshing.set(false)
                }
            }
        }
    }

    override fun invalidateCache() {
        memoryCache = null
    }

    private suspend fun fetchContacts(
        includeHidden: Boolean = false
    ): MutableList<ContactListItem> = withContext(Dispatchers.IO) {
        val hiddenContacts = appMgmt.hiddenContacts.value
        val pinnedContacts = appMgmt.pinnedContacts.value
        val contentResolver = context.contentResolver

        val basicContacts = runCatching {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.LOOKUP_KEY
                ), null, null,
                "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                generateSequence {
                    if (cursor.moveToNext()) {
                        Triple(
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)) ?: "",
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                        )
                    } else null
                }.toList()
            } ?: emptyList()
        }.getOrElse { emptyList() }

        val contactIds = basicContacts.map { it.first }
        val phonesMap = fetchPhones(contactIds)
        val emailsMap = fetchEmails(contactIds)

        val seenContacts = mutableSetOf<String>()
        val items = mutableListOf<ContactListItem>()
        val scrollMap = mutableMapOf<String, Int>()

        basicContacts.forEach { (id, name, lookup) ->
            val key = "$id|$lookup"
            if (!seenContacts.add(key)) return@forEach
            if (lookup in hiddenContacts && !includeHidden) return@forEach

            val pinned = lookup in pinnedContacts
            items.add(
                ContactListItem(
                    displayName = name,
                    phoneNumber = phonesMap[id] ?: "",
                    email = emailsMap[id] ?: "",
                    category = if (pinned) ContactCategory.FAVORITE else ContactCategory.REGULAR
                )
            )
        }

        items.sortWith(
            compareByDescending<ContactListItem> { it.category == ContactCategory.FAVORITE }
                .thenBy { it.displayName.lowercase() }
        )

        items.forEachIndexed { index, item ->
            val pinned = item.category == ContactCategory.FAVORITE
            val mapKey = if (pinned) "★" else item.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            scrollMap.putIfAbsent(mapKey, index)
        }
        _contactScrollMap.value = scrollMap

        items
    }

    private fun fetchPhones(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN (${ids.joinToString(",") { "?" }})",
            ids.toTypedArray(), null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                map.putIfAbsent(id, number)
            }
        }
        return map
    }

    private fun fetchEmails(ids: List<String>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} IN (${ids.joinToString(",") { "?" }})",
            ids.toTypedArray(), null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID))
                val email = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)) ?: ""
                map.putIfAbsent(id, email)
            }
        }
        return map
    }
}
