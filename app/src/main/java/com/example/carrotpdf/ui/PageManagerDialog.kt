package com.example.carrotpdf.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PageManagerDialog(
    pageCount: Int,
    onRemovePages: (Set<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPages by remember(pageCount) {
        mutableStateOf(emptySet<Int>())
    }
    val canRemove = selectedPages.isNotEmpty() && selectedPages.size < pageCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerenciar páginas") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items((0 until pageCount).toList(), key = { index -> index }) { pageIndex ->
                    val selected = pageIndex in selectedPages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPages = if (selected) {
                                    selectedPages - pageIndex
                                } else {
                                    selectedPages + pageIndex
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                selectedPages = if (checked) {
                                    selectedPages + pageIndex
                                } else {
                                    selectedPages - pageIndex
                                }
                            }
                        )
                        Text("Página ${pageIndex + 1}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRemovePages(selectedPages) },
                enabled = canRemove
            ) {
                Text("Remover ${selectedPages.size.coerceAtLeast(1)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
