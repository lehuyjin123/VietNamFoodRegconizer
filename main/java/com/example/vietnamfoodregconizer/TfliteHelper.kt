// File: TFLiteHelper.kt
package com.example.vietnamfoodregconizer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

// Import DetectedObject từ file riêng của bạn
// import com.example.vietnamfoodregconizer.DetectedObject // Đảm bảo đường dẫn package đúng

class TFLiteHelper(private val context: Context) {
    private var interpreter: Interpreter? = null
    // TODO: Đảm bảo tên model và file nhãn chính xác và nằm trong thư mục assets
    // Tên model dựa trên thông tin bạn cung cấp
    private val MODEL_NAME = "mo_hinh_cua_ban.tflite"
    private val LABELS_FILE = "labels.txt" // Tên file nhãn trong thư mục assets (98 nhãn)

    // TODO: Xác nhận Kích thước đầu vào của model bạn (Dùng Netron)
    private val INPUT_SIZE = 640 // Kích thước đầu vào của model bạn (width và height)

    private var labels: List<String> = listOf() // Sẽ được tải từ file labels.txt

    // TODO: Xác nhận chính xác số lượng lớp của model bạn (Dùng Netron và khớp với labels.txt)
    // Dựa trên file labels.txt bạn cung cấp (98 nhãn)
    private val NUM_CLASSES = 98

    // TODO: Xác nhận chính xác cấu trúc output của model YOLO TFLite (Dùng Netron)
    // Shape thường là [1, num_features_per_box, num_boxes] HOẶC [1, num_boxes, num_features_per_box].
    // Dựa trên lỗi buffer size 3696000 byte, shape output là [1, 110, 8400] hoặc [1, 8400, 110]
    private val NUM_BOXES = 8400 // Số lượng box tiềm năng
    private val NUM_FEATURES_PER_BOX = 110 // Số lượng giá trị cho mỗi box (4 box + 1 obj + 105/106 classes)

    private val CLASS_SCORES_COUNT = NUM_CLASSES // Số lượng điểm lớp (phải khớp với NUM_CLASSES, nhưng model output 105/106???)
    // Nếu model output 106 lớp, CLASS_SCORES_COUNT = 106
    // Nếu model train 98 lớp nhưng output 106 score, cần hiểu ý nghĩa 8 score dư ra.
    // Giả định model output 106 score và 98 đầu tiên là của bạn.
    // Cần kiểm tra Netron! Dựa trên lỗi, output là 110 features.
    // Nếu NUM_CLASSES = 98, thì 110 features = 4 box + 1 obj + 105 scores? hoặc 4 box + 1 obj + 98 scores + 7 padding?
    // Giả định 4 box + 1 obj + 105 class scores. Và 98 lớp của bạn là 98 score đầu tiên.
    // CLASS_SCORES_COUNT = 105 // Số lượng scores lớp thực tế trong output tensor

    // Ngưỡng tin cậy để chấp nhận một phát hiện thô (trên điểm objectness hoặc điểm lớp cao nhất)
    // Thường dùng objectness threshold sau đó lọc theo class score threshold
    private val OBJECTNESS_THRESHOLD = 0.9f // Ngưỡng objectness score (thử nghiệm)

    private val CONFIDENCE_THRESHOLD = 0.98f // Ngưỡng tin cậy cuối cùng để chấp nhận một phát hiện (trên điểm lớp cao nhất sau objectness filter)

    private val IOU_THRESHOLD = 0.99f // Ngưỡng IOU cho Non-Maximum Suppression (NMS)
    private val NMS_SCORE_THRESHOLD = 0.99f // Ngưỡng tin cậy phụ trợ cho NMS (lọc box yếu trước NMS logic)


