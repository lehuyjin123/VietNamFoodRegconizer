package com.example.vietnamfoodregconizer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class QRpay : AppCompatActivity() {
    // *** Biến lưu tổng tiền được khai báo ở đây ***
    private var calculatedTotal: Double = 0.0

    private lateinit var qrImageView: ImageView
    private lateinit var confirmButton: Button
    private lateinit var priceTextView: TextView

    // *** Hằng số EXTRA_CALCULATED_TOTAL được định nghĩa ở đây, trong companion object ***
    companion object {
        const val EXTRA_BILL_ITEMS = "extra_bill_items"
        const val EXTRA_CALCULATED_TOTAL = "calculated_total"
    }
    // *** Kết thúc định nghĩa companion object ***


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout) // Đảm bảo tên layout XML đúng

        // Lấy tham chiếu đến các View bằng ID (Đảm bảo ID trong XML trùng khớp)
        qrImageView = findViewById(R.id.ivBankQrCode) // Đảm bảo ID ivBankQrCode có trong layout
        confirmButton = findViewById(R.id.confirmButton) // Đảm bảo ID confirmButton có trong layout
        priceTextView = findViewById(R.id.tvTotalAmount) // Đảm bảo ID tvTotalAmount có trong layout

        // --- Lấy tổng tiền thực tế từ Intent extras ---
        // Sử dụng key EXTRA_CALCULATED_TOTAL đã định nghĩa
        val totalFromPreviousScreen = intent.getDoubleExtra(EXTRA_CALCULATED_TOTAL, 0.0) // Default là 0.0 nếu không tìm thấy extra

        // --- Gọi handleRecognitionResult với tổng tiền thực tế đã nhận được ---
        handleRecognitionResult(totalFromPreviousScreen)


        // --- Thiết lập sự kiện cho nút xác nhận ---
        confirmButton.setOnClickListener {
            // Logic khi nút xác nhận được bấm
            val vietnamLocale = Locale("vi", "VN")
            val currencyFormat = NumberFormat.getCurrencyInstance(vietnamLocale)
            val formattedTotal = currencyFormat.format(calculatedTotal)
                .replace("₫", "VNĐ")
                .replace(".", ",")

            priceTextView.text = "Tổng cộng: $formattedTotal"
            finish() // Đóng Activity QRpay hiện tại
            // priceTextView.visibility = View.VISIBLE // Đã set visibility trong handleRecognitionResult
        }
    }

    /**
     * Cập nhật tổng tiền và trạng thái hiển thị của QR và nút xác nhận.
     * Được gọi sau khi nhận diện món ăn và tính tổng tiền.
     * @param total Tổng tiền tính được.
     */
    private fun handleRecognitionResult(total: Double) {
        calculatedTotal = total // Gán giá trị total vào biến của Activity

        // Định dạng và hiển thị tổng tiền
        val vietnamLocale = Locale("vi", "VN")
        val currencyFormat = NumberFormat.getCurrencyInstance(vietnamLocale)
        val formattedTotal = currencyFormat.format(calculatedTotal)
            .replace("₫", "VNĐ")
            .replace(".", ",")

        priceTextView.text = "Tổng cộng: $formattedTotal"
        priceTextView.visibility = View.VISIBLE // Luôn hiển thị TextView tổng tiền

        // Kiểm tra tổng tiền để hiển thị/ẩn QR và nút xác nhận
        if (calculatedTotal > 0) {
            // TODO: Thêm logic tạo mã QR tại đây dựa trên calculatedTotal
            qrImageView.visibility = View.VISIBLE // Hiển thị ImageView QR
            confirmButton.visibility = View.VISIBLE // Hiển thị nút xác nhận
        } else {
            // Nếu tổng tiền là 0 hoặc âm, ẩn QR và nút xác nhận
            qrImageView.visibility = View.GONE // Hoặc View.INVISIBLE
            confirmButton.visibility = View.GONE // Hoặc View.INVISIBLE
        }
    }
}