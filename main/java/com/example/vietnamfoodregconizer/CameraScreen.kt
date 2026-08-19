package com.example.vietnamfoodregconizer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min


/**
 * Load dữ liệu giá tiền món ăn từ file JSON trong thư mục raw.
 * @param context Application context.
 * @param resourceId ID của file JSON trong thư mục res/raw (ví dụ: R.raw.price_table).
 * @return Map chứa tên món ăn và giá (Map<String, Double>), trả về emptyMap() nếu có lỗi.
 */
fun loadFoodPricesFromJson(context: Context, resourceId: Int): Map<String, Double> {
    val gson = Gson()
    var inputStream: InputStream? = null
    try {
        // Mở file từ thư mục raw sử dụng resource ID
        inputStream = context.resources.openRawResource(resourceId)
        // Tạo BufferedReader để đọc dữ liệu
        val reader = inputStream.bufferedReader()

        // Định nghĩa kiểu dữ liệu đích là List<RestaurantData>
        val type = object : TypeToken<List<RestaurantData>>() {}.type

        // Phân tích cú pháp JSON thành một danh sách các đối tượng RestaurantData
        val restaurantList: List<RestaurantData> = gson.fromJson(reader, type)

        // Tạo một mutable map để lưu trữ tên món ăn và giá (Non-nullable Double)
        val foodPricesMap = mutableMapOf<String, Double>()

        // Lặp qua danh sách nhà hàng và món ăn
        for (restaurantData in restaurantList) {
            // Kiểm tra null cho foodItems list
            val foodItems = restaurantData.foodItems ?: continue
            for (foodItem in foodItems) {
                // Kiểm tra null cho foodName và price
                val foodName = foodItem.foodName
                val price = foodItem.price

                if (foodName != null && price != null) {
                    foodPricesMap[foodName] = price // Thêm vào map nếu tên và giá không null
                } else {
                    // Tùy chọn: Log cảnh báo nếu giá trị price hoặc foodName là null trong JSON
                    Log.w("CameraScreen", "Food item with null name or price found in JSON. Skipping.")
                }
            }
        }

        // Trả về map chứa tên món ăn và giá
        return foodPricesMap

    } catch (e: Resources.NotFoundException) {
        Log.e("CameraScreen", "Resource not found for ID: $resourceId", e)
        return emptyMap()
    } catch (e: IOException) {
        Log.e("CameraScreen", "Error reading raw resource with ID: $resourceId", e)
        return emptyMap()
    } catch (e: Exception) {
        Log.e("CameraScreen", "Error parsing JSON structure from raw resource ID: $resourceId", e)
        return emptyMap()
    }
    finally {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            Log.e("CameraScreen", "Error closing input stream", e)
        }
    }
}

/**
* @param detectedObjects Danh sách các DetectedObject từ model (sau NMS).
* @param prices Map chứa giá tiền {tên_món_ăn -> giá}.
* @param countThreshold Ngưỡng đếm tối đa: nếu số lần nhận diện > ngưỡng này, số lượng tính tiền là 1.
* @param minCountThreshold Ngưỡng đếm tối thiểu: nếu số lần nhận diện < ngưỡng này, loại bỏ hoàn toàn.
* @return Danh sách các BillItem.
*/
fun generateBillItems(
    detectedObjects: List<DetectedObject>,
    prices: Map<String, Double>,
    countThreshold: Int = 5, // Ngưỡng trên: > 5 tính 1
    minCountThreshold: Int = 3 // Ngưỡng dưới: < 3 loại bỏ
): List<BillItem> {
    val labelCounts = mutableMapOf<String, Int>()

    // Bước 1: Đếm số lần xuất hiện của mỗi nhãn
    for (obj in detectedObjects) {
        labelCounts[obj.label] = labelCounts.getOrDefault(obj.label, 0) + 1
    }

    Log.d("CameraScreen", "Detected Label Counts (before custom rules): $labelCounts")

    val billItems = mutableListOf<BillItem>()

    // Bước 2: Tạo BillItem cho mỗi nhãn dựa trên số đếm và 2 quy tắc
    for ((label, count) in labelCounts) {
        // --- Quy tắc lọc: Nếu số đếm dưới ngưỡng tối thiểu, bỏ qua món này ---
        if (count < minCountThreshold) {
            Log.d("CameraScreen", "Filtering out item '$label': detected $count times (below min threshold $minCountThreshold).")
            continue // Bỏ qua món này, không thêm vào billItems
        }

        // --- Nếu số đếm đạt ngưỡng tối thiểu trở lên, xác định số lượng tính tiền và thêm vào bill ---
        val price = prices[label]
        if (price != null) {
            // Áp dụng quy tắc số lượng tính tiền (cho count >= minCountThreshold)
            val quantityToBill = if (count > countThreshold) {
                1 // Nếu số đếm > countThreshold (5), số lượng tính tiền là 1
            } else {
                count // Nếu minCountThreshold <= count <= countThreshold, số lượng tính tiền là số đếm thực tế
            }

            // Chỉ thêm vào danh sách bill nếu số lượng tính tiền > 0 (luôn đúng nếu count >= minCountThreshold và price != null)
            if (quantityToBill > 0) {
                billItems.add(BillItem(label, quantityToBill, price))
                // Log chi tiết quá trình tạo BillItem (Tên, Đếm thực, Số lượng tính tiền, Giá)
                Log.d("CameraScreen", "Generated BillItem: $label, Detected Count: $count, Billing Quantity: $quantityToBill, Price per item: $price")
            }

        } else {
            // Log cảnh báo nếu không tìm thấy giá cho nhãn này
            Log.w("CameraScreen", "Price not found for detected object: $label. Not added to bill.")
        }
    }

    return billItems // Trả về danh sách các BillItem đã tạo
}


