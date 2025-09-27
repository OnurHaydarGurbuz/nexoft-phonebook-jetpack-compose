package com.example.nexoft.feature.contacts

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexoft.feature.remote.ContactsRepositoryImpl
import com.example.nexoft.feature.remote.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactsState(
    val contacts: List<com.example.nexoft.core.model.Contact> = emptyList(),
    val searchQuery: String = "",
    val isRefreshingBadges: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ContactsEvent {
    data class OnSearchChanged(val q: String) : ContactsEvent()
    object OnErrorDismissed : ContactsEvent()
}

class ContactsViewModel : ViewModel() {

    // ---- UI state ----
    private val _state = MutableStateFlow(ContactsState())
    val state = _state.asStateFlow()

    // ---- Cihaz rehberi rozeti için cache ----
    private var cachedDeviceNumbers: Set<String> = emptySet()

    // ---- Backend repo ----
    private val remote by lazy { ContactsRepositoryImpl(ServiceLocator.api) }

    // ---- Log etiketi ----
    private val TAG = "ContactsVM"

    // ---- Uygulama açılışında: sadece GET ----
    init {
        fetchAllFromBackend()

        // 🔧 Test fonksiyonları - geliştirme sırasında gerekirse açın:
        // seedOnceAndRefetch()
        // smokeTestBackend()
    }

    // region --------- BACKEND OPERASYONLARI ---------

    /** Backend: GetAll → UI state güncelle → Logcat */
    private fun fetchAllFromBackend() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            runCatching { remote.getAll() }
                .onSuccess { list ->
                    _state.update {
                        it.copy(
                            contacts = list,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d(TAG, "GET /GetAll başarılı, toplam kayıt=${list.size}")
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Veri yüklenemedi: ${e.message}"
                        )
                    }
                    Log.e(TAG, "GET /GetAll başarısız: ${e.message}", e)
                }
        }
    }

    /** Manuel refresh fonksiyonu - kullanıcı çekip yenilediğinde */
    fun refreshContacts() {
        fetchAllFromBackend()
    }

    /** Backend'e kişi ekleme + UI güncelleme */
    fun addContact(first: String, last: String, phone: String, photoUri: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val newContact = com.example.nexoft.core.model.Contact(
                    id = "",
                    firstName = first,
                    lastName = last,
                    phone = phone,
                    photoUrl = photoUri,
                    isInDevice = cachedDeviceNumbers.contains(normalizePhone(phone))
                )

                // Backend'e gönder
                val created = remote.create(newContact, photoFile = null)
                Log.d(TAG, "Kişi eklendi, id=${created.id}")

                // UI state'i güncelle - backend'den dönen veriyi kullan
                _state.update {
                    it.copy(
                        contacts = it.contacts + created,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Kişi eklenemedi: ${e.message}"
                    )
                }
                Log.e(TAG, "Kişi ekleme hatası: ${e.message}", e)
            }
        }
    }

    /** Backend'e kişi güncelleme + UI güncelleme */
    fun updateContact(
        id: String,
        first: String,
        last: String,
        phone: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val updatedContact = com.example.nexoft.core.model.Contact(
                    id = id,
                    firstName = first,
                    lastName = last,
                    phone = phone,
                    photoUrl = photoUri,
                    isInDevice = cachedDeviceNumbers.contains(normalizePhone(phone))
                )

                // Backend'e gönder
                val updated = remote.update(updatedContact, photoFile = null)
                Log.d(TAG, "Kişi güncellendi, id=${updated.id}")

                // UI state'i güncelle
                _state.update { s ->
                    s.copy(
                        contacts = s.contacts.map { c ->
                            if (c.id == id) updated else c
                        },
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Kişi güncellenemedi: ${e.message}"
                    )
                }
                Log.e(TAG, "Kişi güncelleme hatası: ${e.message}", e)
            }
        }
    }

    /** Backend'den kişi silme + UI güncelleme */
    fun deleteContact(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Backend'den sil
                remote.delete(id)
                Log.d(TAG, "Kişi silindi, id=$id")

                // UI state'ten çıkar
                _state.update { s ->
                    s.copy(
                        contacts = s.contacts.filterNot { it.id == id },
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Kişi silinemedi: ${e.message}"
                    )
                }
                Log.e(TAG, "Kişi silme hatası: ${e.message}", e)
            }
        }
    }

    // endregion --------- BACKEND OPERASYONLARI ---------

    // region --------- TEST FONKSİYONLARI (İSTEĞE BAĞLI) ---------

    /** (İSTEĞE BAĞLI) Tek seferlik tohumlama: 1 kayıt ekle → tekrar GET */
    private var seededOnce = false
    private fun seedOnceAndRefetch() {
        if (seededOnce) return
        seededOnce = true

        viewModelScope.launch {
            try {
                val yeni = com.example.nexoft.core.model.Contact(
                    id = "",
                    firstName = "Test",
                    lastName = "User",
                    phone = "5550001122",
                    photoUrl = null,
                    isInDevice = false
                )
                val created = remote.create(yeni, photoFile = null)
                Log.d(TAG, "SEED: Kişi oluşturuldu, id=${created.id}")

                fetchAllFromBackend()
            } catch (e: Exception) {
                Log.e(TAG, "SEED: Kişi oluşturma hatası: ${e.message}", e)
            }
        }
    }

    /**
     * (İSTEĞE BAĞLI) Tam duman testi:
     * GET → CREATE (fake) → UPDATE (telefonuna '9' ekle) → DELETE → GET
     */
    private fun smokeTestBackend() {
        viewModelScope.launch {
            try {
                val before = remote.getAll()
                Log.d(TAG, "DMN: İlk GET tamam, mevcut kayıt sayısı=${before.size}")

                val tmp = com.example.nexoft.core.model.Contact(
                    id = "",
                    firstName = "Test",
                    lastName = "User",
                    phone = "5550001122",
                    photoUrl = null,
                    isInDevice = false
                )
                val created = remote.create(tmp, photoFile = null)
                Log.d(TAG, "DMN: CREATE başarılı, id=${created.id}")

                val updatedReq = created.copy(phone = created.phone + "9")
                val updated = remote.update(updatedReq, photoFile = null)
                Log.d(TAG, "DMN: UPDATE başarılı, id=${updated.id}, yeniTelefon=${updated.phone}")

                remote.delete(updated.id)
                Log.d(TAG, "DMN: DELETE başarılı, silinen id=${updated.id}")

                val after = remote.getAll()
                Log.d(TAG, "DMN: Son GET tamam, güncel kayıt sayısı=${after.size}")
            } catch (e: Exception) {
                Log.e(TAG, "DMN: Duman testi başarısız: ${e.message}", e)
            }
        }
    }

    // endregion --------- TEST FONKSİYONLARI ---------

    // region --------- UI EVENT HANDLİNG ---------

    fun onEvent(event: ContactsEvent) {
        when (event) {
            is ContactsEvent.OnSearchChanged ->
                _state.update { it.copy(searchQuery = event.q) }
            is ContactsEvent.OnErrorDismissed ->
                _state.update { it.copy(error = null) }
        }
    }

    /** (Lokal) Cihazda kayıtlı rozetini işaretlemek */
    fun markAsSavedToDevice(contactId: String) {
        _state.update { s ->
            val target = s.contacts.firstOrNull { it.id == contactId }
            if (target != null) {
                cachedDeviceNumbers = cachedDeviceNumbers + normalizePhone(target.phone)
            }
            s.copy(
                contacts = s.contacts.map { c ->
                    if (c.id == contactId) c.copy(isInDevice = true) else c
                }
            )
        }
    }

    /** (Lokal) Cihaz rehberini okuyup rozetleri güncellemek */
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

    // endregion --------- UI EVENT HANDLİNG ---------

    // region --------- Yardımcılar ---------

    private fun hasReadContacts(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    /** Cihaz rehberinden tüm telefon numaralarını (string) çeker. */
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
            // izin yoksa sessiz
        } catch (_: Throwable) {
            // diğer hatalar sessiz
        }
        return list
    }

    /** Basit normalize: sadece rakamları bırak. */
    private fun normalizePhone(raw: String): String = raw.filter { it.isDigit() }

    // endregion --------- Yardımcılar ---------
}