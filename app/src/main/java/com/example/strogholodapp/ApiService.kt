package com.example.strogholodapp

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("get_products.php")
    suspend fun getProducts(): List<Product>

    @POST("add_product.php")
    suspend fun addProduct(@Body product: Product): ServerResponse
}

data class ServerResponse(
    val success: Boolean,
    val message: String
)
