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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.layout.ContentScale

@Composable
fun AddProductScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    existingProduct: Product? = null,
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddProductViewModel = viewModel()
    val context = LocalContext.current

    val name by viewModel.name.collectAsState()
    val price by viewModel.price.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val responseMessage by viewModel.responseMessage.collectAsState()
    val success by viewModel.success.collectAsState()

    val categoriesMap = mapOf(
        "Бонеты" to "Bonety",
        "Лари" to "Lari",
        "Витрины" to "Vitriny",
        "Горки встроенный холод" to "Gorki_vstroennyj",
        "Горки выносной холод" to "Gorki_vynosnoj",
        "Шкафы двухдверные" to "Shkafy_dvuhdvernye",
        "Шкафы однодверные" to "Shkafy_odnodvernye",
        "Кассы" to "Kassy",
        "Кухонное оборудование" to "Kuhonnoe_oborudovanie",
        "Стеллажи" to "Stellazhi"
    )

    var expanded by remember { mutableStateOf(false) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.photoUri.value = it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.photoUri.value = cameraImageUri.value
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            cameraImageUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(existingProduct) {
        existingProduct?.let { viewModel.setProductForEdit(it) }
    }

    LaunchedEffect(success) {
        if (success) {
            onSave(
                Product(
                    id = existingProduct?.id ?: 0,
                    name = name,
                    price = price,
                    description = description,
                    category = categoriesMap[selectedCategory] ?: "",
                    photo = photoUri?.toString() ?: ""
                )
            )
            // Сбросить флаг успеха после сохранения
            viewModel.resetSuccess()
        }
    }


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (existingProduct != null) "Редактировать оборудование" else "Добавить оборудование",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = price,
                onValueChange = { viewModel.price.value = it },
                label = { Text("Цена") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
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
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description.value = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Из галереи")
                }
                Button(onClick = {
                    val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permission == PackageManager.PERMISSION_GRANTED) {
                        val uri = createImageUri(context)
                        cameraImageUri.value = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("С камеры")
                }
            }
        }

        photoUri?.let { uri ->
            item {
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    contentScale = ContentScale.Crop
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.submitProduct(context, categoriesMap, existingProduct)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }
            }
        }

        item {
            responseMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun createImageUri(context: Context): Uri {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        "photo_$timestamp.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}
