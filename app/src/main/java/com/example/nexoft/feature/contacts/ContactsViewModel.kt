package com.example.nexoft.feature.contacts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactsState(
    val contacts: List<com.example.nexoft.core.model.Contact> = emptyList(),
    val searchQuery: String = "",
    val isRefreshingBadges: Boolean = false
)

sealed class ContactsEvent {
    data class OnSearchChanged(val q: String) : ContactsEvent()
}

class ContactsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ContactsState())
    val state = _state.asStateFlow()

    /** Cihazdan okunan normalize telefon numaralarının cache’i */
    private var cachedDeviceNumbers: Set<String> = emptySet()

    // ------------------ Public API ------------------

    fun onEvent(event: ContactsEvent) {
        when (event) {
            is ContactsEvent.OnSearchChanged ->
                _state.update { it.copy(searchQuery = event.q) }
        }
    }

    fun addContact(first: String, last: String, phone: String, photoUri: String?) {
        val normalized = normalizePhone(phone)
        val newOne = com.example.nexoft.core.model.Contact(
            id = java.util.UUID.randomUUID().toString(),
            firstName = first,
            lastName = last,
            phone = phone,
            photoUrl = photoUri,
            isInDevice = cachedDeviceNumbers.contains(normalized)
        )
        _state.update { it.copy(contacts = it.contacts + newOne) }
    }

    /** Rehbere kaydedildi → listede telefon rozeti çıksın (ve cache’e de ekle) */
    fun markAsSavedToDevice(contactId: String) {
        _state.update { s ->
            val target = s.contacts.firstOrNull { it.id == contactId }
            if (target != null) {
                // Cache’e ekle ki rozet anında kalıcı olsun
                cachedDeviceNumbers = cachedDeviceNumbers + normalizePhone(target.phone)
            }
            s.copy(
                contacts = s.contacts.map { c ->
                    if (c.id == contactId) c.copy(isInDevice = true) else c
                }
            )
        }
    }

    /** Edit ekranı “Done” → listedeki kaydı güncelle (rozeti cache’e göre yeniden değerlendir) */
    fun updateContact(
        id: String,
        first: String,
        last: String,
        phone: String,
        photoUri: String?
    ) {
        val normalized = normalizePhone(phone)
        _state.update { s ->
            s.copy(
                contacts = s.contacts.map { c ->
                    if (c.id == id) {
                        c.copy(
                            firstName = first,
                            lastName = last,
                            phone = phone,
                            photoUrl = photoUri,
                            isInDevice = cachedDeviceNumbers.contains(normalized)
                        )
                    } else c
                }
            )
        }
    }

    /** Sil */
    fun deleteContact(id: String) {
        _state.update { s ->
            s.copy(contacts = s.contacts.filterNot { it.id == id })
        }
    }

    /**
     * READ_CONTACTS varsa cihazdaki TÜM numaraları oku → cache’e yaz → listedeki
     * tüm kişilerin `isInDevice` alanını topluca güncelle.
     */
    fun refreshDeviceBadges(ctx: Context) {
        if (!hasReadContacts(ctx)) return
        _state.update { it.copy(isRefreshingBadges = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val numbers = readAllDeviceNumbers(ctx).map(::normalizePhone).toSet()
            cachedDeviceNumbers = numbers

            _state.update { s ->
                val updated = s.contacts.map { c ->
                    val inDevice = numbers.contains(normalizePhone(c.phone))
                    if (c.isInDevice != inDevice) c.copy(isInDevice = inDevice) else c
                }
                s.copy(contacts = updated, isRefreshingBadges = false)
            }
        }
    }

    // ------------------ Helpers ------------------

    private fun hasReadContacts(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    /** Cihaz rehberinden TÜM telefon numaralarını okur (IO-thread). */
    private fun readAllDeviceNumbers(ctx: Context): List<String> {
        val cr = ctx.contentResolver
        val list = mutableListOf<String>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val numCol = ContactsContract.CommonDataKinds.Phone.NUMBER

        try {
            cr.query(uri, arrayOf(numCol), null, null, null)?.use { cur ->
                val idx = cur.getColumnIndexOrThrow(numCol)
                while (cur.moveToNext()) {
                    list += (cur.getString(idx) ?: "")
                }
            }
        } catch (_: SecurityException) {
            // izin yoksa/iptal edilirse sessiz
        } catch (_: Throwable) {
            // diğer hatalarda da sessiz
        }
        return list
    }

    /** Basit normalize: sadece rakamları bırak. (Prototip için yeterli) */
    private fun normalizePhone(raw: String): String = raw.filter { it.isDigit() }
}
