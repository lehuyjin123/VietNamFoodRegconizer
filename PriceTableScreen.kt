package com.example.vietnamfoodregconizer

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader


// ---------- ĐỌC FILE JSON GỐC ----------
fun readJsonFromRaw(context: Context, resourceId: Int): String? {
    return try {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        reader.close()
        stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun parseFoodItemsJson(jsonString: String?): List<FoodItem>? {
    if (jsonString == null) return null
    return try {
        val gson = Gson()
        val listRestaurantDataType = object : TypeToken<List<RestaurantData>>() {}.type
        val restaurantDataList: List<RestaurantData> = gson.fromJson(jsonString, listRestaurantDataType)

        if (restaurantDataList.isNotEmpty()) {
            val originalFoodItems = restaurantDataList.first().foodItems
            originalFoodItems?.mapIndexed { index, item ->
                FoodItem(
                    id = index,
                    foodName = item.foodName,
                    foodType = item.foodType,
                    calories = item.calories,
                    price = item.price
                )
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ---------- LƯU VÀ TẢI GIÁ (SharedPreferences) ----------
fun savePrices(context: Context, foodItems: List<FoodItem>) {
    val gson = Gson()
    val jsonString = gson.toJson(foodItems)
    val prefs = context.getSharedPreferences("food_data", Context.MODE_PRIVATE)
    prefs.edit().putString("saved_prices", jsonString).apply()
}

fun loadPrices(context: Context): List<FoodItem> {
    val prefs = context.getSharedPreferences("food_data", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("saved_prices", null)
    return if (jsonString != null) {
        val type = object : TypeToken<List<FoodItem>>() {}.type
        Gson().fromJson(jsonString, type)
    } else emptyList()
}

// ---------- COMPOSABLE CHÍNH ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTableScreen(navController: NavController) {
    val context = LocalContext.current
    var foodItems by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load dữ liệu khi mở màn hình
    LaunchedEffect(Unit) {
        isLoading = true
        val saved = loadPrices(context)
        foodItems = if (saved.isNotEmpty()) {
            saved
        } else {
            val jsonString = readJsonFromRaw(context, R.raw.price_table)
            parseFoodItemsJson(jsonString) ?: emptyList()
        }
        isLoading = false
        Log.d("PriceTable", "Loaded ${foodItems.size} items")
    }

    // Khi người dùng chỉnh sửa giá
    val updatePrice: (Int, Double) -> Unit = { itemId, newPrice ->
        foodItems = foodItems.map { item ->
            if (item.id == itemId) item.copy(price = newPrice) else item
        }
        savePrices(context, foodItems)
        Log.d("PriceTable", "Updated price for item $itemId: $newPrice")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Price Table") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading price table...")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = foodItems,
                        key = { it.id }
                    ) { item ->
                        FoodItemRow(item = item, onPriceChange = updatePrice)
                        Divider()
                    }
                }
            }
        }
    }
}

// ---------- ROW MỖI MÓN ĂN ----------
@Composable
fun FoodItemRow(item: FoodItem, onPriceChange: (Int, Double) -> Unit) {
    var priceText by remember { mutableStateOf(item.price?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.foodName.toString(),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${item.calories} kcal",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextField(
                value = priceText,
                onValueChange = {
                    priceText = it
                    it.toDoubleOrNull()?.let { price ->
                        onPriceChange(item.id, price)
                    }
                },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}
