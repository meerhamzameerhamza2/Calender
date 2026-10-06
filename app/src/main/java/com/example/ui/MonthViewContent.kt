package com.example.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HijriCalendarHelper
import com.example.data.HinduCalendarHelper
import com.example.model.CalendarDayModel
import com.example.model.CalendarSettings
import com.example.model.EventEntity
import com.example.model.Holiday
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
import java.util.Date
import java.util.Locale

@Composable
fun MonthViewContent(
    viewYear: Int,
    viewMonth: Int,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    days: List<CalendarDayModel>,
    daysForSelectedWeek: List<CalendarDayModel> = emptyList(),
    selectedSummary: String,
    selectedEvents: List<EventEntity>,
    selectedHoliday: Holiday?,
    monthHolidays: List<Holiday> = emptyList(),
    monthEvents: List<EventEntity> = emptyList(),
    settings: CalendarSettings,
    onDayClick: (Int, Int, Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onGoToToday: () -> Unit,
    onAddEventClick: () -> Unit,
    onEventClick: (EventEntity) -> Unit,
    onHolidayClick: ((Holiday) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val monthNames = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Subtitle string for other calendars (Hijri / Hindu)
    val monthSubtitle = remember(viewYear, viewMonth, settings.otherCalendar, settings.ramadanAdjustment) {
        when (settings.otherCalendar) {
            "Muslim Calendar" -> {
                val hijri = HijriCalendarHelper.gregorianToHijri(viewYear, viewMonth, 15, settings.ramadanAdjustment)
                "${hijri.monthName} ${hijri.year}"
            }
            "Hindu Calendar" -> {
                val panchang = HinduCalendarHelper.getHinduPanchang(viewYear, viewMonth, 15)
                "${panchang.monthName} • Samvat ${panchang.vikramSamvat}"
            }
            else -> ""
        }
    }

    val weekDays = if (settings.startWeekOn == "Monday") {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    } else {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }

    var isCalendarCollapsed by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Dragging upwards (scrolling through events) collapses the calendar to 1-week row
                if (available.y < -12f && !isCalendarCollapsed) {
                    isCalendarCollapsed = true
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Dragging downwards at top of list expands back to full month
                if (available.y > 15f && isCalendarCollapsed && lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset <= 5) {
                    isCalendarCollapsed = false
                }
                return Offset.Zero
            }
        }
    }

    val monthRows = remember(days) { days.chunked(7) }
    val visibleRows = if (isCalendarCollapsed) {
        if (daysForSelectedWeek.isNotEmpty()) {
            listOf(daysForSelectedWeek)
        } else {
            val selectedWeekRow = monthRows.find { week ->
                week.any { it.dayNumber == selectedDay }
            } ?: monthRows.firstOrNull() ?: emptyList()
            listOf(selectedWeekRow)
        }
    } else {
        monthRows
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // 1. Month & Year Navigation Header (Clickable to toggle collapse/expand)
            item {
                var headerDragTotal by remember { mutableFloatStateOf(0f) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .pointerInput(isCalendarCollapsed, viewYear, viewMonth, selectedYear, selectedMonth, selectedDay) {
                            detectHorizontalDragGestures(
                                onDragStart = { headerDragTotal = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    headerDragTotal += dragAmount
                                },
                                onDragEnd = {
                                    if (headerDragTotal < -45f) {
                                        if (isCalendarCollapsed) onNextWeek() else onNextMonth()
                                    } else if (headerDragTotal > 45f) {
                                        if (isCalendarCollapsed) onPrevWeek() else onPrevMonth()
                                    }
                                    headerDragTotal = 0f
                                },
                                onDragCancel = {
                                    headerDragTotal = 0f
                                }
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCalendarCollapsed = !isCalendarCollapsed }
                    ) {
                        Text(
                            text = "${monthNames[viewMonth]} $viewYear",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (monthSubtitle.isNotEmpty()) {
                            Text(
                                text = monthSubtitle,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    // Month Navigation Arrows
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isCalendarCollapsed) onPrevWeek() else onPrevMonth()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("month_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = if (isCalendarCollapsed) "Previous Week" else "Previous Month",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (isCalendarCollapsed) onNextWeek() else onNextMonth()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("month_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isCalendarCollapsed) "Next Week" else "Next Month",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 2. Weekday Header Row (Sun in red, others in subtle text)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekDays.forEach { dayName ->
                        val isSunday = dayName == "Sun"
                        Text(
                            text = dayName,
                            color = if (isSunday) SundayRed else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Dynamic Month Grid / Collapsible 1-Week Row (With iOS-like animation & drag handle)
            item {
                var gridDragTotal by remember { mutableFloatStateOf(0f) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .pointerInput(isCalendarCollapsed, viewYear, viewMonth, selectedYear, selectedMonth, selectedDay) {
                            detectHorizontalDragGestures(
                                onDragStart = { gridDragTotal = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    gridDragTotal += dragAmount
                                },
                                onDragEnd = {
                                    if (gridDragTotal < -45f) {
                                        if (isCalendarCollapsed) {
                                            onNextWeek()
                                        } else {
                                            onNextMonth()
                                        }
                                    } else if (gridDragTotal > 45f) {
                                        if (isCalendarCollapsed) {
                                            onPrevWeek()
                                        } else {
                                            onPrevMonth()
                                        }
                                    }
                                    gridDragTotal = 0f
                                },
                                onDragCancel = {
                                    gridDragTotal = 0f
                                }
                            )
                        }
                        .animateContentSize(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            )
                        )
                ) {
                    visibleRows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayModel ->
                                MonthDayCell(
                                    dayModel = dayModel,
                                    onClick = {
                                        if (dayModel.dayNumber > 0) {
                                            onDayClick(dayModel.year, dayModel.month, dayModel.dayNumber)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Subtle iOS translucent drag handle pill to toggle collapse/expand
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isCalendarCollapsed = !isCalendarCollapsed }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                }
            }

            // 4. Selected Date Summary Header (Image 4 format: "Fri, 11 Sept, today")
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedSummary,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (selectedHoliday != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(HolidayAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = selectedHoliday.category,
                                color = HolidayAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 5. Selected Day Holiday & Events List (Translucent iOS style)
            if (selectedHoliday != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF23201D).copy(alpha = 0.85f))
                            .border(BorderStroke(1.dp, HolidayAmber.copy(alpha = 0.25f)), RoundedCornerShape(16.dp))
                            .clickable { onHolidayClick?.invoke(selectedHoliday) }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(HolidayAmber.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = HolidayAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedHoliday.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${selectedHoliday.category} • India",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            if (selectedEvents.isNotEmpty()) {
                items(selectedEvents) { event ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        EventRowItem(event = event, onClick = { onEventClick(event) })
                    }
                }
            } else if (selectedHoliday == null) {
                // Animated Empty State when no events & no holiday on selected date
                item {
                    Spacer(modifier = Modifier.height(if (isCalendarCollapsed) 48.dp else 14.dp))
                    AnimatedEmptyCalendarCheck(
                        title = "No events"
                    )
                }
            }

            // Bottom scroll clearance so list items scroll above the floating buttons
            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // 7. Bottom Action Controls (Translucent frosted blur dock: "Today" pill on left, "(+)" on right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x1012141A),
                            Color(0x3512141A)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frosted Pill Button: "Today" with opacity and blur glass effect
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF242C3B).copy(alpha = 0.70f))
                        .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)), RoundedCornerShape(26.dp))
                        .clickable { onGoToToday() }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .testTag("today_pill_button")
                ) {
                    Text(
                        text = "Today",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Frosted Circular Plus Button: "(+)" with opacity and blur glass effect
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF242C3B).copy(alpha = 0.70f))
                        .border(BorderStroke(1.2.dp, Color.White.copy(alpha = 0.22f)), CircleShape)
                        .clickable { onAddEventClick() }
                        .testTag("month_add_event_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Event",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Month Day Cell with tight spacing, 42dp selection circle fitting both primary & secondary date,
 * and completely blank placeholder for non-month days.
 */
@Composable
fun MonthDayCell(
    dayModel: CalendarDayModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // If day is an empty placeholder (dayNumber <= 0), render an empty cell
    if (dayModel.dayNumber <= 0) {
        Box(
            modifier = modifier
                .height(48.dp)
        )
        return
    }

    val isSunday = remember(dayModel.year, dayModel.month, dayModel.dayNumber) {
        val cal = java.util.Calendar.getInstance().apply {
            set(dayModel.year, dayModel.month - 1, dayModel.dayNumber)
        }
        cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() }
            .testTag("day_cell_${dayModel.dayNumber}_${dayModel.month}")
    ) {
        // Selected Date: Solid blue circle (approx 40-42dp) containing both numbers cleanly (Image 4)
        if (dayModel.isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${dayModel.dayNumber}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                    if (dayModel.subtitleText.isNotEmpty()) {
                        Text(
                            text = dayModel.subtitleText,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        } else {
            // Non-selected Date: primary date, tight minimal gap, secondary date, and optional indicator dot
            val textColor = when {
                !dayModel.isCurrentMonth -> if (isSunday) SundayRed.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.38f)
                isSunday -> SundayRed
                else -> Color.White
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${dayModel.dayNumber}",
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp
                )

                if (dayModel.subtitleText.isNotEmpty()) {
                    Text(
                        text = dayModel.subtitleText,
                        color = if (!dayModel.isCurrentMonth) Color(0xFF8E929E).copy(alpha = 0.5f) else Color(0xFF8E929E),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 10.sp
                    )
                }

                // Dot indicator for Holiday or Event
                if (dayModel.holiday != null || dayModel.hasEvents) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (dayModel.holiday != null) HolidayAmber
                                else if (dayModel.hasImportantDay) ImportantPink
                                else ElectricBlue
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun EventRowItem(
    event: EventEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeStr = if (event.isAllDay) {
        "All-day"
    } else {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        "${fmt.format(Date(event.startTimestamp))} - ${fmt.format(Date(event.endTimestamp))}"
    }

    val stripeColor = if (event.isImportantDay) ImportantPink else ElectricBlue

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1F232D).copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(stripeColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.isImportantDay) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Important",
                        tint = ImportantPink,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = timeStr,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                if (!event.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = event.location,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