    fun initialize() {
        Log.d("TFLiteHelper", "Initializing TFLiteHelper (Single Model)...")
        try {
            Log.d("TFLiteHelper", "Attempting to load model file: $MODEL_NAME")
            val modelFile = loadModelFile(MODEL_NAME)
            Log.d("TFLiteHelper", "Model file loaded successfully.")

            // Cấu hình Interpreter (có thể cần thêm options nếu dùng delegate GPU/NNAPI)
            val interpreterOptions = Interpreter.Options()
            // Ví dụ thêm NNAPI delegate (nếu bạn muốn dùng NNAPI):
            // val nnApiDelegate = NnApiDelegate()
            // interpreterOptions.addDelegate(nnApiDelegate)
            // Nếu bạn muốn dùng GPU delegate:
            // try {
            //     val gpuDelegate = GpuDelegate()
            //     interpreterOptions.addDelegate(gpuDelegate)
            // } catch (e: Exception) {
            //     Log.e("TFLiteHelper", "Failed to add GPU delegate.", e)
            // }


            Log.d("TFLiteHelper", "Attempting to create Interpreter.")
            interpreter = Interpreter(modelFile, interpreterOptions)
            Log.d("TFLiteHelper", "Interpreter created successfully.")

            // Tải nhãn (98 nhãn)
            try {
                labels = FileUtil.loadLabels(context, LABELS_FILE)
                Log.d("TFLiteHelper", "Labels loaded successfully: ${labels.size} labels.")
                // Kiểm tra số lượng nhãn khớp với NUM_CLASSES
                if (labels.size != NUM_CLASSES) {
                    Log.e("TFLiteHelper", "Labels file size mismatch! Expected $NUM_CLASSES, found ${labels.size}. Check $LABELS_FILE content.")
                    // Xử lý lỗi hoặc cảnh báo nếu số lượng nhãn không khớp số lớp bạn mong đợi
                    // Nếu model output 106 scores, nhưng bạn chỉ có 98 nhãn, bạn chỉ có thể hiển thị 98 nhãn đầu tiên.
                }
            } catch (e: Exception) {
                Log.e("TFLiteHelper", "Failed to load labels from $LABELS_FILE", e)
                labels = emptyList() // Đặt nhãn rỗng nếu không load được
            }

            // TODO: Log input/output shape của model sau khi interpreter được tạo để kiểm tra
            // val inputShape = interpreter!!.getInputTensor(0).shape()
            // val outputShape = interpreter!!.getOutputTensor(0).shape()
            // Log.d("TFLiteHelper", "Model Input Shape: ${inputShape.joinToString()}")
            // Log.d("TFLiteHelper", "Model Output Shape: ${outputShape.joinToString()}")


        } catch (e: Exception) {
            Log.e("TFLiteHelper", "Model initialization failed with exception.", e)
            interpreter = null
            e.printStackTrace() // In stack trace đầy đủ hơn
        }
        Log.d("TFLiteHelper", "TFLiteHelper initialization finished.")
    }

