package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CloudflareMusicApi
import com.example.CloudflareTrack
import com.example.monetization.EntitlementManager
import com.example.ui.components.VipLockGateCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTrackSelected: (CloudflareTrack) -> Unit
) {
    val context = LocalContext.current
    val entitlementManager = remember { EntitlementManager.getInstance(context) }
    val isVip by entitlementManager.isVip.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CloudflareTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // The explicit worker URL
    val workerUrl = "https://alubm.parsaghorbani8861.workers.dev"

    // Only search if VIP
    LaunchedEffect(searchQuery, isVip) {
        if (!isVip) return@LaunchedEffect
        
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        
        delay(500) // Debounce
        
        isLoading = true
        errorMessage = null
        
        val result = CloudflareMusicApi.searchOrFetchTracks(searchQuery, entitlementManager.getDeviceId())
        result.onSuccess { tracks ->
            searchResults = tracks
            isLoading = false
        }.onFailure { error ->
            errorMessage = error.localizedMessage ?: "خطا در جستجو"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("جستجوی آهنگ یا خواننده...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (!isVip) {
            // Show VIP Lock Card if not premium
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                VipLockGateCard(
                    workerUrl = workerUrl,
                    onUnlocked = {
                        // isVip will become true automatically, this will trigger the LaunchedEffect
                    }
                )
            }
        } else {
            // Show Search Results
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFA855F7))
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 14.sp)
                }
            } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "نتیجه‌ای یافت نشد.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { track ->
                        TrackListItem(track = track, onClick = { onTrackSelected(track) })
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun TrackListItem(track: CloudflareTrack, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for album art (in a real app, use AsyncImage from Coil)
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text("🎵", fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
