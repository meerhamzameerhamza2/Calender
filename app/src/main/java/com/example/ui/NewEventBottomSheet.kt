package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EventEntity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ImportantPink
import com.example.ui.theme.SundayRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventBottomSheet(
    sheetState: SheetState? = null,
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    editingEvent: EventEntity? = null,
    initialIsImportantDay: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (EventEntity) -> Unit,
    onDelete: ((EventEntity) -> Unit)? = null
) {
    var isImportantDay by remember(editingEvent, initialIsImportantDay) {
        mutableStateOf(editingEvent?.isImportantDay ?: initialIsImportantDay)
    }
    var title by remember(editingEvent) { mutableStateOf(editingEvent?.title ?: "") }
    var isAllDay by remember(editingEvent) { mutableStateOf(editingEvent?.isAllDay ?: false) }
    var location by remember(editingEvent) { mutableStateOf(editingEvent?.location ?: "") }
    var remark by remember(editingEvent) { mutableStateOf(editingEvent?.remark ?: "") }
    var alertType by remember(editingEvent) {
        mutableStateOf(
            if (editingEvent?.alertType == "Alarm & Sound") "Alarm & Sound, Vibrate"
            else (editingEvent?.alertType ?: "Alarm & Sound, Vibrate")
        )
    }
    var repeatType by remember(editingEvent, initialIsImportantDay) {
        mutableStateOf(
            when (editingEvent?.repeatType) {
                "Daily" -> "Every day"
                "Weekly" -> "Every week"
                "Monthly" -> "Every month"
                "Yearly" -> "Every year"
                "Once", null -> if (initialIsImportantDay && editingEvent == null) "Every year" else "Never"
                else -> editingEvent?.repeatType ?: "Never"
            }
        )
    }
    var reminderMinutes by remember(editingEvent) { mutableIntStateOf(editingEvent?.reminderMinutesBefore ?: 15) }

    var isStartExpanded by remember { mutableStateOf(false) }
    var isEndExpanded by remember { mutableStateOf(false) }
    var showRepeatPopup by remember { mutableStateOf(false) }
    var showNotificationPopup by remember { mutableStateOf(false) }
    var showAlertTypePopup by remember { mutableStateOf(false) }

    val initialStartCal = remember {
        Calendar.getInstance().apply {
            if (editingEvent != null) {
                timeInMillis = editingEvent.startTimestamp
            } else {
                set(initialYear, initialMonth - 1, initialDay, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    val initialEndCal = remember {
        Calendar.getInstance().apply {
            if (editingEvent != null) {
                timeInMillis = editingEvent.endTimestamp
            } else {
                set(initialYear, initialMonth - 1, initialDay, 10, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    var startTimestamp by remember { mutableStateOf(initialStartCal.timeInMillis) }
    var endTimestamp by remember { mutableStateOf(initialEndCal.timeInMillis) }

    val dateFormat = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Repeat Popup Dialog
    if (showRepeatPopup) {
        val repeatOptions = listOf(
            "Never",
            "Every day",
            "Every week",
            "Every month",
            "Every year"
        )
        AlertDialog(
            onDismissRequest = { showRepeatPopup = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Repeat",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    repeatOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repeatType = option
                                    showRepeatPopup = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (repeatType == option),
                                onClick = {
                                    repeatType = option
                                    showRepeatPopup = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricBlue,
                                    unselectedColor = TextTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (repeatType == option) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatPopup = false }) {
                    Text(text = "Cancel", color = ElectricBlue)
                }
            }
        )
    }

    // Notification Popup Dialog
    if (showNotificationPopup) {
        val notificationOptions = listOf(
            -1 to "None",
            0 to "At time of event",
            5 to "5 minutes before",
            10 to "10 minutes before",
            15 to "15 minutes before",
            30 to "30 minutes before",
            60 to "1 hour before",
            1440 to "1 day before"
        )
        var selectedTempMinutes by remember(reminderMinutes) { mutableIntStateOf(reminderMinutes) }
        AlertDialog(
            onDismissRequest = { showNotificationPopup = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Notification Reminder",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    notificationOptions.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTempMinutes = mins
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedTempMinutes == mins),
                                onClick = {
                                    selectedTempMinutes = mins
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricBlue,
                                    unselectedColor = TextTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = if (selectedTempMinutes == mins) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderMinutes = selectedTempMinutes
                    showNotificationPopup = false
                }) {
                    Text(text = "OK", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPopup = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Alert Type Selector Dialog (Alarm & Sound, Vibrate; Notification; Vibrate Only; Silent)
    if (showAlertTypePopup) {
        val alertOptions = listOf(
            "Alarm & Sound, Vibrate" to "High priority alarm sound with vibration",
            "Notification" to "Standard notification tone and banner",
            "Vibrate Only" to "Vibration only without ringtone sound",
            "Silent" to "Silent notification icon in status bar"
        )
        AlertDialog(
            onDismissRequest = { showAlertTypePopup = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Alert & Reminder Type",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    alertOptions.forEach { (type, description) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    alertType = type
                                    showAlertTypePopup = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (alertType == type),
                                onClick = {
                                    alertType = type
                                    showAlertTypePopup = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ElectricBlue,
                                    unselectedColor = TextTertiary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = type,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (alertType == type) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    text = description,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlertTypePopup = false }) {
                    Text(text = "Cancel", color = ElectricBlue)
                }
            }
        )
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val locationFocusRequester = remember { FocusRequester() }
    val remarkFocusRequester = remember { FocusRequester() }

    // Handle system back button to cleanly close the sheet
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = {
            // Strictly prevent auto-closing on scroll up/down or outside clicks
            // User requested: "scroll up ya down krne se ye close nhi hoga only close icon button click pr hi close hoga"
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val maxSheetHeight = (maxHeight - 24.dp).coerceAtLeast(300.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF14171F),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
            // Header Bar: Close, Title, Save Check (Pinned at top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("event_close_button")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = if (editingEvent == null) "New Schedule" else "Edit Schedule",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (editingEvent != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(editingEvent)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("event_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = SundayRed
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val event = (editingEvent ?: EventEntity(
                                    title = title,
                                    startTimestamp = startTimestamp,
                                    endTimestamp = endTimestamp
                                )).copy(
                                    title = title,
                                    startTimestamp = startTimestamp,
                                    endTimestamp = endTimestamp,
                                    isAllDay = isAllDay,
                                    isImportantDay = isImportantDay,
                                    repeatType = repeatType,
                                    reminderMinutesBefore = reminderMinutes,
                                    alertType = alertType,
                                    location = location,
                                    remark = remark
                                )
                                onSave(event)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("event_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (title.isNotBlank()) ElectricBlue else TextTertiary
                        )
                    }
                }
            }

            // Scrollable Form Container (Adapts smoothly without overflowing)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Tabs: "Event" vs "Important Day" (matches Infinix Image 5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceCard)
                    .padding(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isImportantDay) ElectricBlue else Color.Transparent)
                        .clickable { isImportantDay = false }
                        .padding(vertical = 10.dp)
                        .testTag("tab_event")
                ) {
                    Text(
                        text = "Event",
                        color = if (!isImportantDay) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isImportantDay) ImportantPink else Color.Transparent)
                        .clickable { isImportantDay = true }
                        .padding(vertical = 10.dp)
                        .testTag("tab_important_day")
                ) {
                    Text(
                        text = "Important Day",
                        color = if (isImportantDay) Color.White else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title TextField
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (title.isEmpty()) {
                    Text(
                        text = if (isImportantDay) "Enter important day title..." else "Enter event title...",
                        color = TextTertiary,
                        fontSize = 16.sp
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(if (isImportantDay) ImportantPink else ElectricBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timing Card (Matching Image 1: All-day switch, Start Time & End Time with <> and Wheel Picker)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurfaceCard)
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp)
            ) {
                // All-day Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "All-day",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = if (isImportantDay) ImportantPink else ElectricBlue,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceElevated
                        ),
                        modifier = Modifier.testTag("event_allday_switch")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Start Time Section (Clickable header matching Image 1)
                val startFormatted = remember(startTimestamp, isAllDay) {
                    val dateCal = Calendar.getInstance().apply { timeInMillis = startTimestamp }
                    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(dateCal.time)
                    val dayNum = dateCal.get(Calendar.DAY_OF_MONTH)
                    val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(dateCal.time)
                    if (isAllDay) {
                        "$dayOfWeek, $dayNum $monthName"
                    } else {
                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(dateCal.time).lowercase(Locale.getDefault())
                        "$dayOfWeek, $dayNum $monthName $timeStr"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            isStartExpanded = !isStartExpanded
                            if (isStartExpanded) isEndExpanded = false
                        }
                        .padding(vertical = 4.dp)
                        .testTag("event_start_row")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start Time",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isStartExpanded) "⌃" else "⌄",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = startFormatted,
                        color = if (isStartExpanded) ElectricBlue else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Collapsible Start Date/Time Wheel Picker (Image 1)
                AnimatedVisibility(
                    visible = isStartExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                        WheelDateTimePicker(
                            timestamp = startTimestamp,
                            isAllDay = isAllDay,
                            onTimestampChange = { newTs ->
                                startTimestamp = newTs
                                if (endTimestamp < startTimestamp) {
                                    endTimestamp = startTimestamp + 3600000L
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // End Time Section (Clickable header matching Image 1)
                val endFormatted = remember(endTimestamp, isAllDay) {
                    val dateCal = Calendar.getInstance().apply { timeInMillis = endTimestamp }
                    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(dateCal.time)
                    val dayNum = dateCal.get(Calendar.DAY_OF_MONTH)
                    val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(dateCal.time)
                    if (isAllDay) {
                        "$dayOfWeek, $dayNum $monthName"
                    } else {
                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(dateCal.time).lowercase(Locale.getDefault())
                        "$dayOfWeek, $dayNum $monthName $timeStr"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            isEndExpanded = !isEndExpanded
                            if (isEndExpanded) isStartExpanded = false
                        }
                        .padding(vertical = 4.dp)
                        .testTag("event_end_row")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End Time",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isEndExpanded) "⌃" else "⌄",
                            color = TextSecondary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = endFormatted,
                        color = if (isEndExpanded) ElectricBlue else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Collapsible End Date/Time Wheel Picker (Image 1)
                AnimatedVisibility(
                    visible = isEndExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                        WheelDateTimePicker(
                            timestamp = endTimestamp,
                            isAllDay = isAllDay,
                            onTimestampChange = { newTs ->
                                endTimestamp = newTs
                                if (endTimestamp < startTimestamp) {
                                    startTimestamp = endTimestamp
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Card: Repeat, Reminder, Location, Remark
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceCard)
                    .padding(16.dp)
            ) {
                // Repeat Selector (Opens Popup Dialog)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showRepeatPopup = true }
                        .padding(vertical = 8.dp)
                        .testTag("event_repeat_selector")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Repeat", color = TextPrimary, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = repeatType,
                            color = ElectricBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reminder Selector (Opens Popup Dialog)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showNotificationPopup = true }
                        .padding(vertical = 8.dp)
                        .testTag("event_notification_selector")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Notification", color = TextPrimary, fontSize = 14.sp)
                    }
                    val reminderText = when (reminderMinutes) {
                        -1 -> "None"
                        0 -> "At time of event"
                        5 -> "5 min before"
                        10 -> "10 min before"
                        15 -> "15 min before"
                        30 -> "30 min before"
                        60 -> "1 hour before"
                        1440 -> "1 day before"
                        else -> "$reminderMinutes min before"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reminderText,
                            color = ElectricBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Alert Type Selector (Alarm, Notification, Vibrate Only, Silent)
                if (reminderMinutes >= 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAlertTypePopup = true }
                            .padding(vertical = 8.dp)
                            .testTag("event_alert_type_selector")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (alertType.contains("Alarm")) ImportantPink else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Alert Type", color = TextPrimary, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alertType,
                                color = if (alertType.contains("Alarm")) ImportantPink else ElectricBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { locationFocusRequester.requestFocus() }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (location.isEmpty()) {
                            Text(text = "Add location", color = TextTertiary, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = location,
                            onValueChange = { location = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(ElectricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(locationFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            delay(150)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                }
                                .testTag("event_location_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remark Input
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { remarkFocusRequester.requestFocus() }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp, max = 160.dp)
                    ) {
                        if (remark.isEmpty()) {
                            Text(text = "Add note or remark", color = TextTertiary, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = remark,
                            onValueChange = { remark = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            cursorBrush = SolidColor(ElectricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(remarkFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            delay(150)
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                }
                                .testTag("event_remark_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
                }
            }
        }
    }
}
}

@Composable
fun DateTimePickerInline(
    timestamp: Long,
    isAllDay: Boolean,
    accentColor: Color,
    onTimestampChange: (Long) -> Unit
) {
    val cal = remember(timestamp) {
        Calendar.getInstance().apply { timeInMillis = timestamp }
    }

    val dayFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Date Controls: < Day > + Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = timestamp
                        add(Calendar.DAY_OF_MONTH, -1)
                    }
                    onTimestampChange(newCal.timeInMillis)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = TextPrimary
                )
            }

            Text(
                text = dayFormat.format(Date(timestamp)),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = timestamp
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                    onTimestampChange(newCal.timeInMillis)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = TextPrimary
                )
            }
        }

        // Quick date presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf("Today" to 0, "Tomorrow" to 1, "+1 Week" to 7)
            presets.forEach { (label, daysAhead) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            val newCal = Calendar.getInstance().apply {
                                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                                val currentMin = cal.get(Calendar.MINUTE)
                                time = Date()
                                add(Calendar.DAY_OF_MONTH, daysAhead)
                                set(Calendar.HOUR_OF_DAY, currentHour)
                                set(Calendar.MINUTE, currentMin)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onTimestampChange(newCal.timeInMillis)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // If not all-day, provide Time Selection (Hour, Minute, AM/PM)
        if (!isAllDay) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder))
            Spacer(modifier = Modifier.height(10.dp))

            val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
            val minute = cal.get(Calendar.MINUTE)
            val isPm = cal.get(Calendar.AM_PM) == Calendar.PM

            Text(
                text = "Select Time",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Hour chips: 1 to 12
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time adjustment -1h / +1h
                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = timestamp
                            add(Calendar.HOUR_OF_DAY, -1)
                        }
                        onTimestampChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "-1 Hour",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, if (isPm) "PM" else "AM"),
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = timestamp
                            add(Calendar.HOUR_OF_DAY, 1)
                        }
                        onTimestampChange(newCal.timeInMillis)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "+1 Hour",
                        tint = TextPrimary
                    )
                }
            }

            // Minute Chips & AM/PM Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val minOptions = listOf(0, 15, 30, 45)
                minOptions.forEach { m ->
                    val isSelected = minute == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor else DarkSurfaceCard)
                            .clickable {
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = timestamp
                                    set(Calendar.MINUTE, m)
                                }
                                onTimestampChange(newCal.timeInMillis)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), ":%02d", m),
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // AM / PM toggle
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = timestamp
                                val currentAmPm = get(Calendar.AM_PM)
                                set(Calendar.AM_PM, if (currentAmPm == Calendar.AM) Calendar.PM else Calendar.AM)
                            }
                            onTimestampChange(newCal.timeInMillis)
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPm) "Switch to AM" else "Switch to PM",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

