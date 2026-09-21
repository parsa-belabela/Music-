package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to "Off",
        5 to "5 Minutes",
        10 to "10 Minutes",
        15 to "15 Minutes",
        30 to "30 Minutes",
        45 to "45 Minutes",
        60 to "1 Hour"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(options) { (mins, label) ->
                    TextButton(
                        onClick = {
                            onSelectMinutes(mins)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label)
                            if (currentMinutes == mins) {
                                Text("Active", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
