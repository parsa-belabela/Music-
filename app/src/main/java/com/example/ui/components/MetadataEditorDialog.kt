package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.Track

@Composable
fun MetadataEditorDialog(
    track: Track,
    lang: AppLanguage = AppLanguage.PERSIAN,
    onSave: (title: String, artist: String, album: String, genre: String, year: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }
    var genre by remember { mutableStateOf(track.genre) }
    var yearText by remember { mutableStateOf(track.year.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lang == AppLanguage.PERSIAN) "ویرایش اطلاعات آهنگ" else "Edit Song Metadata") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (lang == AppLanguage.PERSIAN) "نام قطعه" else "Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(if (lang == AppLanguage.PERSIAN) "هنرمند" else "Artist") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(if (lang == AppLanguage.PERSIAN) "آلبوم" else "Album") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(if (lang == AppLanguage.PERSIAN) "سبک موسیقی" else "Genre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it },
                    label = { Text(if (lang == AppLanguage.PERSIAN) "سال انتشار" else "Year") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val yr = yearText.toIntOrNull() ?: track.year
                    onSave(title, artist, album, genre, yr)
                    onDismiss()
                }
            ) {
                Text(if (lang == AppLanguage.PERSIAN) "ذخیره تغییرات" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel")
            }
        }
    )
}
