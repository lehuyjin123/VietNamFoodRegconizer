package com.example.vietnamfoodregconizer

data class RestaurantData(
    val restaurant: String?,
    val foodItems: List<FoodItem>?
)
data class FoodItem(
    val id: Int,
    val foodName: String?, // Tên món ăn trong JSON (sẽ dùng làm key tra cứu)
    val foodType: String?,
    val calories: Int?,
    val price: Double?     // Giá tiền (Double? để xử lý null)
)