@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.strogholodapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.strogholodapp.categoriesMap
import com.example.strogholodapp.humanCategory

@Composable
fun AddProductScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    existingProduct: Product? = null,
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddProductViewModel = viewModel()
    val context = LocalContext.current

    // Сбрасываем флаг успеха и, если новый продукт, очищаем поля
    LaunchedEffect(existingProduct) {
        viewModel.resetSuccess()
        if (existingProduct != null) {
            viewModel.setProductForEdit(existingProduct)
        } else {
            viewModel.name.value = ""
            viewModel.price.value = ""
            viewModel.description.value = ""
            viewModel.selectedCategory.value = "Бонеты"
            viewModel.photoUri.value = null
        }
    }

    val name by viewModel.name.collectAsState()
    val price by viewModel.price.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val responseMessage by viewModel.responseMessage.collectAsState()
    val success by viewModel.success.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.photoUri.value = it } }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) viewModel.photoUri.value = cameraImageUri.value }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            cameraImageUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    // После успешной отправки один раз вызываем onSave и сразу сбрасываем success
    LaunchedEffect(success) {
        if (success) {
            val updatedAt = if (existingProduct == null || existingProduct.price != price) {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            } else {
                existingProduct.priceUpdatedAt
            }

            onSave(
                Product(
                    id = existingProduct?.id ?: 0,
                    name = name,
                    price = price,
                    description = description,
                    category = categoriesMap[selectedCategory] ?: "",
                    photo = photoUri?.toString() ?: "",
                    priceUpdatedAt = updatedAt
                )
            )
            viewModel.resetSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (existingProduct != null) "Редактировать оборудование" else "Добавить оборудование",
            style = MaterialTheme.typography.titleLarge
        )

        StyledTextField(value = name, onValueChange = { viewModel.name.value = it }, label = "Название")
        StyledTextField(value = price, onValueChange = { viewModel.price.value = it }, label = "Цена")

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Категория") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categoriesMap.keys.forEach { label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            viewModel.selectedCategory.value = label
                            expanded = false
                        }
                    )
                }
            }
        }

        StyledTextField(
            value = description,
            onValueChange = { viewModel.description.value = it },
            label = "Описание"
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Default.Photo, contentDescription = "Галерея")
                Spacer(Modifier.width(8.dp))
                Text("Из галереи")
            }

            OutlinedButton(
                onClick = {
                    val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        val uri = createImageUri(context)
                        cameraImageUri.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = "Камера")
                Spacer(Modifier.width(8.dp))
                Text("С камеры")
            }
        }

        photoUri?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { viewModel.submitProduct(context, categoriesMap, existingProduct) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить")
            }
            OutlinedButton(
                onClick = {
                    viewModel.resetSuccess()
                    onCancel()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }
        }

        responseMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

fun createImageUri(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo_$timestamp.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