// HÀM TÍNH TỔNG TIỀN TỪ DANH SÁCH BILL ITEM
fun calculateTotalPriceFromBillItems(billItems: List<BillItem>): Double {
    return billItems.sumOf { it.subtotal }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Sử dụng DetectedObject đã được định nghĩa ở nơi khác
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var recognitionResultText by remember { mutableStateOf("") } // Hiển thị kết quả nhận diện và tổng tiền

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Khởi tạo TFLiteHelper (Single Model)
    val classifier = remember(context) {
        TFLiteHelper(context).apply { initialize() }
    }

    // LOAD DỮ LIỆU GIÁ TIỀN TỪ JSON KHI COMPOSABLE ĐƯỢC TẠO LẦN ĐẦU
    val foodPrices: Map<String, Double> = remember(context) {
        // Truyền ID tài nguyên của tệp price_table.json trong thư mục raw
        loadFoodPricesFromJson(context, R.raw.price_table) // Đảm bảo bạn có file price_table.json trong res/raw
    }

    // Request camera permission on launch
    LaunchedEffect(cameraPermissionState) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Camera preview setup
    LaunchedEffect(cameraProviderFuture, lifecycleOwner, previewView) {
        val cameraProvider = cameraProviderFuture.get()

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = androidx.camera.core.CameraSelector.Builder()
            .requireLensFacing(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("CameraScreen", "Use case binding failed", exc)
            // Toast.makeText(context, "Failed to start camera preview", Toast.LENGTH_SHORT).show() // Toast không nên trong Composable thuần
            recognitionResultText = "Lỗi khởi động camera." // Hiển thị lỗi trên UI
        }
    }


    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                // Đảm bảo dữ liệu giá tiền đã được load thành công trước khi chụp
                if (foodPrices.isEmpty()) {
                    Toast.makeText(context, "Price data not loaded. Cannot calculate total.", Toast.LENGTH_SHORT).show()
                    recognitionResultText = "Dữ liệu giá chưa load. Không thể nhận diện." // Hiển thị lỗi trên UI
                    return@Button
                }

                val photoFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture?.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context), // Chạy callback trên Main Thread
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val savedUri = Uri.fromFile(photoFile)
                            // Cập nhật UI state cho ảnh chụp (trên Main Thread)
                            imageUri = savedUri

                            // Bắt đầu một Coroutine trên IO dispatcher để xử lý ảnh và model
                            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    // Decode file ảnh (trên luồng nền)
                                    val options = BitmapFactory.Options()
                                    // TODO: Tùy chọn: Giảm mẫu ảnh nếu cần. Cần implement calculateInSampleSize
                                    // options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
                                    val bitmap = BitmapFactory.decodeFile(savedUri.path, options)

                                    if (bitmap != null) {
                                        // --- Chạy model và xử lý kết quả (trên luồng nền) ---
                                        val results = classifier.analyzeBitmap(bitmap) // results là List<DetectedObject> sau NMS

                                        // --- Tạo danh sách các mục trong hóa đơn từ kết quả NMS và giá ---
                                        val billItems = generateBillItems(results, foodPrices, 5) // Áp dụng quy tắc đếm > 5

                                        // --- Tính tổng tiền từ danh sách billItems ---
                                        val calculatedTotal = calculateTotalPriceFromBillItems(billItems) // Tính tổng từ BillItems


                                        // --- Cập nhật UI state trên Main Thread và khởi chạy Activity ---
                                        withContext(Dispatchers.Main) { // Chuyển về Main Thread để cập nhật UI và khởi chạy Activity
                                            capturedBitmap = bitmap // Cập nhật state bitmap hiển thị
                                            detectedObjects = results // Cập nhật state với kết quả nhận diện (để vẽ box)

                                            // Cập nhật text hiển thị kết quả
                                            if (results.isNotEmpty()) {
                                                recognitionResultText = "Đã nhận diện: ${results.size} đối tượng.\n" +
                                                        "Tổng tiền dự kiến: ${String.format("%,.0f", calculatedTotal).replace(",", ".")} VNĐ"
                                            } else {
                                                recognitionResultText = "Không phát hiện được món ăn nào."
                                            }
                                            // Log kết quả nhận diện sau NMS và danh sách bill items
                                            Log.d("CameraScreen", "Detection results (after NMS): $results")
                                            Log.d("CameraScreen", "Generated Bill Items: $billItems, Calculated Total: $calculatedTotal")


                                            // --- Bắt đầu QRpay Activity và truyền danh sách billItems và tổng tiền ---
                                            val intent = Intent(context, QRpay::class.java)
                                            intent.putExtra(QRpay.EXTRA_CALCULATED_TOTAL, calculatedTotal) // Truyền tổng tiền

                                            // Truyền danh sách bill items (cần import kotlin.collections.ArrayList)
                                            // Sử dụng ArrayList(billItems) vì putParcelableArrayListExtra cần ArrayList
                                            intent.putParcelableArrayListExtra(QRpay.EXTRA_BILL_ITEMS, ArrayList(billItems))

                                            context.startActivity(intent) // Bắt đầu Activity
                                        } // Kết thúc withContext

                                    } else {
                                        Log.e("CameraScreen", "Failed to decode bitmap from $savedUri")
                                        withContext(Dispatchers.Main) {
                                            recognitionResultText = "Lỗi xử lý ảnh."
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CameraScreen", "Error processing image or running model in background", e)
                                    withContext(Dispatchers.Main) {
                                        recognitionResultText = "Lỗi xử lý: ${e.message}"
                                    }
                                }
                            } // Kết thúc launch Coroutine
                        } // Kết thúc onImageSaved

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraScreen", "Photo capture failed: ${exception.message}", exception)
                            recognitionResultText = "Lỗi chụp ảnh."
                        }
                    } // Kết thúc ImageCapture.OnImageSavedCallback object
                ) // Kết thúc takePicture
            }, // Kết thúc onClick
            modifier = Modifier.fillMaxWidth(),
            // Nút chỉ được bật nếu quyền camera được cấp, ImageCapture sẵn sàng và dữ liệu giá đã load
            enabled = cameraPermissionState.status.isGranted && imageCapture != null && foodPrices.isNotEmpty()
        ) {
            Text("Chụp ảnh và Nhận diện")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hiển thị ảnh và các bounding box
        capturedBitmap?.let { bmp ->
            Box(modifier = Modifier
                .height(300.dp)
                .fillMaxWidth()) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Ảnh đã chụp với các phát hiện",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // Vẽ Bounding Boxes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Tính tỷ lệ scale ảnh hiển thị so với ảnh gốc
                    val imageWidth = bmp.width.toFloat()
                    val imageHeight = bmp.height.toFloat()
                    val scaleFactor = min(size.width / imageWidth, size.height / imageHeight)
                    // Tính offset để căn giữa ảnh
                    val offsetX = (size.width - imageWidth * scaleFactor) / 2
                    val offsetY = (size.height - imageHeight * scaleFactor) / 2


                    detectedObjects.forEach { detectedObject ->
                        // Lấy tọa độ box tuyệt đối từ DetectedObject [xmin, ymin, xmax, ymax]
                        // Giả định boundingBox chứa FloatArray(non-nullable)
                        val xminAbs = detectedObject.boundingBox[0]
                        val yminAbs = detectedObject.boundingBox[1]
                        val xmaxAbs = detectedObject.boundingBox[2]
                        val ymaxAbs = detectedObject.boundingBox[3]

                        // Chuyển đổi tọa độ tuyệt đối sang tọa độ trên Canvas đã scale và căn giữa
                        val xminCanvas = xminAbs * scaleFactor + offsetX
                        val yminCanvas = yminAbs * scaleFactor + offsetY
                        val xmaxCanvas = xmaxAbs * scaleFactor + offsetX
                        val ymaxCanvas = ymaxAbs * scaleFactor + offsetY


                        drawRect(
                            color = androidx.compose.ui.graphics.Color.Red, // Màu đỏ
                            topLeft = Offset(xminCanvas, yminCanvas),
                            size = Size(xmaxCanvas - xminCanvas, ymaxCanvas - yminCanvas),
                            style = Stroke(width = 2.dp.toPx()) // Độ dày nét vẽ
                        )
                        // TODO: Add text drawing for label and score (phức tạp hơn, cần phương pháp vẽ khác)
                    }
                }
            }
        }

        // Hiển thị text kết quả nhận diện và tổng tiền
        if (recognitionResultText.isNotBlank()) {
            Text(
                text = recognitionResultText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Thông báo nếu dữ liệu giá tiền chưa load
        if (foodPrices.isEmpty()) {
            Text(
                text = "Warning: Price data not loaded. Check res/raw/price_table.json",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}