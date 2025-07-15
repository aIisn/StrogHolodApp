package com.example.strogholodapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.strogholodapp.ui.theme.StrogHolodAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalCategory = compositionLocalOf<MutableState<String>> {
    error("No category provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StrogHolodAppTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val selectedCategory = remember { mutableStateOf("Все") }
    var selectedItem by remember { mutableStateOf("Актуальные цены") }
    var showAddProductScreen by remember { mutableStateOf(false) }
    var editableProduct by remember { mutableStateOf<Product?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                listOf("Актуальные цены", "Калькулятор стеллажа").forEach { label ->
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = label == selectedItem,
                        onClick = {
                            selectedItem = label
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("StrogHolod") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        CategoryFilterDropdown(selectedCategory)
                    }
                )
            },
            floatingActionButton = {
                if (!showAddProductScreen && selectedItem == "Актуальные цены") {
                    FloatingActionButton(
                        onClick = {
                            editableProduct = null
                            showAddProductScreen = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
            }
        ) { paddingValues ->
            CompositionLocalProvider(LocalCategory provides selectedCategory) {
                if (showAddProductScreen) {
                    AddProductScreen(
                        padding = paddingValues,
                        existingProduct = editableProduct,
                        onSave = {
                            showAddProductScreen = false
                            editableProduct = null
                        },
                        onCancel = {
                            showAddProductScreen = false
                            editableProduct = null
                        }
                    )
                } else {
                    when (selectedItem) {
                        "Актуальные цены" -> EquipmentScreen(
                            modifier = Modifier.padding(paddingValues),
                            onEditRequest = {
                                editableProduct = it
                                showAddProductScreen = true
                            }
                        )
                        "Калькулятор стеллажа" -> PlaceholderScreen("Калькулятор стеллажа", paddingValues)
                    }
                }
            }
        }
    }
}

@Composable
fun EquipmentScreen(
    modifier: Modifier = Modifier,
    onEditRequest: (Product) -> Unit
) {
    val allProducts = remember { mutableStateListOf<Product>() }
    val selectedCategory = LocalCategory.current
    val coroutineScope = rememberCoroutineScope()
    val categoriesMap = mapOf(
        "Все" to null,
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

    var productToDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        val api = ApiClient.retrofit.create(ApiService::class.java)
        try {
            val response = withContext(Dispatchers.IO) { api.getProducts() }
            allProducts.clear()
            allProducts.addAll(response)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val filteredProducts = allProducts.filter {
        val key = categoriesMap[selectedCategory.value]
        key == null || it.category == key
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredProducts) { product ->
            ProductCard(
                product = product,
                onDeleteRequest = { productToDelete = it },
                onDeleted = { allProducts.remove(it) },
                onEditRequest = onEditRequest
            )
        }
    }

    productToDelete?.let { product ->
        ConfirmDeleteDialog(
            product = product,
            onConfirm = {
                productToDelete = null
                coroutineScope.launch {
                    val api = ApiClient.retrofit.create(ApiService::class.java)
                    try {
                        val response = withContext(Dispatchers.IO) {
                            api.deleteProduct(DeleteRequest(product.id, product.photo))
                        }
                        if (response.success) {
                            allProducts.remove(product)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onDismiss = { productToDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCard(
    product: Product,
    onDeleteRequest: (Product) -> Unit,
    onDeleted: (Product) -> Unit,
    onEditRequest: (Product) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = product.photo),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Цена: ${product.price}", fontSize = 14.sp)
            product.description?.let {
                Text(it, fontSize = 12.sp, maxLines = 3)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    onClick = {
                        showMenu = false
                        onEditRequest(product)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Удалить") },
                    onClick = {
                        showMenu = false
                        onDeleteRequest(product)
                    }
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    product: Product,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить товар?") },
        text = { Text("Вы уверены, что хотите удалить «${product.name}»?") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Удалить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun CategoryFilterDropdown(selectedCategory: MutableState<String>) {
    val expanded = remember { mutableStateOf(false) }
    val options = listOf(
        "Все",
        "Бонеты",
        "Лари",
        "Витрины",
        "Горки встроенный холод",
        "Горки выносной холод",
        "Шкафы двухдверные",
        "Шкафы однодверные",
        "Кассы",
        "Кухонное оборудование",
        "Стеллажи"
    )

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        OutlinedButton(onClick = { expanded.value = true }) {
            Text(text = selectedCategory.value, maxLines = 1)
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            options.forEach { label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        selectedCategory.value = label
                        expanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("Раздел \"$title\" пока в разработке", fontSize = 18.sp)
    }
}
