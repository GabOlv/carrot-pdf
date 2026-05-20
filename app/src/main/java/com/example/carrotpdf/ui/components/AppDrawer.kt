package com.example.carrotpdf.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.carrotpdf.R
import com.example.carrotpdf.ui.design.CarrotColors

@Composable
fun AppDrawer(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenPdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(270.dp)
            .fillMaxHeight()
            .background(CarrotColors.Surface)
            .padding(horizontal = 18.dp, vertical = 22.dp)
    ) {
        Text(
            text = "Carrot PDF",
            color = CarrotColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = "Leitor de estudos",
            color = CarrotColors.TextMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(26.dp))

        DrawerItem(
            icon = "▤",
            label = "Abrir PDF",
            isActive = true,
            onClick = onOpenPdf
        )

        DrawerItem(
            icon = "◷",
            label = "Arquivos recentes",
            onClick = {}
        )

        DrawerItem(
            icon = "♡",
            label = "Favoritos",
            onClick = {}
        )

        Divider(
            modifier = Modifier.padding(vertical = 18.dp),
            color = CarrotColors.SurfaceSoft
        )

        ThemeModeCard(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme
        )

        Spacer(modifier = Modifier.height(18.dp))

        DrawerItem(
            icon = "⚙",
            label = "Configurações",
            onClick = {}
        )

        DrawerItem(
            icon = "?",
            label = "Ajuda e feedback",
            onClick = {}
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Local only",
            color = CarrotColors.TextMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DrawerItem(
    icon: String,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                color = if (isActive) CarrotColors.AccentSoft else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            color = if (isActive) CarrotColors.Accent else CarrotColors.TextSecondary,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            color = if (isActive) CarrotColors.Accent else CarrotColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ThemeModeCard(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CarrotColors.Background,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onToggleTheme() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    id = if (isDarkTheme) R.drawable.ic_theme_dark else R.drawable.ic_theme_light
                ),
                contentDescription = if (isDarkTheme) "Dark theme" else "Light theme",
                modifier = Modifier.width(42.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = if (isDarkTheme) "Modo escuro" else "Modo claro",
                    color = CarrotColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Tema do app",
                    color = CarrotColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Switch(
            checked = isDarkTheme,
            onCheckedChange = {
                onToggleTheme()
            }
        )
    }
}