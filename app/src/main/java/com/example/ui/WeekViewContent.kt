package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EventEntity
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ImportantPink
import com.example.ui.theme.SundayRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WeekViewContent(
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    events: List<EventEntity>,
    onDaySelect: (Int, Int, Int) -> Unit,
    onAddEventClick: () -> Unit,
    onEventClick: (EventEntity) -> Unit,
    onPrevWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Determine the 7 days of the currently selected week
    val weekDays = remember(selectedYear, selectedMonth, selectedDay) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, selectedDay)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val list = mutableListOf<Triple<Int, Int, Int>>() // Year, Month, Day
        for (i in 0 until 7) {
            list.add(
                Triple(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal Week Strip Card
            var weekStripDragTotal by remember { mutableFloatStateOf(0f) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceCard)
                    .pointerInput(selectedYear, selectedMonth, selectedDay) {
                        detectHorizontalDragGestures(
                            onDragStart = { weekStripDragTotal = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                weekStripDragTotal += dragAmount
                            },
                            onDragEnd = {
                                if (weekStripDragTotal < -45f) {
                                    onNextWeek()
                                } else if (weekStripDragTotal > 45f) {
                                    onPrevWeek()
                                }
                                weekStripDragTotal = 0f
                            },
                            onDragCancel = {
                                weekStripDragTotal = 0f
                            }
                        )
                    }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEachIndexed { index, (y, m, d) ->
                    val isToday = (y == todayYear && m == todayMonth && d == todayDay)
                    val isSelected = (y == selectedYear && m == selectedMonth && d == selectedDay)
                    val isSunday = (index == 0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected && !isToday) DarkSurfaceElevated else Color.Transparent)
                            .clickable { onDaySelect(y, m, d) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("week_day_${d}_$m")
                    ) {
                        Text(
                            text = dayNames[index],
                            color = if (isSunday) SundayRed else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(32.dp)
                        ) {
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue)
                                )
                            }
                            Text(
                                text = "$d",
                                color = if (isToday) Color.White else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Hourly Timeline
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp)
            ) {
                items(24) { hour ->
                    val hourString = String.format(Locale.getDefault(), "%02d:00", hour)

                    // Find events on selected day in this hour
                    val hourEvents = events.filter { ev ->
                        val cal = Calendar.getInstance().apply { timeInMillis = ev.startTimestamp }
                        cal.get(Calendar.YEAR) == selectedYear &&
                        cal.get(Calendar.MONTH) + 1 == selectedMonth &&
                        cal.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                        cal.get(Calendar.HOUR_OF_DAY) == hour
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = hourString,
                            color = TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.width(48.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(DarkBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (hourEvents.isNotEmpty()) {
                                hourEvents.forEach { ev ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (ev.isImportantDay) ImportantPink.copy(alpha = 0.25f)
                                                else ElectricBlue.copy(alpha = 0.25f)
                                            )
                                            .clickable { onEventClick(ev) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp, 16.dp)
                                                .background(
                                                    if (ev.isImportantDay) ImportantPink else ElectricBlue,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = ev.title,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
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

        // FAB (+)
        FloatingActionButton(
            onClick = onAddEventClick,
            containerColor = ElectricBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .testTag("week_add_event_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Schedule",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
