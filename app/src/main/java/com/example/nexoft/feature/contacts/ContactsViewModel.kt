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
}
