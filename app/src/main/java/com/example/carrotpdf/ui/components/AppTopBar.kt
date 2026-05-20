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
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(CarrotColors.Background)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.clickable { onMenuClick() },
            text = "☰",
            color = CarrotColors.TextSecondary,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = "Carrot PDF",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )
    }
}