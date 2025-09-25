package com.example.nexoft.feature.contacts

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ContactsState(
    val contacts: List<com.example.nexoft.core.model.Contact> = emptyList(),
    val searchQuery: String = ""
)

sealed class ContactsEvent {
    data class OnSearchChanged(val q: String) : ContactsEvent()
}

class ContactsViewModel : ViewModel() {
    private val _state = MutableStateFlow(ContactsState())
    val state = _state.asStateFlow()

    fun onEvent(event: ContactsEvent) {
        when (event) {
            is ContactsEvent.OnSearchChanged ->
                _state.value = _state.value.copy(searchQuery = event.q)
        }
    }
    fun addContact(first: String, last: String, phone: String, photoUri: String?) {
        val newOne = com.example.nexoft.core.model.Contact(
            id = java.util.UUID.randomUUID().toString(),
            firstName = first,
            lastName = last,
            phone = phone,
            photoUrl = photoUri,
            isInDevice = false
        )
        _state.value = _state.value.copy(contacts = _state.value.contacts + newOne)
    }
}
