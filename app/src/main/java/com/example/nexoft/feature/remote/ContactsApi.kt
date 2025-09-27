package com.example.nexoft.feature.remote

import okhttp3.MultipartBody
import retrofit2.http.*

interface ContactsApi {

    @POST("api/User")
    suspend fun createUser(@Body body: CreateUserRequest): ApiEnvelope<UserDto>

    @GET("api/User/{id}")
    suspend fun getUser(@Path("id") id: String): ApiEnvelope<UserDto>

    @PUT("api/User/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body body: UpdateUserRequest
    ): ApiEnvelope<UserDto>

    @DELETE("api/User/{id}")
    suspend fun deleteUser(@Path("id") id: String): ApiEnvelope<EmptyResponse>

    @GET("api/User/GetAll")
    suspend fun getAllUsers(): ApiEnvelope<UserListResponse>

    @Multipart
    @POST("api/User/UploadImage")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): ApiEnvelope<UploadImageResponse>
}
