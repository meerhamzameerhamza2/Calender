package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextSecondary

/**
 * Animated Empty State illustration exactly matching Image 4:
 * A subtle curved dome/hill horizon with soft top glow, topped by
 * a stylized easel desk calendar with spiral binder rings and soft checkmark,
 * followed by "No events" caption.
 */
@Composable
fun AnimatedEmptyCalendarCheck(
    modifier: Modifier = Modifier,
    title: String = "No events",
    subtitle: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_calendar_anim")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
            .testTag("animated_empty_calendar_check"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Custom Canvas drawing the exact Image 4 curved dome hill and desk calendar with checkmark
        Canvas(
            modifier = Modifier
                .size(width = 180.dp, height = 110.dp)
        ) {
            val width = size.width
            val height = size.height

            // --- 1. Curved Dome / Hill with Subtle Glow ---
            val hillPath = Path().apply {
                moveTo(10f, height)
                quadraticBezierTo(
                    width * 0.5f, height * 0.50f, // crest of the hill
                    width - 10f, height
                )
                close()
            }

            // Dome gradient fill
            val hillGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF222632).copy(alpha = glowAlpha),
                    Color(0xFF14161D).copy(alpha = 0.8f),
                    Color(0xFF000000).copy(alpha = 0f)
                ),
                startY = height * 0.50f,
                endY = height
            )
            drawPath(path = hillPath, brush = hillGradient)

            // Dome top crest rim stroke
            val crestPath = Path().apply {
                moveTo(15f, height * 0.95f)
                quadraticBezierTo(
                    width * 0.5f, height * 0.50f,
                    width - 15f, height * 0.95f
                )
            }
            drawPath(
                path = crestPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF4A5568).copy(alpha = glowAlpha + 0.2f),
                        Color(0xFF64748B).copy(alpha = glowAlpha + 0.35f),
                        Color(0xFF4A5568).copy(alpha = glowAlpha + 0.2f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 1.8f, cap = StrokeCap.Round)
            )

            // --- 2. Desk Calendar Pad (rested on the crest) ---
            val calWidth = 68.dp.toPx()
            val calHeight = 52.dp.toPx()
            val calLeft = (width - calWidth) / 2f
            val calTop = height * 0.40f - calHeight / 2f + floatOffset

            // Background second sheet / shadow layer (curled/offset)
            drawRoundRect(
                color = Color(0xFF1F232D),
                topLeft = Offset(calLeft + 3.dp.toPx(), calTop + 3.dp.toPx()),
                size = Size(calWidth, calHeight),
                cornerRadius = CornerRadius(10.dp.toPx())
            )

            // Main calendar slate sheet
            val sheetGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF333846),
                    Color(0xFF262A35)
                ),
                startY = calTop,
                endY = calTop + calHeight
            )
            drawRoundRect(
                brush = sheetGradient,
                topLeft = Offset(calLeft, calTop),
                size = Size(calWidth, calHeight),
                cornerRadius = CornerRadius(10.dp.toPx())
            )

            // Subtle border around calendar sheet
            drawRoundRect(
                color = Color(0xFF474E61),
                topLeft = Offset(calLeft, calTop),
                size = Size(calWidth, calHeight),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // --- 3. Top Spiral Binder Rings ---
            val ringCount = 4
            val ringSpacing = calWidth / (ringCount + 1)
            val ringWidth = 3.5.dp.toPx()
            val ringHeight = 7.dp.toPx()

            for (i in 1..ringCount) {
                val rx = calLeft + i * ringSpacing - ringWidth / 2f
                val ry = calTop - ringHeight * 0.45f
                drawRoundRect(
                    color = Color(0xFF5E6679),
                    topLeft = Offset(rx, ry),
                    size = Size(ringWidth, ringHeight),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }

            // --- 4. Embossed Checkmark on the Calendar Sheet ---
            val checkPath = Path().apply {
                val cx = calLeft + calWidth * 0.32f
                val cy = calTop + calHeight * 0.54f
                moveTo(cx, cy)
                lineTo(cx + calWidth * 0.14f, cy + calHeight * 0.16f)
                lineTo(cx + calWidth * 0.40f, cy - calHeight * 0.16f)
            }

            // Shadow / indented layer of checkmark
            drawPath(
                path = checkPath,
                color = Color(0xFF191C24),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Soft stroke of checkmark
            drawPath(
                path = checkPath,
                color = Color(0xFF2A2E3B),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "No events" Caption
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 15.sp
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = com.example.ui.theme.TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}
