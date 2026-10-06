package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DateDrumItem(
    val year: Int,
    val month: Int,
    val day: Int,
    val label: String
)

/**
 * Wheel / Drum Date & Time Picker:
 * - Independent isolated drum columns for Date, Hour, Minute, and AM/PM.
 * - Forces Left-to-Right (LTR) direction so hours and minutes are never swapped or mirrored regardless of device locale.
 * - Accurate geometric viewport center detection so scrolling hours strictly changes hours, and scrolling minutes strictly changes minutes.
 * - Includes column labels and clean vertical drum scrolling.
 */
@Composable
fun WheelDateTimePicker(
    timestamp: Long,
    isAllDay: Boolean,
    onTimestampChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTimestamp by rememberUpdatedState(timestamp)
    val currentOnTimestampChange by rememberUpdatedState(onTimestampChange)

    val currentCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMonth = currentCal.get(Calendar.MONTH)
    val currentDay = currentCal.get(Calendar.DAY_OF_MONTH)
    val currentHour12 = currentCal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val currentMinute = currentCal.get(Calendar.MINUTE)
    val currentIsPm = currentCal.get(Calendar.AM_PM) == Calendar.PM

    fun updateDate(item: DateDrumItem) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
            set(Calendar.YEAR, item.year)
            set(Calendar.MONTH, item.month)
            set(Calendar.DAY_OF_MONTH, item.day)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newTs = cal.timeInMillis
        if (newTs != currentTimestamp) {
            currentOnTimestampChange(newTs)
        }
    }

    fun updateHour(chosenHour12: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
            val isPm = get(Calendar.AM_PM) == Calendar.PM
            val h24 = if (isPm) {
                if (chosenHour12 == 12) 12 else chosenHour12 + 12
            } else {
                if (chosenHour12 == 12) 0 else chosenHour12
            }
            set(Calendar.HOUR_OF_DAY, h24)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newTs = cal.timeInMillis
        if (newTs != currentTimestamp) {
            currentOnTimestampChange(newTs)
        }
    }

    fun updateMinute(chosenMinute: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
            set(Calendar.MINUTE, chosenMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newTs = cal.timeInMillis
        if (newTs != currentTimestamp) {
            currentOnTimestampChange(newTs)
        }
    }

    fun updateAmPm(willBePm: Boolean) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentTimestamp
            val h12 = get(Calendar.HOUR).let { if (it == 0) 12 else it }
            val h24 = if (willBePm) {
                if (h12 == 12) 12 else h12 + 12
            } else {
                if (h12 == 12) 0 else h12
            }
            set(Calendar.HOUR_OF_DAY, h24)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val newTs = cal.timeInMillis
        if (newTs != currentTimestamp) {
            currentOnTimestampChange(newTs)
        }
    }

    // Anchor date range once when picker opens (±180 days around initial date)
    val anchorCal = remember {
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val dateItems = remember {
        val list = ArrayList<DateDrumItem>(365)
        val tempCal = Calendar.getInstance().apply {
            timeInMillis = anchorCal.timeInMillis
            add(Calendar.DAY_OF_YEAR, -180)
        }
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        for (i in 0..360) {
            val yr = tempCal.get(Calendar.YEAR)
            val mo = tempCal.get(Calendar.MONTH)
            val dy = tempCal.get(Calendar.DAY_OF_MONTH)
            val label = dateFormat.format(tempCal.time)
            list.add(DateDrumItem(yr, mo, dy, label))
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val hours = remember { (1..12).toList() }
    val minutes = remember { (0..59).map { String.format(Locale.US, "%02d", it) } }
    val amPmList = remember { listOf("am", "pm") }

    val selectedDateIndex = remember(currentYear, currentMonth, currentDay) {
        val idx = dateItems.indexOfFirst {
            it.year == currentYear && it.month == currentMonth && it.day == currentDay
        }
        if (idx >= 0) idx else 180
    }

    val selectedHourIndex = remember(currentHour12) { hours.indexOf(currentHour12).coerceAtLeast(0) }
    val selectedMinuteIndex = remember(currentMinute) { currentMinute.coerceIn(0, 59) }
    val selectedAmPmIndex = remember(currentIsPm) { if (currentIsPm) 1 else 0 }

    // Enforce LTR layout so columns are never mirrored regardless of language/locale
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1E222D).copy(alpha = 0.95f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            // DRUM WHEEL PICKER CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Center Selection highlight bar behind the center row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2C3240).copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: Date (e.g. Fri, Sep 11)
                    WheelColumn(
                        items = dateItems.map { it.label },
                        initialIndex = selectedDateIndex,
                        modifier = Modifier.weight(if (isAllDay) 1f else 1.5f),
                        selectedFontSize = 16.sp,
                        label = "Date",
                        onItemSelected = { index ->
                            if (index in dateItems.indices) {
                                val item = dateItems[index]
                                updateDate(item)
                            }
                        }
                    )

                    if (!isAllDay) {
                        // Column 2: Hour (1..12) - STRICTLY HOURS
                        WheelColumn(
                            items = hours.map { it.toString() },
                            initialIndex = selectedHourIndex,
                            modifier = Modifier.weight(0.85f),
                            selectedFontSize = 19.sp,
                            label = "Hour",
                            onItemSelected = { index ->
                                if (index in hours.indices) {
                                    val chosenHour12 = hours[index]
                                    updateHour(chosenHour12)
                                }
                            }
                        )

                        // Colon separator between Hour and Minute
                        Text(
                            text = ":",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        // Column 3: Minute (00..59) - STRICTLY MINUTES
                        WheelColumn(
                            items = minutes,
                            initialIndex = selectedMinuteIndex,
                            modifier = Modifier.weight(0.85f),
                            selectedFontSize = 19.sp,
                            label = "Min",
                            onItemSelected = { index ->
                                if (index in minutes.indices) {
                                    updateMinute(index)
                                }
                            }
                        )

                        // Column 4: AM / PM
                        WheelColumn(
                            items = amPmList,
                            initialIndex = selectedAmPmIndex,
                            modifier = Modifier.weight(0.8f),
                            selectedFontSize = 17.sp,
                            label = "AM/PM",
                            onItemSelected = { index ->
                                if (index in amPmList.indices) {
                                    val willBePm = (index == 1)
                                    updateAmPm(willBePm)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Isolated Wheel Drum Column:
 * - Uses precise geometric viewport center calculation so item centered in the highlight bar is accurately reported.
 * - Dispatches onItemSelected only when the user interacted and the scroll has settled.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 40.dp,
    selectedFontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    label: String? = null,
    onItemSelected: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val currentOnItemSelected by rememberUpdatedState(onItemSelected)
    val safeInitial = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitial
    )
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    var userInteracted by remember { mutableStateOf(false) }

    fun calculateCenterItemIndex(): Int {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return safeInitial
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val centerItem = visibleItems.minByOrNull { item ->
            val itemCenter = item.offset + item.size / 2
            kotlin.math.abs(itemCenter - viewportCenter)
        }
        return centerItem?.index?.coerceIn(0, (items.size - 1).coerceAtLeast(0)) ?: safeInitial
    }

    // Flag user drag interaction
    LaunchedEffect(isDragged) {
        if (isDragged) {
            userInteracted = true
        }
    }

    // Sync state when initialIndex changes externally (and user is not currently dragging)
    LaunchedEffect(initialIndex) {
        if (!listState.isScrollInProgress && !isDragged) {
            val safeTarget = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
            val currentCenter = calculateCenterItemIndex()
            if (currentCenter != safeTarget) {
                listState.scrollToItem(safeTarget)
            }
        }
    }

    // Dispatches onItemSelected ONLY when user actually dragged and scroll settled
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                if (userInteracted) {
                    userInteracted = false
                    val centerIndex = calculateCenterItemIndex()
                    currentOnItemSelected(centerIndex)
                }
            }
    }

    val selectedIndex by remember {
        derivedStateOf {
            calculateCenterItemIndex()
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 5),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight * 2),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(items) { index, itemText ->
                    val distance = kotlin.math.abs(index - selectedIndex)
                    val (fontSize, color, weight) = when (distance) {
                        0 -> Triple(selectedFontSize, Color.White, FontWeight.Bold)
                        1 -> Triple(selectedFontSize * 0.82f, TextSecondary.copy(alpha = 0.8f), FontWeight.Medium)
                        else -> Triple(selectedFontSize * 0.68f, TextTertiary.copy(alpha = 0.35f), FontWeight.Normal)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .clickable {
                                userInteracted = false
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                    currentOnItemSelected(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = itemText,
                            color = color,
                            fontSize = fontSize,
                            fontWeight = weight,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Top & bottom fading gradients for smooth wheel depth
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(itemHeight * 1.4f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E222D), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(itemHeight * 1.4f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF1E222D))
                        )
                    )
            )
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
