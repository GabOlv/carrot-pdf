package com.example.carrotpdf.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun EmptyState(
    onOpenPdf: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Open a PDF to start reading",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))

        Button(
            onClick = onOpenPdf,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CarrotColors.Accent,
                contentColor = CarrotColors.Background
            )
        ) {
            Text("Open PDF")
        }
    }
}