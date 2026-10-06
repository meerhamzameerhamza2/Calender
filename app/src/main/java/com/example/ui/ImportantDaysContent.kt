package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HolidayRepository
import com.example.data.IndianHolidays
import com.example.model.EventEntity
import com.example.model.Holiday
import com.example.model.HolidayType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HolidayAmber
import com.example.ui.theme.ImportantPink
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ImportantTab {
    ALL,
    UPCOMING,
    PAST
}

enum class ImportantFilter {
    ALL,
    MY_EVENTS,
    HOLIDAYS
}

sealed class ImportantItem {
    abstract val timestamp: Long
    data class UserMilestone(val event: EventEntity) : ImportantItem() {
        override val timestamp: Long = event.startTimestamp
    }
    data class NationalDay(val holiday: Holiday, override val timestamp: Long) : ImportantItem()
}

@Composable
fun ImportantDaysContent(
    importantEvents: List<EventEntity>,
    onAddImportantDayClick: () -> Unit,
    onEventClick: (EventEntity) -> Unit,
    onHolidayClick: ((Holiday) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentYr = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var selectedYear by remember { mutableIntStateOf(currentYr) }
    var showYearPickerDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(ImportantTab.UPCOMING) }
    val monthNames = remember { listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }

    val dateFormat = remember { SimpleDateFormat("EEE, d MMMM yyyy", Locale.getDefault()) }

    // Calculate today's midnight start timestamp
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val now = System.currentTimeMillis()

    // Trigger dynamic holiday load for the selected year
    LaunchedEffect(selectedYear) {
        HolidayRepository.loadHolidays(selectedYear)
    }

    // Observe dynamic holidays from HolidayRepository
    val holidaysMap by HolidayRepository.holidaysByYear.collectAsState()

    // Dynamic holidays strictly for selectedYear (Gazetted, National, Festival, Jayanti)
    val yearHolidays = remember(holidaysMap, selectedYear) {
        val list = mutableListOf<ImportantItem.NationalDay>()
        val hList = holidaysMap[selectedYear] ?: IndianHolidays.getHolidaysForYear(selectedYear)
        for (h in hList) {
            val cal = Calendar.getInstance().apply {
                set(h.year, h.month - 1, h.day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            list.add(ImportantItem.NationalDay(h, cal.timeInMillis))
        }
        list
    }

    // Combine user milestones occurring in selectedYear (or yearly recurring events projected to selectedYear) and holidays of selectedYear
    val allItems = remember(importantEvents, yearHolidays, selectedYear) {
        val list = mutableListOf<ImportantItem>()
        for (ev in importantEvents) {
            val evCal = Calendar.getInstance().apply { timeInMillis = ev.startTimestamp }
            val evYear = evCal.get(Calendar.YEAR)
            val rep = ev.repeatType.trim()
            val isYearly = rep.equals("Yearly", ignoreCase = true) || rep.equals("Every year", ignoreCase = true)

            if (evYear == selectedYear) {
                list.add(ImportantItem.UserMilestone(ev))
            } else if (isYearly) {
                val evMonth = evCal.get(Calendar.MONTH)
                val evDay = evCal.get(Calendar.DAY_OF_MONTH)
                val evHour = evCal.get(Calendar.HOUR_OF_DAY)
                val evMin = evCal.get(Calendar.MINUTE)

                val nextOccurrenceCal = Calendar.getInstance().apply {
                    set(selectedYear, evMonth, evDay, evHour, evMin, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val duration = (ev.endTimestamp - ev.startTimestamp).coerceAtLeast(0L)
                val projectedEvent = ev.copy(
                    startTimestamp = nextOccurrenceCal.timeInMillis,
                    endTimestamp = nextOccurrenceCal.timeInMillis + duration
                )
                list.add(ImportantItem.UserMilestone(projectedEvent))
            }
        }
        list.addAll(yearHolidays)
        list.sortedBy { it.timestamp }
    }

    // Upcoming list sorted by NEAREST FIRST (ascending by timestamp)
    val upcomingItems = remember(allItems, todayStart) {
        allItems.filter { it.timestamp >= todayStart }.sortedBy { it.timestamp }
    }

    // Past list sorted by MOST RECENTLY PASSED FIRST (descending by timestamp)
    val pastItems = remember(allItems, todayStart) {
        allItems.filter { it.timestamp < todayStart }.sortedByDescending { it.timestamp }
    }

    var selectedFilter by remember { mutableStateOf(ImportantFilter.ALL) }
    val userEventsCount = remember(allItems) { allItems.count { it is ImportantItem.UserMilestone } }
    val holidaysCount = remember(allItems) { allItems.count { it is ImportantItem.NationalDay } }

    val activeList = when (selectedTab) {
        ImportantTab.ALL -> allItems
        ImportantTab.UPCOMING -> upcomingItems
        ImportantTab.PAST -> pastItems
    }
    val filteredList = remember(activeList, selectedFilter) {
        when (selectedFilter) {
            ImportantFilter.ALL -> activeList
            ImportantFilter.MY_EVENTS -> activeList.filter { it is ImportantItem.UserMilestone }
            ImportantFilter.HOLIDAYS -> activeList.filter { it is ImportantItem.NationalDay }
        }
    }

    if (showYearPickerDialog) {
        YearPickerDialog(
            currentYear = currentYr,
            selectedYear = selectedYear,
            onYearSelected = { yr ->
                selectedYear = yr
                if (yr < currentYr && selectedTab == ImportantTab.UPCOMING) selectedTab = ImportantTab.ALL
                if (yr > currentYr && selectedTab == ImportantTab.PAST) selectedTab = ImportantTab.ALL
                if (yr == currentYr) selectedTab = ImportantTab.UPCOMING
                showYearPickerDialog = false
            },
            onDismiss = { showYearPickerDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. SEGMENTED TABS: All | Upcoming | Past (Full width top bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF181B24))
                    .border(1.dp, Color(0xFF292E3D), RoundedCornerShape(14.dp))
                    .padding(3.dp)
            ) {
                // All Pill
                val isAll = selectedTab == ImportantTab.ALL
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isAll) ElectricBlue else Color.Transparent)
                        .clickable { selectedTab = ImportantTab.ALL }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All (${allItems.size})",
                        color = if (isAll) Color.White else Color(0xFF8E929B),
                        fontSize = 12.sp,
                        fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Upcoming Pill
                val isUpcoming = selectedTab == ImportantTab.UPCOMING
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isUpcoming) ElectricBlue else Color.Transparent)
                        .clickable { selectedTab = ImportantTab.UPCOMING }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Upcoming (${upcomingItems.size})",
                        color = if (isUpcoming) Color.White else Color(0xFF8E929B),
                        fontSize = 12.sp,
                        fontWeight = if (isUpcoming) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Past Pill
                val isPast = selectedTab == ImportantTab.PAST
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (isPast) ElectricBlue else Color.Transparent)
                        .clickable { selectedTab = ImportantTab.PAST }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Past (${pastItems.size})",
                        color = if (isPast) Color.White else Color(0xFF8E929B),
                        fontSize = 12.sp,
                        fontWeight = if (isPast) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // 2. FILTER ROW: Year Selector Pill placed before "All" button, followed by My Events & Holidays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Selector Pill (Placed right before All button)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF181B24))
                        .border(
                            1.dp,
                            if (selectedYear == currentYr) ElectricBlue.copy(alpha = 0.7f) else Color(0xFF292E3D),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { showYearPickerDialog = true }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$selectedYear",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Choose Year",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                ImportantFilterChip(
                    label = "All",
                    isSelected = selectedFilter == ImportantFilter.ALL,
                    onClick = { selectedFilter = ImportantFilter.ALL }
                )
                ImportantFilterChip(
                    label = if (userEventsCount > 0) "My Events ($userEventsCount)" else "My Events",
                    isSelected = selectedFilter == ImportantFilter.MY_EVENTS,
                    onClick = { selectedFilter = ImportantFilter.MY_EVENTS },
                    accentColor = ImportantPink
                )
                ImportantFilterChip(
                    label = "Holidays ($holidaysCount)",
                    isSelected = selectedFilter == ImportantFilter.HOLIDAYS,
                    onClick = { selectedFilter = ImportantFilter.HOLIDAYS },
                    accentColor = HolidayAmber
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. SCROLLABLE LIST OF MILESTONES (Scrolls freely all the way to bottom edge)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (filteredList.isEmpty()) {
                    item {
                        if (selectedFilter == ImportantFilter.MY_EVENTS) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 44.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(ImportantPink.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = ImportantPink,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (selectedTab == ImportantTab.UPCOMING) "No Upcoming Important Events" else "No Past Important Events",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add anniversaries, birthdays, milestones, and special events to celebrate.",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onAddImportantDayClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = ImportantPink),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("btn_add_important_event_empty")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Add Important Event", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == ImportantTab.UPCOMING) Icons.Default.CheckCircle else Icons.Default.History,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTab == ImportantTab.UPCOMING) "No upcoming milestones" else "No past milestones yet",
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (selectedTab == ImportantTab.UPCOMING) {
                                    Text(
                                        text = "Tap (+) below to add your next important day",
                                        color = TextTertiary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (selectedTab) {
                        ImportantTab.UPCOMING -> {
                            // Feature the VERY NEAREST milestone prominently at the top
                            val nearest = filteredList.first()
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Next Milestone",
                                    color = ElectricBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                NearestMilestoneHeroCard(
                                    item = nearest,
                                    dateFormat = dateFormat,
                                    monthNames = monthNames,
                                    now = now,
                                    onClick = {
                                        when (nearest) {
                                            is ImportantItem.UserMilestone -> onEventClick(nearest.event)
                                            is ImportantItem.NationalDay -> onHolidayClick?.invoke(nearest.holiday)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                if (filteredList.size > 1) {
                                    Text(
                                        text = "Coming Up Next",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }

                            // List remaining upcoming items
                            items(filteredList.drop(1)) { item ->
                                ImportantDayRow(
                                    item = item,
                                    dateFormat = dateFormat,
                                    monthNames = monthNames,
                                    now = now,
                                    isPast = false,
                                    onClick = {
                                        when (item) {
                                            is ImportantItem.UserMilestone -> onEventClick(item.event)
                                            is ImportantItem.NationalDay -> onHolidayClick?.invoke(item.holiday)
                                        }
                                    }
                                )
                            }
                        }
                        ImportantTab.PAST -> {
                            // PAST TAB CONTENT
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Past Milestones & Holidays ($selectedYear)",
                                    color = TextTertiary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(filteredList) { item ->
                                ImportantDayRow(
                                    item = item,
                                    dateFormat = dateFormat,
                                    monthNames = monthNames,
                                    now = now,
                                    isPast = true,
                                    onClick = {
                                        when (item) {
                                            is ImportantItem.UserMilestone -> onEventClick(item.event)
                                            is ImportantItem.NationalDay -> onHolidayClick?.invoke(item.holiday)
                                        }
                                    }
                                )
                            }
                        }
                        ImportantTab.ALL -> {
                            // ALL TAB CONTENT: Chronological order for the selected year
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "All Milestones & Holidays ($selectedYear)",
                                    color = ElectricBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(filteredList) { item ->
                                val isPast = item.timestamp < todayStart
                                ImportantDayRow(
                                    item = item,
                                    dateFormat = dateFormat,
                                    monthNames = monthNames,
                                    now = now,
                                    isPast = isPast,
                                    onClick = {
                                        when (item) {
                                            is ImportantItem.UserMilestone -> onEventClick(item.event)
                                            is ImportantItem.NationalDay -> onHolidayClick?.invoke(item.holiday)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Spacer at very bottom so last item is easily reachable above the FAB
                item {
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }

        // 3. FLOATING ADD BUTTON (+) WITHOUT SOLID BLOCKING BACKGROUND
        // Positioned cleanly with zero background barrier so list is always visible underneath
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            FloatingActionButton(
                onClick = onAddImportantDayClick,
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("important_day_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Important Day",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Prominent Hero Card for the Nearest Upcoming Milestone
 */
@Composable
private fun NearestMilestoneHeroCard(
    item: ImportantItem,
    dateFormat: SimpleDateFormat,
    monthNames: List<String>,
    now: Long,
    onClick: () -> Unit
) {
    val daysDiff = TimeUnit.MILLISECONDS.toDays(item.timestamp - now)
    val daysLabel = when {
        daysDiff <= 0L -> "Today!"
        daysDiff == 1L -> "Tomorrow"
        else -> "In $daysDiff days"
    }

    val (title, subtitle, isUser) = when (item) {
        is ImportantItem.UserMilestone -> Triple(item.event.title, dateFormat.format(Date(item.event.startTimestamp)), true)
        is ImportantItem.NationalDay -> Triple(
            item.holiday.title,
            "${item.holiday.day} ${monthNames.getOrElse(item.holiday.month) { "" }} ${item.holiday.year} • ${item.holiday.category}",
            false
        )
    }

    val accent = if (isUser) ImportantPink else HolidayAmber

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF141D2B),
                        Color(0xFF161922)
                    )
                )
            )
            .border(BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ImportantPink.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "My Event",
                                color = ImportantPink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = daysLabel,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Standard Row for Upcoming and Past milestones
 */
@Composable
private fun ImportantDayRow(
    item: ImportantItem,
    dateFormat: SimpleDateFormat,
    monthNames: List<String>,
    now: Long,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val daysDiff = TimeUnit.MILLISECONDS.toDays(item.timestamp - now)
    val daysLabel = when {
        isPast -> {
            val passedDays = -daysDiff
            when {
                passedDays <= 1L -> "Yesterday"
                passedDays < 30L -> "$passedDays days ago"
                passedDays < 365L -> "${passedDays / 30} mos ago"
                else -> "${passedDays / 365} yrs ago"
            }
        }
        else -> {
            when {
                daysDiff == 0L -> "Today"
                daysDiff == 1L -> "Tomorrow"
                else -> "In $daysDiff days"
            }
        }
    }

    val (title, subtitle, isUser) = when (item) {
        is ImportantItem.UserMilestone -> Triple(item.event.title, dateFormat.format(Date(item.event.startTimestamp)), true)
        is ImportantItem.NationalDay -> Triple(
            item.holiday.title,
            "${item.holiday.day} ${monthNames.getOrElse(item.holiday.month) { "" }} ${item.holiday.year} • ${item.holiday.category}",
            false
        )
    }

    val accent = if (isUser) ImportantPink else HolidayAmber
    val cardAlpha = if (isPast) 0.65f else 1.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard.copy(alpha = cardAlpha))
            .border(1.dp, DarkBorder.copy(alpha = cardAlpha), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (isPast) 0.12f else 0.20f))
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (isPast) accent.copy(alpha = 0.6f) else accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = if (isPast) TextPrimary.copy(alpha = 0.85f) else TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isUser) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ImportantPink.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "My Event",
                            color = ImportantPink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = if (isPast) 0.10f else 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = daysLabel,
                color = if (isPast) TextSecondary else accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ImportantFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = ElectricBlue
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.22f) else Color(0xFF1B1F2A)
            )
            .border(
                1.dp,
                if (isSelected) accentColor else Color(0xFF2C3242),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Clean, modern Year Picker Dialog allowing user to select ANY year dynamically
 */
@Composable
private fun YearPickerDialog(
    currentYear: Int,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var decadeStart by remember { mutableIntStateOf((selectedYear / 12) * 12) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14171F),
            border = BorderStroke(1.dp, Color(0xFF292E3D)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: "Select Year" + Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Year",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Decade Range Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1E29))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { decadeStart -= 12 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Years",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "$decadeStart – ${decadeStart + 11}",
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(
                        onClick = { decadeStart += 12 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Years",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3x4 Grid of 12 Years
                val yearsInDecade = (decadeStart..(decadeStart + 11)).toList()
                for (chunk in yearsInDecade.chunked(3)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (yr in chunk) {
                            val isSel = yr == selectedYear
                            val isCurr = yr == currentYear
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSel -> ElectricBlue
                                            isCurr -> Color(0xFF232A3B)
                                            else -> Color(0xFF191D28)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            isSel -> ElectricBlue
                                            isCurr -> ElectricBlue.copy(alpha = 0.5f)
                                            else -> Color(0xFF292E3D)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onYearSelected(yr) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$yr",
                                        color = if (isSel) Color.White else if (isCurr) ElectricBlue else TextPrimary,
                                        fontWeight = if (isSel || isCurr) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 15.sp
                                    )
                                    if (isCurr && !isSel) {
                                        Text(
                                            text = "Current",
                                            color = ElectricBlue.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Jump to Current Year button if not currently selected
                if (selectedYear != currentYear) {
                    Button(
                        onClick = { onYearSelected(currentYear) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232A3B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jump to This Year ($currentYear)", color = ElectricBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

