package com.example.carrotpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun AppTopBar(
    onOpenPdf: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CarrotColors.Background)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Text(
                modifier = Modifier.clickable {
                    isMenuOpen = true
                },
                text = "☰",
                color = CarrotColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium
            )

            DropdownMenu(
                expanded = isMenuOpen,
                onDismissRequest = {
                    isMenuOpen = false
                },
                containerColor = CarrotColors.Surface
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Abrir PDF",
                            color = CarrotColors.TextPrimary
                        )
                    },
                    onClick = {
                        isMenuOpen = false
                        onOpenPdf()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Configurações",
                            color = CarrotColors.TextPrimary
                        )
                    },
                    onClick = {
                        isMenuOpen = false
                        // We will implement settings later.
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = "Carrot PDF",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "⋮",
            color = CarrotColors.TextSecondary,
            style = MaterialTheme.typography.titleLarge
        )
    }
}