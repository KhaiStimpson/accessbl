package com.accessswitch.phone

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.accessswitch.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries the system ContactsContract for contacts.
 * Filters by favourite contact IDs stored in settings.
 *
 * Contacts are always read from the system database — never stored locally.
 * Requires READ_CONTACTS permission at runtime.
 */
@Singleton
class ContactsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val contentResolver: ContentResolver
        get() = context.contentResolver

    /**
     * Query all contacts that have a phone number.
     * Returns them sorted alphabetically by display name.
     */
    fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            if (cursor != null) {
                val idIndex = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                )
                val nameIndex = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                val numberIndex = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val photoIndex = cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                )

                val seenIds = mutableSetOf<Long>()

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    // Skip duplicate contacts (same contact with multiple numbers)
                    // Keep only the first number for each contact
                    if (id in seenIds) continue
                    seenIds.add(id)

                    val name = cursor.getString(nameIndex) ?: continue
                    val number = cursor.getString(numberIndex) ?: continue
                    val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex) else null

                    contacts.add(
                        Contact(
                            id = id,
                            name = name,
                            phoneNumber = number,
                            photoUri = photoUri
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // READ_CONTACTS permission not granted
            // Return empty list — caller should check permission first
        } finally {
            cursor?.close()
        }

        return contacts
    }

    /**
     * Get only the favourite contacts (those whose IDs are saved in settings).
     * If no favourites are configured, returns all contacts.
     */
    fun getFavouriteContacts(): List<Contact> {
        val favouriteIds = settingsRepository.currentSettings.favouriteContactIds
        if (favouriteIds.isEmpty()) {
            return getAllContacts()
        }

        val allContacts = getAllContacts()
        return allContacts.filter { it.id in favouriteIds }
    }

    /**
     * Look up a contact name by phone number.
     * Used to display caller name for incoming calls.
     * Returns the phone number if no matching contact found.
     */
    fun getContactNameByNumber(phoneNumber: String): String {
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME
        )

        var cursor: Cursor? = null
        try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            cursor = contentResolver.query(uri, projection, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(
                    ContactsContract.PhoneLookup.DISPLAY_NAME
                )
                return cursor.getString(nameIndex) ?: phoneNumber
            }
        } catch (_: Exception) {
            // Permission issue or invalid number
        } finally {
            cursor?.close()
        }

        return phoneNumber
    }
}
