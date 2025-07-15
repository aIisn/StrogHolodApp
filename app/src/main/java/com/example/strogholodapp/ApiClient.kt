package com.example.strogholodapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://formanagers.strogholod.ru/api/") // ← без public_html
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
