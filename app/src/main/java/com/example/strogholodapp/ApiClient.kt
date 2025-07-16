package com.example.strogholodapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder

object ApiClient {
    val retrofit: Retrofit by lazy {
        val gson = GsonBuilder()
            // Это позволит GSON автоматически мапить JSON-поля вида price_updated_at → priceUpdatedAt
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

        Retrofit.Builder()
            .baseUrl("https://formanagers.strogholod.ru/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
