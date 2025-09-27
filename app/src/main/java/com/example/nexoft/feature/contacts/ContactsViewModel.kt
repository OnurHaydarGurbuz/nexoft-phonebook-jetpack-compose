package com.example.nexoft.feature.contacts

import android.annotation.SuppressLint
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
    data object OnErrorDismissed : ContactsEvent()
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


    init {
        fetchAllFromBackend()

        // 🔧 Geliştirme esnasında açıp test edebilirsin:
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

    /** Manuel refresh: kullanıcı çekip yenilediğinde çağır. */
    fun refreshContacts() = fetchAllFromBackend()

    /**
     * REMOTE Create:
     * - (varsa) foto URI → geçici File
     * - uploadImage + create
     * - UI state’e ekle
     */
    fun addContactRemote(
        ctx: Context,
        first: String,
        last: String,
        phone: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val file = fileFromUriOrNull(ctx, photoUri)

                val created = remote.create(
                    com.example.nexoft.core.model.Contact(
                        id = "",
                        firstName = first.trim(),
                        lastName = last.trim(),
                        phone = phone.trim(),
                        photoUrl = null,
                        isInDevice = cachedDeviceNumbers.contains(normalizePhone(phone))
                    ),
                    photoFile = file
                )

                val final = created.copy(
                    isInDevice = cachedDeviceNumbers.contains(normalizePhone(created.phone))
                )

                _state.update { s ->
                    s.copy(
                        contacts = s.contacts + final,
                        isLoading = false,
                        error = null
                    )
                }
                Log.d(TAG, "REMOTE/CREATE başarılı, id=${created.id}")
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Kişi eklenemedi: ${e.message}") }
                Log.e(TAG, "REMOTE/CREATE hata: ${e.message}", e)
            }
        }
    }


    fun updateContactRemote(
        ctx: Context,
        id: String,
        first: String,
        last: String,
        phone: String,
        photoUri: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val file = fileFromUriOrNull(ctx, photoUri)

                val base = state.value.contacts.firstOrNull { it.id == id }
                    ?: com.example.nexoft.core.model.Contact(
                        id = id, firstName = "", lastName = "", phone = "", photoUrl = null, isInDevice = false
                    )

                val toSave = base.copy(
                    firstName = first.trim(),
                    lastName = last.trim(),
                    phone = phone.trim()
                )

                val updated = remote.update(toSave, photoFile = file)

                val final = updated.copy(
                    isInDevice = cachedDeviceNumbers.contains(normalizePhone(updated.phone))
                )

                _state.update { s ->
                    s.copy(
                        contacts = s.contacts.map { if (it.id == id) final else it },
                        isLoading = false,
                        error = null
                    )
                }
                Log.d(TAG, "REMOTE/UPDATE başarılı, id=${updated.id}")
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Kişi güncellenemedi: ${e.message}") }
                Log.e(TAG, "REMOTE/UPDATE hata: ${e.message}", e)
            }
        }
    }


    fun deleteContact(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                remote.delete(id)
                _state.update { s ->
                    s.copy(
                        contacts = s.contacts.filterNot { it.id == id },
                        isLoading = false,
                        error = null
                    )
                }
                Log.d(TAG, "REMOTE/DELETE başarılı, id=$id")
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Kişi silinemedi: ${e.message}") }
                Log.e(TAG, "REMOTE/DELETE hata: ${e.message}", e)
            }
        }
    }



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



    @SuppressLint("UseKtx")
    private fun fileFromUriOrNull(ctx: Context, uriString: String?): java.io.File? {
        if (uriString.isNullOrBlank()) return null
        return runCatching {
            val uri = android.net.Uri.parse(uriString)
            val cr = ctx.contentResolver
            val mime = cr.getType(uri) ?: "image/jpeg"
            val ext = when {
                mime.contains("png") -> "png"
                else -> "jpg"
            }
            val input = cr.openInputStream(uri) ?: return null
            val tmp = java.io.File.createTempFile("upload_", ".$ext", ctx.cacheDir)
            input.use { ins -> tmp.outputStream().use { outs -> ins.copyTo(outs) } }
            tmp
        }.getOrNull()
    }

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

        } catch (_: Throwable) {

        }
        return list
    }


    private fun normalizePhone(raw: String): String = raw.filter { it.isDigit() }

}
