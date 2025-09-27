package com.example.nexoft.feature.remote

import com.example.nexoft.core.model.Contact
import java.io.File

interface ContactsRepository {
    suspend fun getAll(): List<Contact>
    suspend fun create(contact: Contact, photoFile: File?): Contact
    suspend fun update(contact: Contact, photoFile: File?): Contact
    suspend fun delete(id: String)
}
