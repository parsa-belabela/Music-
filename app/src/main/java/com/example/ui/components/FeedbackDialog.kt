package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
    val lang = appSettings.language

    fun buildEmailBody(): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return """
Category: ${Localization.getString(selectedCategory.labelKey, appSettings.language)}
Timestamp: $dateStr

--- User Message ---
$messageText

--- System & App Diagnostics ---
App: Aura High-Fidelity Music Player
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
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
            context.startActivity(Intent.createChooser(emailIntent, Localization.getString("send_feedback", lang)))
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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Deep blur scrim container obscuring everything behind it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE06060C))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Modal Card (Consumes click so inside clicks don't dismiss)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* consume click */ }
                    )
                    .liquidGlass(
                        shape = RoundedCornerShape(26.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = palette.primary,
                        tintAlpha = 0.28f,
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.primary.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Feedback,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = Localization.getString("feedback_dialog_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "parsaghorbani0000@gmail.com",
                                    style = MaterialTheme.typography.labelSmall.copy(color = palette.accent)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x22FFFFFF))

                    // Category Selector
                    Text(
                        text = Localization.getString("feedback_type", lang),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB0B0C0)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedbackCategory.values().forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) palette.accent.copy(alpha = 0.30f) else Color(0x18FFFFFF),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.2.dp, palette.accent) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedCategory = cat }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = Localization.getString(cat.labelKey, lang),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFF888899)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Message Input Field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            if (hasError && it.isNotBlank()) hasError = false
                        },
                        placeholder = {
                            Text(
                                text = Localization.getString("feedback_message_hint", lang),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280))
                            )
                        },
                        isError = hasError,
                        minLines = 4,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x18000000),
                            unfocusedContainerColor = Color(0x18000000),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = palette.accent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_text_field")
                    )

                    if (hasError) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "لطفاً توضیحات خود را وارد کنید" else "Please enter your message before sending.",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFF43F5E))
                        )
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCCCCCC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Localization.getString("cancel", lang))
                        }

                        Button(
                            onClick = { submitFeedback() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("submit_feedback_button")
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Localization.getString("feedback_submit", lang),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
