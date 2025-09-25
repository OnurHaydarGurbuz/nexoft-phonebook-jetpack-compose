package com.example.nexoft.core.model

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val photoUrl: String? = null,
    val isInDevice: Boolean = false
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}