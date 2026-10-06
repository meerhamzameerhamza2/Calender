package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarSettings
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: CalendarSettings,
    onSettingsChanged: (CalendarSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOtherCalendarDialog by remember { mutableStateOf(false) }
    var showStartWeekDialog by remember { mutableStateOf(false) }
    var showRamadanDialog by remember { mutableStateOf(false) }
    var showTimeZoneDialog by remember { mutableStateOf(false) }

    val todayDate = remember { Date() }
    val dayNumber = remember { SimpleDateFormat("d", Locale.getDefault()).format(todayDate) }
    val dayName = remember { SimpleDateFormat("EEE", Locale.getDefault()).format(todayDate).uppercase(Locale.getDefault()) }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar (Responsive padding and status bar alignment)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("settings_title")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Dynamic Icon Preview Card
            Text(
                text = "Dynamic Calendar Icon",
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E222D).copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic Icon Container preview (Dark black gray bg, blue date, white 3-letter day)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF16181D))
                        .border(1.5.dp, Color(0xFF2C303A), RoundedCornerShape(18.dp))
                        .testTag("dynamic_icon_preview")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayNumber,
                            color = ElectricBlue,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = dayName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Date & Day Icon",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adapts across OxygenOS, HyperOS, One UI and XOS. Updates automatically at midnight.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Display Options Group
            Text(
                text = "Display",
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E222D).copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Start Week On
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartWeekDialog = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Start week on", color = TextPrimary, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = settings.startWeekOn, color = ElectricBlue, fontSize = 14.sp)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))

                // Time Zone (Clickable to change time zone)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTimeZoneDialog = true }
                        .padding(vertical = 14.dp)
                        .testTag("settings_timezone_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Default time zone", color = TextPrimary, fontSize = 15.sp)
                        Text(text = settings.timeZone, color = TextSecondary, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = settings.timeZone.substringBefore(" (").substringAfterLast("/"), color = ElectricBlue, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Change Time Zone",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Holidays & Region (India Default as specified by prompt)
            Text(
                text = "Holidays & Regional",
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E222D).copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // National / Regional Holiday (India locked as default)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "National / Regional Holiday", color = TextPrimary, fontSize = 15.sp)
                        Text(text = "India (Gazetted & Restricted)", color = ElectricBlue, fontSize = 13.sp)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))

                // Religious Holidays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Religious Holidays", color = TextPrimary, fontSize = 15.sp)
                        Text(text = "All Major Festivals", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Other Calendars (Only Muslim & Hindu Calendar Support as specified by prompt)
            Text(
                text = "Other Calendars",
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E222D).copy(alpha = 0.82f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Calendar Selection: None / Muslim / Hindu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOtherCalendarDialog = true }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Secondary Calendar", color = TextPrimary, fontSize = 15.sp)
                        Text(text = settings.otherCalendar, color = ElectricBlue, fontSize = 13.sp)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (settings.otherCalendar == "Muslim Calendar") {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))

                    // Adjust Ramadan / Hijri Date Offset (-2 to +2 days)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRamadanDialog = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Adjust Ramadan / Hijri date", color = TextPrimary, fontSize = 15.sp)
                            val adjStr = if (settings.ramadanAdjustment > 0) "+${settings.ramadanAdjustment} day" else "${settings.ramadanAdjustment} days"
                            Text(text = if (settings.ramadanAdjustment == 0) "Default (0 days)" else adjStr, color = ElectricBlue, fontSize = 13.sp)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Other Calendar Selection Dialog
    if (showOtherCalendarDialog) {
        val calendarOptions = listOf("None", "Muslim Calendar", "Hindu Calendar")
        AlertDialog(
            onDismissRequest = { showOtherCalendarDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text(text = "Other Calendars", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    calendarOptions.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSettingsChanged(settings.copy(otherCalendar = option))
                                    showOtherCalendarDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = (settings.otherCalendar == option),
                                onClick = {
                                    onSettingsChanged(settings.copy(otherCalendar = option))
                                    showOtherCalendarDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = option, color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOtherCalendarDialog = false }) {
                    Text(text = "Close", color = ElectricBlue)
                }
            }
        )
    }

    // Start Week On Dialog
    if (showStartWeekDialog) {
        val options = listOf("Sunday", "Monday")
        AlertDialog(
            onDismissRequest = { showStartWeekDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text(text = "Start week on", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSettingsChanged(settings.copy(startWeekOn = option))
                                    showStartWeekDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = (settings.startWeekOn == option),
                                onClick = {
                                    onSettingsChanged(settings.copy(startWeekOn = option))
                                    showStartWeekDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = option, color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStartWeekDialog = false }) {
                    Text(text = "Close", color = ElectricBlue)
                }
            }
        )
    }

    // Ramadan / Hijri Date Offset Dialog
    if (showRamadanDialog) {
        val adjustments = listOf(-2, -1, 0, 1, 2)
        AlertDialog(
            onDismissRequest = { showRamadanDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text(text = "Adjust Ramadan / Hijri date", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Offset the Islamic lunar calendar by days according to local moon sighting:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    adjustments.forEach { adj ->
                        val label = when {
                            adj == 0 -> "0 days (Default)"
                            adj > 0 -> "+$adj day"
                            else -> "$adj days"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSettingsChanged(settings.copy(ramadanAdjustment = adj))
                                    showRamadanDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (settings.ramadanAdjustment == adj),
                                onClick = {
                                    onSettingsChanged(settings.copy(ramadanAdjustment = adj))
                                    showRamadanDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ElectricBlue)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, color = TextPrimary, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRamadanDialog = false }) {
                    Text(text = "Close", color = ElectricBlue)
                }
            }
        )
    }

    if (showTimeZoneDialog) {
        TimeZoneSelectionDialog(
            currentTimeZone = settings.timeZone,
            onTimeZoneSelected = { newTz ->
                onSettingsChanged(settings.copy(timeZone = newTz))
            },
            onDismiss = { showTimeZoneDialog = false }
        )
    }
}
