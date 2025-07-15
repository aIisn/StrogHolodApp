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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

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
    var selectedCategory by remember { mutableStateOf(categoriesMap.keys.first()) }

    // Галерея
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { photoUri = it }
    }

    // Камера
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = cameraImageUri.value
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageUri(context)
            cameraImageUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Добавить оборудование", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Цена") },
            modifier = Modifier.fillMaxWidth()
        )

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
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categoriesMap.keys.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Из галереи")
            }
            Button(onClick = {
                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    val uri = createImageUri(context)
                    cameraImageUri.value = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("С камеры")
            }
        }

        photoUri?.let {
            Image(
                painter = rememberAsyncImagePainter(model = it),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .padding(top = 8.dp),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val newProduct = Product(
                        id = 0,
                        name = name,
                        price = price,
                        description = description,
                        photo = photoUri?.toString() ?: "",
                        category = categoriesMap[selectedCategory] ?: ""
                    )
                    onSave(newProduct)
                },
                enabled = name.isNotBlank() && price.isNotBlank() && photoUri != null
            ) {
                Text("Сохранить")
            }

            OutlinedButton(onClick = onCancel) {
                Text("Отмена")
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
