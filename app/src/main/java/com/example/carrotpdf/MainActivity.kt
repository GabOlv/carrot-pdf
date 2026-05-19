package com.example.carrotpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.carrotpdf.ui.CarrotPdfApp
import com.example.carrotpdf.ui.theme.CarrotpdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CarrotpdfTheme {
                CarrotPdfApp()
            }
        }
    }
}