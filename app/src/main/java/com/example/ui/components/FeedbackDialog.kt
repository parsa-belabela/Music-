package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FeedbackCategory(val labelKey: String) {
    BUG("type_bug"),
    FEATURE("type_feature"),
    GENERAL("type_general")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    appSettings: AppSettings,
    palette: AmbientPalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(FeedbackCategory.BUG) }
    var messageText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    fun buildEmailBody(): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return """
Category: ${Localization.getString(selectedCategory.labelKey, appSettings.language)}
Timestamp: $dateStr

--- User Message ---
$messageText

--- System & App Diagnostics ---
App: Aura Music Player v1.0.0
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android Version: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
App Language: ${appSettings.language.name}
Theme: ${appSettings.theme.name}
Visualizer FPS: ${appSettings.visualizerFps}Hz
Audio Gapless: ${appSettings.gaplessEnabled}
        """.trimIndent()
    }

    fun submitFeedback() {
        if (messageText.isBlank()) {
            hasError = true
            return
        }

        val subject = "[Aura Feedback] ${Localization.getString(selectedCategory.labelKey, AppLanguage.ENGLISH)} - ${Build.MODEL}"
        val body = buildEmailBody()
        val emailUri = Uri.parse("mailto:parsaghorbani0000@gmail.com?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        val emailIntent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send Feedback"))
            Toast.makeText(context, Localization.getString("feedback_success", appSettings.language), Toast.LENGTH_LONG).show()
            onDismiss()
        } catch (e: Exception) {
            // Graceful fallback to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Aura Feedback", body)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, Localization.getString("feedback_copied", appSettings.language), Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.22f,
                    borderWidth = 1.2.dp
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.getString("feedback_dialog_title", appSettings.language),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA0A0B8))
                    }
                }

                // Category Selector
                Text(
                    text = Localization.getString("feedback_type", appSettings.language),
                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFA0A0C0))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeedbackCategory.values().forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) palette.primary.copy(alpha = 0.35f) else Color(0xFF1E1C33))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.getString(cat.labelKey, appSettings.language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFFB0B0C8),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Message Text Field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = {
                        messageText = it
                        if (it.isNotBlank()) hasError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("feedback_message_input"),
                    placeholder = {
                        Text(
                            text = Localization.getString("feedback_message_hint", appSettings.language),
                            color = Color(0xFF70708C),
                            fontSize = 13.sp
                        )
                    },
                    isError = hasError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = Color(0xFF2A2845),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = palette.accent
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                // Direct email destination badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.getString("feedback_sending_to", appSettings.language),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9090AB), fontSize = 11.sp)
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val body = buildEmailBody()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Aura Feedback", body)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, Localization.getString("feedback_copied", appSettings.language), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.getString("feedback_copy", appSettings.language), fontSize = 11.sp)
                    }

                    Button(
                        onClick = { submitFeedback() },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("feedback_submit_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.getString("feedback_submit", appSettings.language), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
