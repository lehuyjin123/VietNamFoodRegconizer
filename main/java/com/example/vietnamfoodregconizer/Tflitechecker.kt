package com.example.vietnamfoodregconizer

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Helper function to check multiple TFLite model files in the app's assets folder.
 * It attempts to load each model and prints information about its input and output tensors.
 *
 * @param context The application context.
 * @param modelFileNames An array or list of TFLite filenames in the assets folder (e.g., ["yolo_region.tflite", "cnn_classifier.tflite"]).
 */
fun checkTFLiteModels(context: Context, vararg modelFileNames: String) {
    Log.d("TFLiteChecker", "--- Checking TFLite Models ---")

    if (modelFileNames.isEmpty()) {
        Log.e("TFLiteChecker", "No model file names provided for checking.")
        Log.d("TFLiteChecker", "--- TFLite Model Check Finished ---")
        return
    }

    for (modelFileName in modelFileNames) {
        Log.d("TFLiteChecker", "--- Checking Model: $modelFileName ---")

        try {
            // 1. Check if the file exists in assets
            val assetManager = context.assets
            val fileList = assetManager.list("")

            if (fileList == null || !fileList.contains(modelFileName)) {
                Log.e("TFLiteChecker", "Error: Model file '$modelFileName' not found in assets.")
                Log.d("TFLiteChecker", "Available files in assets: ${fileList?.joinToString() ?: "None"}")
                // Continue to the next file instead of returning
                continue
            }
            Log.d("TFLiteChecker", "Model file '$modelFileName' found in assets.")

            // 2. Attempt to load the model file into a MappedByteBuffer
            val modelByteBuffer: MappedByteBuffer = try {
                val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelFileName)
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                fileDescriptor.close()
                Log.d("TFLiteChecker", "Model file '$modelFileName' mapped to ByteBuffer successfully.")
                mappedByteBuffer
            } catch (e: Exception) {
                Log.e("TFLiteChecker", "Error mapping model file '$modelFileName' to ByteBuffer", e)
                // Continue to the next file instead of returning
                continue
            }

            // 3. Attempt to create an Interpreter from the ByteBuffer
            val interpreter: Interpreter? = try {
                Interpreter(modelByteBuffer)
            } catch (e: Exception) {
                Log.e("TFLiteChecker", "Error creating TFLite Interpreter for '$modelFileName'", e)
                null
            }

            if (interpreter == null) {
                Log.e("TFLiteChecker", "Failed to initialize TFLite Interpreter for '$modelFileName'.")
                // Continue to the next file instead of returning
                continue
            }
            Log.d("TFLiteChecker", "TFLite Interpreter for '$modelFileName' initialized successfully.")

            // 4. Get and print information about input tensors
            val inputCount = interpreter.inputTensorCount
            Log.d("TFLiteChecker", "  $modelFileName: Number of Input Tensors: $inputCount")
            for (i in 0 until inputCount) {
                val inputTensor = interpreter.getInputTensor(i)
                Log.d("TFLiteChecker", "    $modelFileName:   Input Tensor $i:")
                Log.d("TFLiteChecker", "    $modelFileName:     Name: ${inputTensor.name()}")
                Log.d("TFLiteChecker", "    $modelFileName:     Shape: ${inputTensor.shape().joinToString()}")
                Log.d("TFLiteChecker", "    $modelFileName:     DataType: ${inputTensor.dataType()}")
            }

            // 5. Get and print information about output tensors
            val outputCount = interpreter.outputTensorCount
            Log.d("TFLiteChecker", "  $modelFileName: Number of Output Tensors: $outputCount")
            for (i in 0 until outputCount) {
                val outputTensor = interpreter.getOutputTensor(i)
                Log.d("TFLiteChecker", "    $modelFileName:   Output Tensor $i:")
                Log.d("TFLiteChecker", "    $modelFileName:     Name: ${outputTensor.name()}")
                Log.d("TFLiteChecker", "    $modelFileName:     Shape: ${outputTensor.shape().joinToString()}")
                Log.d("TFLiteChecker", "    $modelFileName:     DataType: ${outputTensor.dataType()}")
            }

            // 6. Close the interpreter when done to release resources
            interpreter.close()
            Log.d("TFLiteChecker", "Interpreter for '$modelFileName' closed.")

        } catch (e: Exception) {
            Log.e("TFLiteChecker", "An unexpected error occurred during TFLite check for '$modelFileName'", e)
        }
    } // End of loop

    Log.d("TFLiteChecker", "--- TFLite Model Check Finished ---")
}

// Example Usage in an Activity or Fragment:
/*
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... your layout setup ...

        // Call the checker function for your two models
        checkTFLiteModels(this, "best_region_detector.tflite", "food_classifier_mobilenetv2.tflite")
    }
}
*/