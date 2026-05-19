package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun AppTopBar(
    onOpenPdf: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CarrotColors.Background)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "☰",
            color = CarrotColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = "Carrot PDF",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier = Modifier.clickable { onOpenPdf() },
            text = "+",
            color = CarrotColors.Accent,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}