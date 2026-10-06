package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HolidayRepository
import com.example.model.EventEntity
import com.example.model.Holiday
import com.example.model.HolidayType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HolidayAmber
import com.example.ui.theme.ImportantPink
import com.example.ui.theme.SundayRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class AgendaItem {
    abstract val timestamp: Long
    abstract val title: String

    data class HolidayItem(
        val holiday: Holiday,
        override val timestamp: Long
    ) : AgendaItem() {
        override val title: String get() = holiday.title
        val isNationalOrGazetted: Boolean
            get() = holiday.category.contains("Gazetted", ignoreCase = true) ||
                    holiday.type == HolidayType.NATIONAL
    }

    data class UserEventItem(
        val event: EventEntity,
        override val timestamp: Long
    ) : AgendaItem() {
        override val title: String get() = event.title
        val isImportant: Boolean get() = event.isImportantDay
    }
}

private data class MonthSection(
    val title: String,
    val year: Int,
    val month: Int,
    val items: List<AgendaItem>
)

@Composable
fun AgendaViewContent(
    events: List<EventEntity>,
    onAddEventClick: () -> Unit,
    onEventClick: (EventEntity) -> Unit,
    onDeleteEventClick: ((EventEntity) -> Unit)? = null,
    onHolidayClick: ((Holiday) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var detailsEvent by remember { mutableStateOf<EventEntity?>(null) }
    var detailsHoliday by remember { mutableStateOf<Holiday?>(null) }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }

    // Start of TODAY at 00:00:00.000 for strict "Upcoming" filtering
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    // End of current year (Dec 31, 23:59:59.999) - shows ONLY current year's upcoming items
    val endOfCurrentYear = remember(currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    // Load upcoming holidays for current year only (from today until end of year)
    val upcomingHolidays = remember(currentYear, todayStart, endOfCurrentYear) {
        val rawHolidays = HolidayRepository.getHolidaysForYear(currentYear)
            .distinctBy { "${it.year}_${it.month}_${it.day}_${it.title}" }

        val list = mutableListOf<AgendaItem.HolidayItem>()
        for (h in rawHolidays) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, h.year)
                set(Calendar.MONTH, h.month - 1)
                set(Calendar.DAY_OF_MONTH, h.day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis in todayStart..endOfCurrentYear) {
                list.add(AgendaItem.HolidayItem(h, cal.timeInMillis))
            }
        }
        list
    }

    // Load upcoming user events and important days for current year only
    val upcomingEvents = remember(events, todayStart, endOfCurrentYear) {
        events.filter { ev ->
            (ev.endTimestamp >= todayStart || ev.startTimestamp >= todayStart) &&
                    ev.startTimestamp <= endOfCurrentYear
        }.map { AgendaItem.UserEventItem(it, it.startTimestamp) }
    }

    // Combined all upcoming items of current year in ONE unified chronological list
    val allUpcomingItems = remember(upcomingHolidays, upcomingEvents) {
        (upcomingHolidays + upcomingEvents).sortedBy { it.timestamp }
    }

    val monthHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val monthShortFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Group items into monthly sections for clear readability
    val monthSections = remember(allUpcomingItems) {
        val groups = linkedMapOf<String, MutableList<AgendaItem>>()
        val sectionMeta = linkedMapOf<String, Pair<Int, Int>>() // title to (year, month)

        for (item in allUpcomingItems) {
            val cal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val title = monthHeaderFormat.format(cal.time)

            if (!groups.containsKey(title)) {
                groups[title] = mutableListOf()
                sectionMeta[title] = Pair(year, month)
            }
            groups[title]?.add(item)
        }

        groups.map { (title, items) ->
            val meta = sectionMeta[title] ?: Pair(currentYear, 0)
            MonthSection(
                title = title,
                year = meta.first,
                month = meta.second,
                items = items
            )
        }
    }

    // Event Details Dialog
    if (detailsEvent != null) {
        val ev = detailsEvent!!
        val isImportant = ev.isImportantDay
        val badgeColor = if (isImportant) ImportantPink else ElectricBlue

        AlertDialog(
            onDismissRequest = { detailsEvent = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isImportant) "★ Important Day" else "Event",
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = ev.title,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkBorder)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Date & Time
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = fullDateFormat.format(Date(ev.startTimestamp)),
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val timeStr = if (ev.isAllDay) {
                                "All day"
                            } else {
                                "${timeFormat.format(Date(ev.startTimestamp))} - ${timeFormat.format(Date(ev.endTimestamp))}"
                            }
                            Text(
                                text = timeStr,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Repeat
                    if (!ev.repeatType.isNullOrBlank() && ev.repeatType != "Never" && ev.repeatType != "Once") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Repeats: ${ev.repeatType}",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Location
                    if (!ev.location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ev.location,
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Notification & Reminder
                    if (ev.reminderMinutesBefore >= 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val reminderText = if (ev.reminderMinutesBefore == 0) "At time of event" else "${ev.reminderMinutesBefore} minutes before"
                            Text(
                                text = "Reminder: $reminderText (${ev.alertType})",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Remark
                    if (ev.remark.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Remark",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ev.remark,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Delete Button
                    TextButton(
                        onClick = {
                            val target = ev
                            detailsEvent = null
                            eventToDelete = target
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = SundayRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Delete", color = SundayRed)
                    }

                    // Edit Button
                    TextButton(
                        onClick = {
                            val target = ev
                            detailsEvent = null
                            onEventClick(target)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Edit", color = ElectricBlue)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { detailsEvent = null }) {
                    Text(text = "Close", color = TextTertiary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (eventToDelete != null) {
        val ev = eventToDelete!!
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Text(
                    text = "Delete ${if (ev.isImportantDay) "Important Day" else "Event"}?",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${ev.title}\"? This action cannot be undone.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEventClick?.invoke(ev)
                        eventToDelete = null
                    }
                ) {
                    Text(text = "Delete", color = SundayRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text(text = "Cancel", color = TextTertiary)
                }
            }
        )
    }

    // Holiday Details Dialog
    if (detailsHoliday != null) {
        val h = detailsHoliday!!
        AlertDialog(
            onDismissRequest = { detailsHoliday = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HolidayAmber.copy(alpha = 0.2f))
                        .border(1.dp, HolidayAmber, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${h.category} • India",
                        color = HolidayAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = h.title,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DarkBorder)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val hCal = Calendar.getInstance().apply {
                        set(h.year, h.month - 1, h.day)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = HolidayAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = fullDateFormat.format(hCal.time),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "National and cultural celebration observed across India.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsHoliday = null }) {
                    Text(text = "Close", color = ElectricBlue)
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            // Empty state if no upcoming items in current year
            if (allUpcomingItems.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No upcoming items for $currentYear",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to add a new event or important day.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        OutlinedButton(
                            onClick = onAddEventClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Event", color = ElectricBlue)
                        }
                    }
                }
            } else {
                // Grouped Monthly Sections in one single continuous list
                monthSections.forEach { section ->
                    item(key = "header_${section.title}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = section.title.uppercase(Locale.getDefault()),
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${section.items.size} ${if (section.items.size == 1) "item" else "items"}",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    items(
                        items = section.items,
                        key = { item ->
                            when (item) {
                                is AgendaItem.HolidayItem -> "h_${item.holiday.year}_${item.holiday.month}_${item.holiday.day}_${item.holiday.title}"
                                is AgendaItem.UserEventItem -> "e_${item.event.id}_${item.timestamp}"
                            }
                        }
                    ) { item ->
                        val date = Date(item.timestamp)
                        val dayNum = dayNumberFormat.format(date)
                        val dayOfWeek = dayOfWeekFormat.format(date)
                        val monthShort = monthShortFormat.format(date)
                        val relativeTime = getRelativeTime(item.timestamp, todayStart)

                        val isToday = isSameDay(item.timestamp, todayStart)

                        when (item) {
                            is AgendaItem.HolidayItem -> {
                                val holiday = item.holiday

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurfaceCard)
                                        .border(
                                            width = if (isToday) 1.5.dp else 1.dp,
                                            color = if (isToday) HolidayAmber else DarkBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (onHolidayClick != null) {
                                                onHolidayClick(holiday)
                                            } else {
                                                detailsHoliday = holiday
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Date Box
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(48.dp)
                                    ) {
                                        Text(
                                            text = dayNum,
                                            color = if (dayOfWeek.equals("Sun", ignoreCase = true)) SundayRed else TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp
                                        )
                                        Text(
                                            text = dayOfWeek,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp
                                        )
                                        Text(
                                            text = monthShort,
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    // Vertical Accent Bar
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(HolidayAmber)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Content
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = holiday.title,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${holiday.category} • India",
                                                color = HolidayAmber,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    // Relative Time Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(HolidayAmber.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = relativeTime,
                                            color = HolidayAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            is AgendaItem.UserEventItem -> {
                                val ev = item.event
                                val isImportant = ev.isImportantDay
                                val accentColor = if (isImportant) ImportantPink else ElectricBlue

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurfaceCard)
                                        .border(
                                            width = if (isToday) 1.5.dp else if (isImportant) 1.2.dp else 1.dp,
                                            color = if (isToday) accentColor else if (isImportant) ImportantPink.copy(alpha = 0.45f) else DarkBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            detailsEvent = ev
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Date Box
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(48.dp)
                                    ) {
                                        Text(
                                            text = dayNum,
                                            color = if (dayOfWeek.equals("Sun", ignoreCase = true)) SundayRed else TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp
                                        )
                                        Text(
                                            text = dayOfWeek,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 14.sp
                                        )
                                        Text(
                                            text = monthShort,
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    // Vertical Accent Bar
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Content
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = ev.title,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isImportant) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(ImportantPink.copy(alpha = 0.2f))
                                                        .border(0.5.dp, ImportantPink.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "★ Important",
                                                        color = ImportantPink,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timeLabel = if (ev.isAllDay) "All day" else timeFormat.format(date)
                                        val subLabel = if (!ev.location.isNullOrBlank()) {
                                            "$timeLabel • ${ev.location}"
                                        } else {
                                            timeLabel
                                        }
                                        Text(
                                            text = subLabel,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Relative Time Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = relativeTime,
                                            color = accentColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button (+) to add new event or important day
        FloatingActionButton(
            onClick = onAddEventClick,
            containerColor = ElectricBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .testTag("agenda_add_event_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Schedule",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}

private fun getRelativeTime(itemTimestamp: Long, todayStart: Long): String {
    val cItem = Calendar.getInstance().apply {
        timeInMillis = itemTimestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMillis = cItem.timeInMillis - todayStart
    val diffDays = (diffMillis / (24 * 60 * 60 * 1000L)).toInt()

    return when {
        diffDays == 0 -> "Today"
        diffDays == 1 -> "Tomorrow"
        diffDays in 2..6 -> "In $diffDays days"
        diffDays in 7..13 -> "In 1 week"
        diffDays in 14..27 -> "In ${diffDays / 7} weeks"
        diffDays in 28..59 -> "In 1 month"
        diffDays >= 60 -> "In ${diffDays / 30} months"
        else -> "Today"
    }
}
