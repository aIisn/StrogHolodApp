package com.example.strogholodapp

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    // Получение списка оборудования
    @GET("get_products.php")
    suspend fun getProducts(): List<Product>

    // Добавление карточки оборудования (JSON)
    @POST("add_product.php")
    suspend fun addProduct(@Body product: Product): ServerResponse

    // Загрузка изображения (multipart)
    @Multipart
    @POST("upload_photo.php")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
        @Part category: MultipartBody.Part
    ): ServerResponse
}

// Ответ от сервера
data class ServerResponse(
    val success: Boolean,
    val message: String
)
