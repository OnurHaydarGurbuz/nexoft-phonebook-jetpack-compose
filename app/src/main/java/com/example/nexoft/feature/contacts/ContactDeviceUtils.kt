// ContactDeviceUtils.kt
package com.example.nexoft.feature.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

private fun hasPerm(ctx: Context, perm: String) =
    ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

fun isInDeviceContacts(context: Context, phone: String): Boolean {
    // İZİN YOKSA asla sorgulama → false dön
    if (!hasPerm(context, Manifest.permission.READ_CONTACTS)) return false
    if (phone.isBlank()) return false

    val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
        .appendPath(phone).build()

    return try {
        context.contentResolver.query(uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null, null, null
        )?.use { c -> c.moveToFirst() } ?: false
    } catch (_: SecurityException) {
        false
    }
}

fun saveToDeviceContacts(
    context: Context,
    first: String,
    last: String,
    phone: String,
    photo: Uri?
): Boolean {
    // İZİN YOKSA yazma denemesi yapma
    if (!hasPerm(context, Manifest.permission.WRITE_CONTACTS)) return false

    val ops = ArrayList<ContentProviderOperation>()
    val rawIdx = ops.size

    ops += ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
        .build()

    ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdx)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, first)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, last)
        .build()

    ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdx)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        .build()

    if (photo != null) {
        val bytes = try {
            context.contentResolver.openInputStream(photo)?.use { it.readBytes() }
        } catch (_: Throwable) { null }
        if (bytes != null) {
            ops += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawIdx)
                .withValue(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
                .build()
        }
    }

    return try {
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: Throwable) {
        false
    }
}
