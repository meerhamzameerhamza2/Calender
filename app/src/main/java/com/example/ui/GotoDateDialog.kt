package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

/**
 * Touch phone friendly "Go to" date picker dialog.
 * Replaces arrow buttons with smooth vertical touch swiping wheel pickers.
 */
@Composable
fun GotoDateDialog(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"
    )

    val minYear = 1970
    val maxYear = 2050
    val yearList = remember { (minYear..maxYear).map { it.toString() } }

    val maxDaysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val daysList = remember(maxDaysInMonth) {
        (1..maxDaysInMonth).map { it.toString() }
    }

    if (selectedDay > maxDaysInMonth) {
        selectedDay = maxDaysInMonth
    }

    val selectedMonthIndex = (selectedMonth - 1).coerceIn(0, 11)
    val selectedDayIndex = (selectedDay - 1).coerceIn(0, maxDaysInMonth - 1)
    val selectedYearIndex = (selectedYear - minYear).coerceIn(0, yearList.size - 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1E27),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Go to",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goto_dialog_title")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF222631).copy(alpha = 0.85f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wheel View Area with Mathematically Centered Highlight Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Selection Highlight Capsule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C3240).copy(alpha = 0.70f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    )

                    // 3 Equal-width Columns (Month, Day, Year)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Touch Wheel (Centered)
                        WheelColumn(
                            items = monthNames,
                            initialIndex = selectedMonthIndex,
                            label = null,
                            modifier = Modifier.weight(1f),
                            onItemSelected = { idx ->
                                selectedMonth = idx + 1
                            }
                        )

                        // Day Touch Wheel (Centered)
                        WheelColumn(
                            items = daysList,
                            initialIndex = selectedDayIndex,
                            label = null,
                            modifier = Modifier.weight(1f),
                            onItemSelected = { idx ->
                                selectedDay = idx + 1
                            }
                        )

                        // Year Touch Wheel (Centered)
                        WheelColumn(
                            items = yearList,
                            initialIndex = selectedYearIndex,
                            label = null,
                            modifier = Modifier.weight(1f),
                            onItemSelected = { idx ->
                                selectedYear = minYear + idx
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Centered Labels Row (Month, Day, Year)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Month",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Day",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Year",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedYear, selectedMonth, selectedDay)
                    onDismiss()
                },
                modifier = Modifier.testTag("goto_confirm_button")
            ) {
                Text(text = "Confirm", color = ElectricBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("goto_cancel_button")
            ) {
                Text(text = "Cancel", color = TextSecondary, fontSize = 16.sp)
            }
        }
    )
}
