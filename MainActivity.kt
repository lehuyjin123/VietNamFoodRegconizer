package com.example.vietnamfoodregconizer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vietnamfoodregconizer.ui.theme.VietNamFoodRegconizerTheme

object AppRoutes {
    const val HOME = "home"
    const val ABOUT_US = "aboutus"
    const val CAMERA_SCREEN = "camera_screen_rtoute"
    const val PRICE_TABLE = "price_table_route"
    const val FOOD_HISTORY = "food_history_route"
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            VietNamFoodRegconizerTheme {
                val navController = rememberNavController()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Vietnamese Food Recognizer") }
                        )
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.HOME,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable(AppRoutes.HOME) {
                            VietnameseFoodRecognizerHome(
                                onPriceTableClick = {
                                    Log.d("Navigation", "Price Table button clicked")
                                    navController.navigate(AppRoutes.PRICE_TABLE)
                                },
                                onRecognizeFoodClick = {Log.d("Navigation", "Recognize Food button clicked")
                                    navController.navigate(AppRoutes.CAMERA_SCREEN)
                                },
                                onFoodHistoryClick = {
                                    Log.d("Navigation", "Food History button clicked")
                                    navController.navigate(AppRoutes.FOOD_HISTORY)
                                },
                                onAboutUsClick = {
                                    Log.d("Navigation", "About Us button clicked")
                                    navController.navigate(AppRoutes.ABOUT_US)
                                }
                            )
                        }
                        composable(AppRoutes.ABOUT_US) {
                            AboutUsScreen(navController = navController)
                        }
                        composable(AppRoutes.CAMERA_SCREEN) {
                            CameraScreen(navController = navController)
                        }
                        composable(AppRoutes.PRICE_TABLE) {
                            PriceTableScreen(navController = navController)
                        }
                        composable(AppRoutes.FOOD_HISTORY) {
                            Text("Coming soon...")
                        }
                    }
                }
            }
            checkTFLiteModels(this, "mo_hinh_cua_ban.tflite", "mo_hinh_cua_ban.tflite")
        }
    }
}

@Composable
fun VietnameseFoodRecognizerHome(
    onPriceTableClick: () -> Unit,
    onRecognizeFoodClick: () -> Unit,
    onFoodHistoryClick: () -> Unit,
    onAboutUsClick: () -> Unit
) {
    val UEHBlue = Color(0xFF003399)
    val UEHOrange = Color(0xFFFFA500)
    val White = Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.homebackground),
                contentDescription = "Food background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Created by 3ITech",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onAboutUsClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = UEHBlue),
                        elevation = ButtonDefaults.buttonElevation(6.dp),
                        modifier = Modifier.size(90.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = "About Us", tint = White)
                            Text("About\nUs", textAlign = TextAlign.Center, color = White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Bottom Button Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Triple("Price\nTable", Pair(onPriceTableClick, Icons.Default.AttachMoney), UEHBlue),
                    Triple("Recognize\nFood", Pair(onRecognizeFoodClick, Icons.Default.CameraAlt), UEHOrange),
                    Triple("Food\nHistory", Pair(onFoodHistoryClick, Icons.Default.History), UEHBlue)
                ).forEach { (label, actionIcon, color) ->
                    val (action, icon) = actionIcon
                    Button(
                        onClick = action,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        elevation = ButtonDefaults.buttonElevation(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = label, tint = White)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = White,fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVietnameseFoodRecognizerHome() {
    VietNamFoodRegconizerTheme {
        VietnameseFoodRecognizerHome(
            onPriceTableClick = {},
            onRecognizeFoodClick = {},
            onFoodHistoryClick = {},
            onAboutUsClick = {}
        )
    }
}
