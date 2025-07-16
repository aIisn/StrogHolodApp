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

    private var editingProductId: Int? = null
    private var oldPhotoUrl: String? = null

    fun setProductForEdit(product: Product) {
        name.value = product.name
        price.value = product.price
        description.value = product.description ?: ""
        selectedCategory.value = humanCategory(product.category)
        photoUri.value = Uri.parse(product.photo)
        editingProductId = product.id
        oldPhotoUrl = product.photo
    }

    fun submitProduct(context: Context, categoriesMap: Map<String, String>, existingProduct: Product? = null) {
        viewModelScope.launch {
            try {
                val categoryCode = categoriesMap[selectedCategory.value] ?: ""
                var finalPhotoUrl = oldPhotoUrl

                val realFile = photoUri.value?.let { uri ->
                    FileUtils.getFileFromUri(context, uri)
                }

                // Если URI изменён и файл есть — загружаем новое фото
                if (realFile != null && realFile.exists()) {
                    val requestFile = realFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val photoPart = MultipartBody.Part.createFormData("photo", realFile.name, requestFile)
                    val categoryPart = MultipartBody.Part.createFormData("category", categoryCode)

                    val uploadResponse = api.uploadPhoto(photoPart, categoryPart)
                    if (!uploadResponse.success) {
                        _responseMessage.value = "Ошибка при загрузке фото: ${uploadResponse.message}"
                        _success.value = false
                        return@launch
                    }

                    finalPhotoUrl = uploadResponse.message
                }

                val priceUpdatedAt = if (existingProduct == null || existingProduct.price != price.value) {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                } else {
                    existingProduct.priceUpdatedAt
                }

                val product = Product(
                    id = editingProductId ?: 0,
                    name = name.value,
                    price = price.value,
                    description = description.value,
                    category = categoryCode,
                    photo = finalPhotoUrl ?: "",
                    priceUpdatedAt = priceUpdatedAt
                )


                val response = if (existingProduct != null) {
                    api.updateProduct(product)
                } else {
                    api.addProduct(product)
                }

                _responseMessage.value = response.message
                _success.value = response.success

            } catch (e: Exception) {
                _responseMessage.value = "Ошибка: ${e.message}"
                _success.value = false
            }
        }
    }

    fun resetSuccess() {
        _success.value = false
}


    private fun humanCategory(code: String): String {
        return when (code) {
            "Bonety" -> "Бонеты"
            "Lari" -> "Лари"
            "Vitriny" -> "Витрины"
            "Gorki_vstroennyj" -> "Горки встроенный холод"
            "Gorki_vynosnoj" -> "Горки выносной холод"
            "Shkafy_dvuhdvernye" -> "Шкафы двухдверные"
            "Shkafy_odnodvernye" -> "Шкафы однодверные"
            "Kassy" -> "Кассы"
            "Kuhonnoe_oborudovanie" -> "Кухонное оборудование"
            "Stellazhi" -> "Стеллажи"
            else -> "Бонеты"
        }
    }
}
