package com.example.nexoft.feature.remote

import com.example.nexoft.core.model.Contact
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ContactsRepositoryImpl(
    private val api: ContactsApi
) : ContactsRepository {

    override suspend fun getAll(): List<Contact> {
        val res = api.getAllUsers()
        require(res.success && res.data?.users != null) { res.messages?.joinToString().orEmpty() }
        return res.data!!.users!!.map { it.toDomain() }
    }

    override suspend fun create(contact: Contact, photoFile: File?): Contact {
        val url = photoFile?.let { upload(it) }
        val body = CreateUserRequest(
            firstName = contact.firstName,
            lastName = contact.lastName,
            phoneNumber = contact.phone,
            profileImageUrl = url ?: contact.photoUrl
        )
        val res = api.createUser(body)
        require(res.success && res.data != null) { res.messages?.joinToString().orEmpty() }
        return res.data.toDomain()
    }

    override suspend fun update(contact: Contact, photoFile: File?): Contact {
        val url = photoFile?.let { upload(it) }
        val body = UpdateUserRequest(
            firstName = contact.firstName,
            lastName = contact.lastName,
            phoneNumber = contact.phone,
            profileImageUrl = url ?: contact.photoUrl
        )
        val res = api.updateUser(contact.id, body)
        require(res.success && res.data != null) { res.messages?.joinToString().orEmpty() }
        return res.data.toDomain()
    }

    override suspend fun delete(id: String) {
        val res = api.deleteUser(id)
        require(res.success) { res.messages?.joinToString().orEmpty() }
    }

    private suspend fun upload(file: File): String {
        val ext = file.extension.lowercase()
        require(ext in listOf("jpg", "jpeg", "png")) { "Only jpg/png allowed" }
        val mime = if (ext == "png") "image/png" else "image/jpeg"
        val part = MultipartBody.Part.createFormData(
            /* name = */ "image", // Swagger'da field adı "image"
            /* filename = */ file.name,
            /* body = */ file.asRequestBody(mime.toMediaType())
        )
        val res = api.uploadImage(part)
        require(res.success && res.data != null) { res.messages?.joinToString().orEmpty() }
        return res.data.imageUrl
    }
}
