package com.example.nexoft.feature.remote


import com.example.nexoft.core.model.Contact


fun UserDto.toDomain() = Contact(
    id = id,
    firstName = firstName,
    lastName = lastName,
    phone = phoneNumber,
    photoUrl = profileImageUrl,
    isInDevice = false
)
