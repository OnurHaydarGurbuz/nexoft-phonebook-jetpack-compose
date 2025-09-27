package com.example.nexoft.feature.remote


object ServiceLocator {
    val api: ContactsApi by lazy { retrofit.create(ContactsApi::class.java) }
}
