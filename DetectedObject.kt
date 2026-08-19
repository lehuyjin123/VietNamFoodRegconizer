package com.example.vietnamfoodregconizer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Arrays // Import tường minh nếu cần

// Data class để biểu diễn một đối tượng được phát hiện bởi model (sau NMS)
@Parcelize // Làm cho nó Parcelable để có thể truyền qua Intent
data class DetectedObject(
    val label: String,      // Nhãn (tên lớp) của đối tượng được phát hiện
    val score: Float,       // Điểm tin cậy (confidence score) của phát hiện
    val boundingBox: FloatArray // Bounding Box: floatArrayOf(xmin, ymin, xmax, ymax) Tọa độ tuyệt đối trên ảnh gốc
) : Parcelable { // Implement Parcelable để có thể truyền qua Intent

    // Override equals và hashCode để so sánh mảng boundingBox đúng cách
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectedObject

        if (label != other.label) return false
        if (score != other.score) return false
        if (!Arrays.equals(boundingBox, other.boundingBox)) return false // So sánh mảng

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + Arrays.hashCode(boundingBox) // Hashcode cho mảng
        return result
    }

    override fun toString(): String {
        // Định dạng lại để hiển thị gọn hơn
        val formattedScore = String.format("%.2f", score)
        val formattedBox = boundingBox.joinToString(separator = ", ", prefix = "[", postfix = "]") { String.format("%.2f", it) }
        return "DetectedObject(label='$label', score=$formattedScore, boundingBox=$formattedBox)"
    }
}


// Data class để biểu diễn một mục trong hóa đơn
@Parcelize // Làm cho nó Parcelable để có thể truyền qua Intent
data class BillItem(
    val label: String,      // Tên món ăn
    val quantity: Int,      // Số lượng tính tiền (sau khi áp dụng quy tắc đếm > 5)
    val pricePerItem: Double // Giá tiền cho 1 đơn vị món ăn
) : Parcelable { // Implement Parcelable

    // Thuộc tính tính tổng tiền cho mục này (giá * số lượng)
    val subtotal: Double
        get() = quantity * pricePerItem

    override fun toString(): String {
        val formattedPrice = String.format("%,.0f", pricePerItem).replace(",", ".")
        val formattedSubtotal = String.format("%,.0f", subtotal).replace(",", ".")
        return "$label: $quantity x ${formattedPrice} VNĐ = ${formattedSubtotal} VNĐ"
    }
}