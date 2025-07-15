package com.example.strogholodapp

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddProductViewModel : ViewModel() {

    private val api = ApiClient.retrofit.create(ApiService::class.java)

    // Поля формы
    val name = MutableStateFlow("")
    val price = MutableStateFlow("")
    val description = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Бонеты")
    val photoUri = MutableStateFlow<Uri?>(null)

    // Ответ от сервера
    private val _responseMessage = MutableStateFlow<String?>(null)
    val responseMessage: StateFlow<String?> = _responseMessage

    // Успешность отправки
    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    // Отправка товара на сервер
    fun submitProduct(categoriesMap: Map<String, String>) {
        val product = Product(
            id = 0,
            name = name.value,
            price = price.value,
            description = description.value,
            photo = photoUri.value?.toString() ?: "",
            category = categoriesMap[selectedCategory.value] ?: ""
        )

        viewModelScope.launch {
            try {
                val response = api.addProduct(product)
                _responseMessage.value = response.message
                _success.value = response.success
            } catch (e: Exception) {
                _responseMessage.value = "Ошибка: ${e.message}"
                _success.value = false
            }
        }
    }
}