    // Helper function để load file model từ assets
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        Log.d("TFLiteHelper", "Inside loadModelFile() for $modelName")
        context.assets.openFd(modelName).use { fileDescriptor ->
            Log.d("TFLiteHelper", "File descriptor obtained for $modelName.")
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                Log.d("TFLiteHelper", "Input stream created.")
                val fileChannel = inputStream.channel
                Log.d("TFLiteHelper", "File channel obtained.")
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                Log.d("TFLiteHelper", "MappedByteBuffer created.")
                return mappedByteBuffer
            }
        }
        // Không cần catch Exception ở đây vì nó sẽ được catch ở initialize()
    }

    fun analyzeBitmap(bitmap: Bitmap): List<DetectedObject> {
        if (interpreter == null) {
            Log.e("TFLiteHelper", "analyzeBitmap: Interpreter is not initialized.")
            return emptyList()
        }

        if (false) { // Kiểm tra null đã có
            Log.e("TFLiteHelper", "analyzeBitmap: Input bitmap is null.")
            return emptyList()
        }

        // 1. Tiền xử lý ảnh đầu vào
        val resizedBitmap = try {
            // Sử dụng createScaledBitmap để resize
            bitmap.scale(INPUT_SIZE, INPUT_SIZE)
        } catch (e: Exception) {
            Log.e("TFLiteHelper", "analyzeBitmap: Error scaling bitmap", e)
            e.printStackTrace()
            return emptyList()
        }
        // Chuyển đổi Bitmap đã resize sang ByteBuffer
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // 2. Chuẩn bị buffer cho đầu ra của mô hình
        // Model YOLOv8 TFLite có MỘT output tensor với shape [1, 110, 8400] (hoặc [1, 8400, 110])
        // Shape: [batch_size, num_features_per_box, num_boxes]
        // Tạo buffer để nhận toàn bộ dữ liệu output (FLOAT32 = 4 bytes)
        // Kích thước buffer PHẢI KHỚP CHÍNH XÁC kích thước output tensor của model
        val outputBuffer = ByteBuffer.allocateDirect(1 * NUM_FEATURES_PER_BOX * NUM_BOXES * 4) // FLOAT32 = 4 bytes
        outputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.rewind()

        // Sử dụng Map<Int, Any> để chứa output tensor duy nhất (index 0)
        val outputMap = HashMap<Int, Any>()
        outputMap[0] = outputBuffer // Map buffer này tới output tensor index 0 ("Identity")

        // 3. Chạy mô hình
        try {
            val inputs = arrayOf<Any>(byteBuffer)
            interpreter!!.runForMultipleInputsOutputs(inputs, outputMap)
            Log.d("TFLiteHelper", "Interpreter ran successfully.")

        } catch (e: Exception) {
            Log.e("TFLiteHelper", "analyzeBitmap: Error running interpreter", e)
            e.printStackTrace() // In stack trace đầy đủ hơn
            // Log input/output shape của interpreter ngay trước khi chạy có thể giúp gỡ lỗi shape mismatch
            // try {
            //     Log.e("TFLiteHelper", "Input Tensor Shape (Interpreter): ${interpreter!!.getInputTensor(0).shape().joinToString()}")
            //     Log.e("TFLiteHelper", "Output Tensor Shape (Interpreter): ${interpreter!!.getOutputTensor(0).shape().joinToString()}")
            // } catch (logE: Exception) { Log.e("TFLiteHelper", "Error getting tensor shapes for logging", logE) }

            return emptyList()
        }

        // === 4. Hậu xử lý kết quả từ buffer output DUY NHẤT ===
        val rawDetections = mutableListOf<DetectedObject>()
        outputBuffer.rewind() // Reset buffer về đầu để đọc dữ liệu

        // Giả định cấu trúc 110 features: [4 (box: cx, cy, w, h) + 1 (objectness) + 105 (class scores)]
        // Cần xác nhận chính xác bằng Netron hoặc tài liệu!
        val boxCoordsSize = 4 // cx, cy, w, h
        val classScoresSizeInOutput = 105 // Số lượng điểm lớp thực tế trong 110 features (110 - 4 - 1 = 105?)

        val boxCoordsStartIndex = 0
        val objectnessScoreIndex = 4
        val classScoresStartIndex = 5


        for (i in 0 until NUM_BOXES) { // Lặp qua 8400 bounding box tiềm năng
            // Đọc NUM_FEATURES_PER_BOX (110) giá trị float cho mỗi box
            val boxData = FloatArray(NUM_FEATURES_PER_BOX)

            // Đọc từng float một từ buffer
            for(k in 0 until NUM_FEATURES_PER_BOX) {
                boxData[k] = outputBuffer.getFloat()
            }

            // Trích xuất thông tin từ boxData dựa trên cấu trúc giả định [box, obj, classes]
            // TODO: CẦN KIỂM TRA LẠI INDEX CHÍNH XÁC BẰNG NETRON CHO MODEL CỦA BẠN!
            val boxCoordsRaw = boxData.sliceArray(boxCoordsStartIndex until boxCoordsStartIndex + boxCoordsSize) // 4 giá trị box [cx, cy, w, h] chuẩn hóa
            val objectnessScore = boxData[objectnessScoreIndex] // Objectness score

            // Áp dụng ngưỡng objectness score trước khi xem xét class scores
            if (objectnessScore >= OBJECTNESS_THRESHOLD) {

                // Trích xuất điểm tin cậy cho từng lớp
                val classScores = boxData.sliceArray(classScoresStartIndex until classScoresStartIndex + classScoresSizeInOutput) // Lấy 105 điểm lớp

                // Tìm lớp có điểm cao nhất trong 105 scores và điểm đó
                var maxScore = 0f // Đây sẽ là điểm tin cậy cuối cùng cho box
                var detectedClassIdInOutput = -1 // ID lớp trong output tensor (0-104?)
                for (j in 0 until classScoresSizeInOutput) { // Lặp qua 105 scores
                    if (classScores[j] > maxScore) {
                        maxScore = classScores[j]
                        detectedClassIdInOutput = j // ID lớp trong output tensor (0-104?)
                    }
                }

                // Áp dụng ngưỡng tin cậy cuối cùng trên điểm lớp cao nhất (sau khi đã lọc objectness)
                if (maxScore >= CONFIDENCE_THRESHOLD) {

                    // TODO: Ánh xạ ID lớp trong Output Tensor (0-104?) sang ID lớp trong file labels.txt (0-97)
                    // Điều này phụ thuộc vào cách model được train và export.
                    // Giả định 98 lớp của bạn là 98 lớp đầu tiên trong 105/106 lớp output.
                    // Cần kiểm tra Netron và logic train/export model của bạn!
                    val finalDetectedClassId = detectedClassIdInOutput // Giả định ID trong output tensor khớp với ID trong labels (nếu output >= labels.size)

                    // Kiểm tra ID lớp có hợp lệ với danh sách nhãn (98 nhãn) không
                    if (finalDetectedClassId >= 0 && finalDetectedClassId < labels.size) {

                        // Lấy nhãn từ Class ID (0-97)
                        val label = getLabelFromClassId(finalDetectedClassId)

                        // Chuyển đổi tọa độ box từ [cx, cy, w, h] chuẩn hóa sang [xmin, ymin, xmax, ymax] tuyệt đối
                        val cx_norm = boxCoordsRaw[0]
                        val cy_norm = boxCoordsRaw[1]
                        val w_norm = boxCoordsRaw[2]
                        val h_norm = boxCoordsRaw[3]

                        val xmin = (cx_norm - w_norm / 2) * bitmap.width
                        val ymin = (cy_norm - h_norm / 2) * bitmap.height
                        val xmax = (cx_norm + w_norm / 2) * bitmap.width
                        val ymax = (cy_norm + h_norm / 2) * bitmap.height

                        // Đảm bảo tọa độ nằm trong giới hạn ảnh
                        val finalXmin = max(0f, min(xmin, xmax))
                        val finalYmin = max(0f, min(ymin, ymax))
                        val finalXmax = max(0f, min(xmin, xmax))
                        val finalYmax = max(0f, min(ymin, ymax))


                        val absoluteBox = floatArrayOf(finalXmin, finalYmin, finalXmax, finalYmax)

                        // Thêm vào danh sách raw detections
                        rawDetections.add(DetectedObject(label, maxScore, absoluteBox)) // Lưu điểm lớp cao nhất và box tuyệt đối
                    } else {
                        Log.w("TFLiteHelper", "analyzeBitmap: Detected class ID $finalDetectedClassId is out of bounds for labels list size ${labels.size}. Skipping.")
                    }
                }
            }
        }

        Log.d("TFLiteHelper", "Found ${rawDetections.size} objects above objectness and confidence thresholds before NMS.")

        // === ÁP DỤNG NON-MAX SUPPRESSION (NMS) ===
        // Sử dụng IOU_THRESHOLD và NMS_SCORE_THRESHOLD (lọc box yếu trước NMS logic)
        val finalDetections = applyNMS(rawDetections, IOU_THRESHOLD, NMS_SCORE_THRESHOLD) // Gọi applyNMS

        Log.d("TFLiteHelper", "Found ${finalDetections.size} objects after NMS.")

        return finalDetections // TRẢ VỀ danh sách sau NMS
    }

    // Hàm này thực hiện Non-Maximum Suppression
    // scoreThreshold ở đây dùng để lọc các box có điểm thấp trước khi áp dụng logic IOU
    private fun applyNMS(
        detections: List<DetectedObject>,
        iouThreshold: Float,
        scoreThreshold: Float // Tham số này sẽ được sử dụng để lọc ban đầu
    ): List<DetectedObject> {
        // 1. Lọc các detections có điểm tin cậy (score) >= scoreThreshold
        val filteredDetections = detections.filter { it.score >= scoreThreshold }

        if (filteredDetections.isEmpty()) return emptyList()

        val sortedDetections = filteredDetections.sortedByDescending { it.score }

        val selectedDetections = mutableListOf<DetectedObject>()
        val suppressed = BooleanArray(sortedDetections.size) { false }

        for (i in sortedDetections.indices) {
            if (suppressed[i]) continue

            val detection = sortedDetections[i]

            selectedDetections.add(detection)

            for (j in i + 1 until sortedDetections.size) {
                if (suppressed[j]) continue

                val otherDetection = sortedDetections[j]

                if (detection.label == otherDetection.label) {
                    val iou = calculateIOU_Corrected(detection.boundingBox, otherDetection.boundingBox)

                    // --- THÊM CÁC DÒNG LOG SAU VÀO ĐÂY ---
                    Log.d("NMS_DEBUG", "Comparing: ${detection.label} (score: ${detection.score}) box ${detection.boundingBox.joinToString()} with ${otherDetection.label} (score: ${otherDetection.score}) box ${otherDetection.boundingBox.joinToString()}")
                    Log.d("NMS_DEBUG", "Calculated IOU: $iou vs Threshold: $iouThreshold. Suppression condition (iou > threshold): ${iou > iouThreshold}")
                    Log.d("NMS_DEBUG", "Suppressed status of otherDetection[j]: ${suppressed[j]}")
                    // --- KẾT THÚC THÊM LOG ---


                    if (iou > iouThreshold) {
                        suppressed[j] = true
                        Log.d("NMS_DEBUG", "Suppressed box with score ${otherDetection.score} due to high IOU.")
                    }
                }
            }
        }

        return selectedDetections
    }

    // Hàm tính Intersection over Union (IOU) giữa hai bounding box
    // box là [xmin, ymin, xmax, ymax]
    private fun calculateIOU_Corrected(box1: FloatArray, box2: FloatArray): Float {
        val xmin1 = box1[0]
        val ymin1 = box1[1]
        val xmax1 = box1[2]
        val ymax1 = box1[3]

        val xmin2 = box2[0]
        val ymin2 = box2[1]
        val xmax2 = box2[2]
        val ymax2 = box2[3]

        // Tính tọa độ của phần giao nhau (intersection)
        val intersectXmin = max(xmin1, xmin2)
        val intersectYmin = max(ymin1, ymin2)
        val intersectXmax = min(xmax1, xmax2)
        val intersectYmax = min(ymax1, ymax2) // Sửa lỗi chính tả Ymin2 -> intersectYmax

        // Tính diện tích phần giao nhau
        val intersectWidth = max(0f, intersectXmax - intersectXmin)
        val intersectHeight = max(0f, intersectYmax - intersectYmin) // Sử dụng intersectYmax

        val intersectArea = intersectWidth * intersectHeight

        // Tính diện tích của hai box
        val area1 = (xmax1 - xmin1) * (ymax1 - ymin1)
        val area2 = (xmax2 - xmin2) * (ymax2 - ymin2)

        // Tính diện tích phần hợp (union)
        val unionArea = area1 + area2 - intersectArea

        // Tránh chia cho 0 hoặc âm
        return if (unionArea <= 0f) 0f else intersectArea / unionArea // Dùng <= 0f để bao gồm cả trường hợp unionArea = 0
    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // Model YOLOv8 TFLite thường yêu cầu đầu vào FLOAT32, chuẩn hóa [0,1]
        val byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4) // Batch_size * H * W * Channels * BytesPerFloat
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]
                // Chuẩn hóa pixel về khoảng [0,1] cho các kênh màu RGB
                // Lưu ý: Thứ tự màu trong ByteBuffer thường là RGB, cần kiểm tra lại model
                byteBuffer.putFloat((((`val` shr 16) and 0xFF) / 255.0f)) // Red
                byteBuffer.putFloat((((`val` shr 8) and 0xFF) / 255.0f))  // Green
                byteBuffer.putFloat(((`val` and 0xFF) / 255.0f))          // Blue
            }
        }
        return byteBuffer
    }

    // Hàm lấy nhãn từ Class ID
    private fun getLabelFromClassId(classId: Int): String {
        // Sử dụng danh sách nhãn đã tải (98 nhãn)
        return if (labels.isNotEmpty() && classId >= 0 && classId < labels.size) {
            labels[classId]
        } else {
            "Unknown Class ($classId)"
        }
    }

}