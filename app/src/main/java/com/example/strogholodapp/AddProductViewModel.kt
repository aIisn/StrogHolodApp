package com.example.strogholodapp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AddProductViewModel : ViewModel() {

    private val api = ApiClient.retrofit.create(ApiService::class.java)

    val name = MutableStateFlow("")
    val price = MutableStateFlow("")
    val description = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Бонеты")
    val photoUri = MutableStateFlow<Uri?>(null)

    private val _responseMessage = MutableStateFlow<String?>(null)
    val responseMessage: StateFlow<String?> = _responseMessage

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    fun submitProduct(context: Context, categoriesMap: Map<String, String>) {
        viewModelScope.launch {
            try {
                val realFile = photoUri.value?.let { uri ->
                    FileUtils.getFileFromUri(context, uri)
                }

                if (realFile == null) {
                    _responseMessage.value = "Ошибка: файл не найден"
                    _success.value = false
                    return@launch
                }

                val categoryCode = categoriesMap[selectedCategory.value] ?: ""

                // Создание тела multipart запроса
                val requestFile = realFile.asRequestBody("image/*".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData("photo", realFile.name, requestFile)
                val categoryPart = MultipartBody.Part.createFormData("category", categoryCode)

                // Отправляем фото и получаем ссылку
                val uploadResponse = api.uploadPhoto(photoPart, categoryPart)

                if (!uploadResponse.success) {
                    _responseMessage.value = "Ошибка при загрузке фото: ${uploadResponse.message}"
                    _success.value = false
                    return@launch
                }

                val product = Product(
                    id = 0,
                    name = name.value,
                    price = price.value,
                    description = description.value,
                    category = categoryCode,
                    photo = uploadResponse.message // ссылка на фото
                )

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
