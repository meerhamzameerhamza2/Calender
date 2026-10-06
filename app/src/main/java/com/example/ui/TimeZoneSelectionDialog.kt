package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.TimeZone

data class TimeZoneItem(
    val id: String,
    val displayName: String,
    val gmtOffset: String
)

@Composable
fun TimeZoneSelectionDialog(
    currentTimeZone: String,
    onTimeZoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val popularZones = remember {
        listOf(
            TimeZoneItem("Asia/Kolkata", "India (IST)", "GMT+05:30"),
            TimeZoneItem("UTC", "Coordinated Universal Time (UTC)", "GMT+00:00"),
            TimeZoneItem("Asia/Dubai", "Dubai, UAE (GST)", "GMT+04:00"),
            TimeZoneItem("America/New_York", "New York (EST/EDT)", "GMT-05:00"),
            TimeZoneItem("America/Los_Angeles", "Los Angeles (PST/PDT)", "GMT-08:00"),
            TimeZoneItem("Europe/London", "London (GMT/BST)", "GMT+00:00"),
            TimeZoneItem("Europe/Paris", "Paris, Berlin (CET/CEST)", "GMT+01:00"),
            TimeZoneItem("Asia/Singapore", "Singapore (SGT)", "GMT+08:00"),
            TimeZoneItem("Asia/Tokyo", "Tokyo (JST)", "GMT+09:00"),
            TimeZoneItem("Australia/Sydney", "Sydney (AEST)", "GMT+10:00"),
            TimeZoneItem("Asia/Riyadh", "Saudi Arabia (AST)", "GMT+03:00"),
            TimeZoneItem("Asia/Karachi", "Pakistan (PKT)", "GMT+05:00"),
            TimeZoneItem("Asia/Dhaka", "Bangladesh (BST)", "GMT+06:00")
        )
    }

    val allTimeZones = remember {
        TimeZone.getAvailableIDs()
            .map { id ->
                val tz = TimeZone.getTimeZone(id)
                val offsetMs = tz.rawOffset
                val hours = offsetMs / 3600000
                val mins = kotlin.math.abs((offsetMs % 3600000) / 60000)
                val sign = if (offsetMs >= 0) "+" else "-"
                val gmtString = String.format("GMT%s%02d:%02d", sign, kotlin.math.abs(hours), mins)
                TimeZoneItem(id, id.replace('_', ' '), gmtString)
            }
            .distinctBy { it.id }
            .sortedBy { it.displayName }
    }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            popularZones
        } else {
            allTimeZones.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true) ||
                it.gmtOffset.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Select Time Zone",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search country, city, GMT offset...",
                                color = TextTertiary,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(ElectricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timezone_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Popular Zone Chips
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Popular Zones",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickChips = listOf(
                            "IST" to "Asia/Kolkata",
                            "UTC" to "UTC",
                            "Dubai" to "Asia/Dubai",
                            "London" to "Europe/London",
                            "New York" to "America/New_York",
                            "Tokyo" to "Asia/Tokyo"
                        )
                        items(quickChips) { (label, zoneId) ->
                            val isSelected = currentTimeZone.contains(zoneId)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) ElectricBlue else DarkSurfaceCard)
                                    .border(1.dp, if (isSelected) ElectricBlue else DarkBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        val match = popularZones.find { it.id == zoneId }
                                        val formatted = if (match != null) "${match.id} (${match.gmtOffset})" else zoneId
                                        onTimeZoneSelected(formatted)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Time Zone List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(filteredList) { zone ->
                        val formattedZone = "${zone.id} (${zone.gmtOffset})"
                        val isSelected = currentTimeZone.contains(zone.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onTimeZoneSelected(formattedZone)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = zone.displayName,
                                    color = if (isSelected) ElectricBlue else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = "${zone.id} • ${zone.gmtOffset}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = ElectricBlue)
            }
        }
    )
}
