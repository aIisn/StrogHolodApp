package com.example.strogholodapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import androidx.compose.material.icons.filled.Add

// CompositionLocal для категории
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

data class Product(
    val id: Int,
    val name: String,
    val price: String,
    val description: String?,
    val photo: String,
    val category: String
)

interface ApiService {
    @GET("get_products.php")
    suspend fun getProducts(): List<Product>
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val selectedCategory = remember { mutableStateOf("Все") }

    val navigationItems = listOf("Актуальные цены", "Калькулятор стеллажа")
    var selectedItem by remember { mutableStateOf(navigationItems[0]) }
    var showAddProductScreen by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                navigationItems.forEach { label ->
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
                        onClick = { showAddProductScreen = true },
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
                        onSave = { product ->
                            Log.d("AddProduct", "Создан продукт: $product")
                            showAddProductScreen = false
                        },
                        onCancel = {
                            showAddProductScreen = false
                        }
                    )
                } else {
                    when (selectedItem) {
                        "Актуальные цены" -> EquipmentScreen(Modifier.padding(paddingValues))
                        "Калькулятор стеллажа" -> PlaceholderScreen("Калькулятор стеллажа", paddingValues)
                    }
                }
            }
        }
    }
}


@Composable
fun EquipmentScreen(modifier: Modifier = Modifier) {
    val allProducts = remember { mutableStateListOf<Product>() }
    val selectedCategory = LocalCategory.current

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

    LaunchedEffect(Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://formanagers.strogholod.ru/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

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
            ProductCard(product)
        }
    }
}

@Composable
fun ProductCard(product: Product) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
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

            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Цена: ${product.price}",
                fontSize = 14.sp
            )
            product.description?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    maxLines = 3
                )
            }
        }
    }
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
