package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit,
    lang: AppLanguage = AppLanguage.PERSIAN
) {
    val options = listOf(
        0 to if (lang == AppLanguage.PERSIAN) "خاموش" else "Off",
        5 to if (lang == AppLanguage.PERSIAN) "۵ دقیقه" else "5 Minutes",
        10 to if (lang == AppLanguage.PERSIAN) "۱۰ دقیقه" else "10 Minutes",
        15 to if (lang == AppLanguage.PERSIAN) "۱۵ دقیقه" else "15 Minutes",
        30 to if (lang == AppLanguage.PERSIAN) "۳۰ دقیقه" else "30 Minutes",
        45 to if (lang == AppLanguage.PERSIAN) "۴۵ دقیقه" else "45 Minutes",
        60 to if (lang == AppLanguage.PERSIAN) "۱ ساعت" else "1 Hour"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == AppLanguage.PERSIAN) "تایمر خواب" else "Sleep Timer") },
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
                                Text(
                                    if (lang == AppLanguage.PERSIAN) "فعال" else "Active",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == AppLanguage.PERSIAN) "بستن" else "Close")
            }
        }
    )
}
