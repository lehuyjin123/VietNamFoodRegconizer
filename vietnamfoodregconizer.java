package com.example.vietnamfoodregconizer; // Đảm bảo đúng package name của bạn

import android.app.Activity;
import android.os.Bundle; // Cần import Bundle
import android.widget.ImageView; // Cần import ImageView
import android.widget.TextView; // Cần import TextView

import java.text.NumberFormat; // Import cho định dạng tiền tệ
import java.util.Locale; // Import cho định dạng tiền tệ

// Đảm bảo bạn đã import lớp R của dự án của mình
// import com.your_app_name.R; // Thay com.your_app_name bằng package gốc dự án của bạn

public class vietnamfoodregconizer extends Activity {

    // Khai báo các biến để giữ tham chiếu đến ImageView và TextView trong layout
    private ImageView ivBankQrCode;
    private TextView tvTotalAmount;

    // Biến để lưu tổng tiền hiện tại
    private double currentTotalAmount = 0.0;

    // Phương thức này được gọi khi Activity được tạo lần đầu tiên
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Luôn gọi super.onCreate()

        setContentView(R.layout.layout); // <-- Ví dụ: Nếu layout của bạn là activity_checkout.xml

        // --- Ánh xạ các View từ layout XML vào các biến đã khai báo ---
        // Sử dụng ID mà bạn đã đặt trong file XML layout

        ivBankQrCode = findViewById(R.id.ivBankQrCode); // <-- Thay R.id.ivBankQrCode bằng ID của ImageView QR trong layout của bạn
        tvTotalAmount = findViewById(R.id.tvTotalAmount); // <-- Thay R.id.tvTotalAmount bằng ID của TextView tổng tiền trong layout của bạn

        // --- Thiết lập trạng thái ban đầu hoặc các Listener nếu cần ---

        // Cập nhật hiển thị tổng tiền ban đầu (ví dụ: 0)
        updateTotalAmount(currentTotalAmount);
    }

    public void addAndConfirmItem(double price) {
        // (Tùy chọn) Thêm món này vào danh sách giỏ hàng của bạn (logic quản lý giỏ hàng)
        // ...

        // Cộng giá của món vừa thêm vào tổng tiền hiện tại
        currentTotalAmount += price;

        // Cập nhật hiển thị tổng tiền trên giao diện
        updateTotalAmount(currentTotalAmount);
    }

    // Hàm để cập nhật hiển thị tổng tiền trên TextView
    // Hàm này đã được giải thích chi tiết trước đó
    public void updateTotalAmount(double amount) {
        currentTotalAmount = amount; // Lưu lại giá trị tổng tiền

        // Định dạng số tiền theo định dạng tiền Việt (có dấu phân cách hàng nghìn)
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        String formattedAmount = formatter.format(amount);

        // Cập nhật text cho TextView tổng tiền
        tvTotalAmount.setText("Tổng cộng: " + formattedAmount + " VNĐ");
    }

    // --- Các hàm xử lý sự kiện (ví dụ: bấm nút) nếu có ---
    // ...
}