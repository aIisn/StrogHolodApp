package com.example.strogholodapp

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    padding: PaddingValues = PaddingValues(0.dp),
    onSave: (Product) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)        // 💡 внешний отступ от Scaffold
            .padding(16.dp),         // 💡 внутренний паддинг формы
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = photoUrl,
            onValueChange = { photoUrl = it },
            label = { Text("URL фотографии") },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                val newProduct = Product(
                    id = 0, // может быть заменено сервером
                    name = name,
                    price = price,
                    description = description,
                    photo = photoUrl,
                    category = categoriesMap[selectedCategory] ?: ""
                )
                Log.d("AddProduct", "Создан продукт: $newProduct")
                onSave(newProduct)
            }) {
                Text("Сохранить")
            }

            OutlinedButton(onClick = onCancel) {
                Text("Отмена")
            }
        }
    }
}