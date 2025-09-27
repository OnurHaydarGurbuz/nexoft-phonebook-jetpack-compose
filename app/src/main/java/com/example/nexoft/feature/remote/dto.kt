package com.example.nexoft.feature.remote

data class ApiEnvelope<T>(
    val success: Boolean,
    val messages: List<String>?,
    val data: T?,
    val status: Int
)

data class UserDto(
    val id: String,
    val createdAt: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val profileImageUrl: String?
)

data class CreateUserRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null
)

data class UpdateUserRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val profileImageUrl: String? = null
)


data class UserListResponse(
    val users: List<UserDto>?   // Swagger: data { users: [...] }
)

data class UploadImageResponse(
    val imageUrl: String        // Swagger: imageUrl
)

class EmptyResponse              // DELETE: data:{}

