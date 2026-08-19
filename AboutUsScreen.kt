package com.example.vietnamfoodregconizer

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

data class Creator(val name: String, val imageResId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(navController: NavController) {
    val context = LocalContext.current

    fun getAppVersion(context: Context): String {
        return try {
            // Use the modern getPackageInfo for API 33+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
                    ?: "N/A" // Use Elvis operator to provide a default if versionName is null
            } else {
                // Use the older method for API levels below 33
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    ?: "N/A" // Use Elvis operator to provide a default if versionName is null
            }
        } catch (e: Exception) {
            "N/A" // Return "N/A" in case of exception
        }
    }

    // Define your list of creators
    val creators = listOf(
        Creator("Lê Huy - App developer", R.drawable.creator_lehuy),
        Creator("Lý Thanh Tâm - AI modeling", R.drawable.creator_lythanhtam),
        Creator("Nguyễn Minh Quang - Data processor", R.drawable.creator_minhquang),
        Creator("Trần Hải Đông - QR system", R.drawable.creator_tranhaidong)
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giới thiệu") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // App Title and Description
            Text(
                text = "Vietnamese Food Recognizer",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ứng dụng này giúp bạn nhận diện các món ăn truyền thống của Việt Nam bằng cách chụp ảnh. Nó sử dụng công nghệ nhận diện hình ảnh để cung cấp thông tin về món ăn.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Version Information
            Text(
                text = "Phiên bản",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = getAppVersion(context),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Creators Section
            Text(
    text = "Nhà phát triển",
    style = MaterialTheme.typography.headlineSmall
)
Spacer(modifier = Modifier.height(8.dp)) // Add space below the section title

// Grid layout for creators (2x2)
Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally // Center the grid within the main Column
) {
    // First Row (top two creators)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly // Distribute space evenly between the two items
    ) {
        // Display the first creator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = creators[0].imageResId),
                contentDescription = creators[0].name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = creators[0].name,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Display the second creator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = creators[1].imageResId),
                contentDescription = creators[1].name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = creators[1].name,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp)) // Space between the two rows

    // Second Row (bottom two creators)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly // Distribute space evenly between the two items
    ) {
        // Display the third creator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = creators[2].imageResId),
                contentDescription = creators[2].name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = creators[2].name,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Display the fourth creator
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = creators[3].imageResId),
                contentDescription = creators[3].name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = creators[3].name,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


            Spacer(modifier = Modifier.height(24.dp)) // Add space after the Creators section

            // Contact Information/Links (Example)
            Text(
                text = "Liên hệ",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Email: jinmap123@gmail.com", // Replace with your email
                style = MaterialTheme.typography.bodyMedium
            )
            // You could add clickable links here using `ClickableText` or a Button

            Spacer(modifier = Modifier.height(24.dp)) // Add some space

            // Acknowledgements (Optional)
            Text(
                text = "Lời cảm ơn",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sử dụng ChatGPT,Gemini, thư viện TensorFlow Lite, CameraX, Jetpack Compose, Coil, và các tài nguyên khác.",
                style = MaterialTheme.typography.bodyMedium
            )

            // Add more sections as needed (e.g., Privacy Policy, Terms of Service)
        }
    }
}

// Add a Preview for the AboutUsScreen (optional but recommended)
@Preview(showBackground = true)
@Composable
fun PreviewAboutUsScreen() {
    // For preview, create a dummy NavController
    val navController = rememberNavController()
    AboutUsScreen(navController = navController)
}