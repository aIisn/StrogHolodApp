package com.example.strogholodapp

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {
    @GET("get_products.php")
    suspend fun getProducts(): List<Product>

    @POST("add_product.php")
    suspend fun addProduct(@Body product: Product): ServerResponse

    @POST("delete_product.php")
    suspend fun deleteProduct(@Body request: DeleteRequest): ServerResponse

    @Multipart
    @POST("upload_image.php")
    suspend fun uploadPhoto(
        @Part photo: MultipartBody.Part,
        @Part category: MultipartBody.Part
    ): ServerResponse
}

data class ServerResponse(
    val success: Boolean,
    val message: String
)

data class DeleteRequest(
    val id: Int,
    val photo: String
)
