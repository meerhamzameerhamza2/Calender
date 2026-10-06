package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SundayRed
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun YearViewContent(
    currentYear: Int,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    onYearChange: (Int) -> Unit,
    onMonthSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Year Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onYearChange(currentYear - 1) }, modifier = Modifier.testTag("year_prev_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Year",
                    tint = TextSecondary
                )
            }

            Text(
                text = "$currentYear",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("year_title")
            )

            IconButton(onClick = { onYearChange(currentYear + 1) }, modifier = Modifier.testTag("year_next_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Year",
                    tint = TextSecondary
                )
            }
        }

        // 12 Mini-Months Grid (3 columns x 4 rows)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp)
        ) {
            items(12) { index ->
                val monthNumber = index + 1
                MiniMonthCard(
                    year = currentYear,
                    month = monthNumber,
                    monthName = monthNames[index],
                    todayYear = todayYear,
                    todayMonth = todayMonth,
                    todayDay = todayDay,
                    onMonthClick = { onMonthSelect(monthNumber) }
                )
            }
        }
    }
}

@Composable
fun MiniMonthCard(
    year: Int,
    month: Int,
    monthName: String,
    todayYear: Int,
    todayMonth: Int,
    todayDay: Int,
    onMonthClick: () -> Unit
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday is 0

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceCard)
            .clickable { onMonthClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = monthName,
            color = if (year == todayYear && month == todayMonth) ElectricBlue else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Day of week header: S M T W T F S
        val letters = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            letters.forEachIndexed { idx, letter ->
                Text(
                    text = letter,
                    color = if (idx == 0) SundayRed else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Mini Day Grid: dynamic rows (5 or 6 rows as needed so no days are missing)
        val numRows = ((startDayOfWeek + daysInMonth + 6) / 7).coerceAtLeast(5)
        Column {
            for (row in 0 until numRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startDayOfWeek + 1
                        if (dayNumber in 1..daysInMonth) {
                            val isToday = (year == todayYear && month == todayMonth && dayNumber == todayDay)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(15.dp)
                                    .then(
                                        if (isToday) {
                                            Modifier
                                                .clip(CircleShape)
                                                .background(ElectricBlue)
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                Text(
                                    text = "$dayNumber",
                                    color = when {
                                        isToday -> Color.White
                                        col == 0 -> SundayRed
                                        else -> TextPrimary
                                    },
                                    fontSize = 7.5.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeight = 9.sp
                                    )
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}
