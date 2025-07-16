@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.strogholodapp.ApiClient
import com.example.strogholodapp.ApiService
import com.example.strogholodapp.DeleteRequest
import com.example.strogholodapp.Product
import com.example.strogholodapp.categoriesMap
import com.example.strogholodapp.ui.theme.CardBackground
import com.example.strogholodapp.ui.theme.GradientEnd
import com.example.strogholodapp.ui.theme.GradientStart
import com.example.strogholodapp.ui.theme.StrogHolodAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val selectedCategory = remember { mutableStateOf("Изменена цена") }
    var selectedItem by remember { mutableStateOf("Актуальные цены") }
    var showAddProductScreen by remember { mutableStateOf(false) }
    var editableProduct by remember { mutableStateOf<Product?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                listOf("Актуальные цены", "Калькулятор стеллажа")
                    .forEach { label ->
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
                        "Калькулятор стеллажа" -> PlaceholderScreen(
                            title = "Калькулятор стеллажа",
                            padding = paddingValues
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EquipmentScreen(
    modifier: Modifier = Modifier,
    onEditRequest: (Product) -> Unit
) {
    val allProducts = remember { mutableStateListOf<Product>() }
    val selectedCategory = LocalCategory.current
    val coroutineScope = rememberCoroutineScope()

    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productToClearHistory by remember { mutableStateOf<Product?>(null) }

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

    val key = categoriesMap[selectedCategory.value]
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    val filteredProducts = if (selectedCategory.value == "Изменена цена") {
        allProducts.sortedByDescending { product ->
            runCatching { dateFormat.parse(product.priceUpdatedAt)?.time }
                .getOrNull() ?: 0L
        }
    } else {
        allProducts.filter { key == null || it.category == key }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredProducts) { product ->
            ProductCard(
                product = product,
                onDeleteRequest = { productToDelete = it },
                onEditRequest = onEditRequest,
                onClearHistory = { productToClearHistory = it }
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
                    val resp = withContext(Dispatchers.IO) {
                        api.deleteProduct(DeleteRequest(product.id, product.photo))
                    }
                    if (resp.success) allProducts.remove(product)
                }
            },
            onDismiss = { productToDelete = null }
        )
    }

    productToClearHistory?.let { product ->
        ConfirmClearHistoryDialog(
            product = product,
            onConfirm = {
                productToClearHistory = null
                coroutineScope.launch {
                    val api = ApiClient.retrofit.create(ApiService::class.java)
                    val updated = product.copy(oldPrice = product.price)
                    val resp = withContext(Dispatchers.IO) {
                        api.updateProduct(updated)
                    }
                    if (resp.success) {
                        val idx = allProducts.indexOfFirst { it.id == product.id }
                        if (idx >= 0) allProducts[idx] = updated
                    }
                }
            },
            onDismiss = { productToClearHistory = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCard(
    product: Product,
    onDeleteRequest: (Product) -> Unit,
    onEditRequest: (Product) -> Unit,
    onClearHistory: (Product) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val isRecent = runCatching {
        val updated = dateFormat.parse(product.priceUpdatedAt)?.time ?: 0L
        System.currentTimeMillis() - updated <= 3L * 24 * 60 * 60 * 1000
    }.getOrDefault(false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(GradientStart, GradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = product.photo),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            if (isRecent && !product.oldPrice.isNullOrBlank() && product.oldPrice != product.price) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.oldPrice!!,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(text = product.price, fontSize = 14.sp, color = Color.Red)
                }
            } else {
                Text("Цена: ${product.price}", fontSize = 14.sp)
            }

            product.description?.let {
                Text(it, fontSize = 12.sp, maxLines = 3)
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                DropdownMenuItem(
                    text = { Text("Очистить историю цен") },
                    onClick = {
                        showMenu = false
                        onClearHistory(product)
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
fun ConfirmClearHistoryDialog(
    product: Product,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Очистить историю цен?") },
        text = { Text("Старая цена для «${product.name}» будет удалена.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Очистить") }
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
        "Изменена цена", "Бонеты", "Лари", "Витрины",
        "Горки встроенный холод", "Горки выносной холод",
        "Шкафы двухдверные", "Шкафы однодверные",
        "Кассы", "Кухонное оборудование", "Стеллажи"
    )

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        OutlinedButton(onClick = { expanded.value = true }) {
            Text(selectedCategory.value, maxLines = 1, color = Color.Black)
        }
        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            options.forEach { label ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.Black) },
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
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("Раздел \"$title\" пока в разработке", fontSize = 18.sp)
    }
}
