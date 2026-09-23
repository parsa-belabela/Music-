package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.Track

@Composable
fun MetadataEditorDialog(
    track: Track,
    lang: AppLanguage = AppLanguage.PERSIAN,
    onSave: (title: String, artist: String, album: String, genre: String, year: Int, artworkUri: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(track.title) }
    var artist by remember { mutableStateOf(track.artist) }
    var album by remember { mutableStateOf(track.album) }
    var genre by remember { mutableStateOf(track.genre) }
    var yearText by remember { mutableStateOf(track.year.toString()) }
    var selectedArtworkUri by remember { mutableStateOf(track.artworkUri) }

    // Android Zero-Permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Take persistable URI permission if needed
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            selectedArtworkUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (lang == AppLanguage.PERSIAN) "ویرایش اطلاعات و کاور آهنگ" else "Edit Song Metadata & Artwork",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Artwork selection preview
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2232))
                        .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedArtworkUri != null) {
                        AsyncImage(
                            model = selectedArtworkUri,
                            contentDescription = "Song Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Cover",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "انتخاب کاور" else "Select Cover",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (selectedArtworkUri != null) {
                    TextButton(
                        onClick = { selectedArtworkUri = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (lang == AppLanguage.PERSIAN) "حذف کاور" else "Remove Cover", fontSize = 12.sp)
                    }
                }

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
                    onSave(title, artist, album, genre, yr, selectedArtworkUri)
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
