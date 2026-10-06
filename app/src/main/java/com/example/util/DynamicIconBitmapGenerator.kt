package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Generates dynamic calendar icons at runtime using Kotlin Bitmap and Canvas.
 * Caches the rendered bitmap as WebP in internal storage and persists the last generated date.
 */
object DynamicIconBitmapGenerator {
    private const val TAG = "DynamicIconGenerator"
    private const val PREFS_NAME = "dynamic_icon_prefs"
    private const val KEY_LAST_GENERATED_DATE = "last_generated_date"

    /**
     * Generates a dynamic calendar icon bitmap for the given calendar date.
     *
     * @param context Android context
     * @param calendar The date to render (defaults to current date)
     * @param size Dimension in pixels (default 512x512 for high resolution)
     * @return Generated Bitmap with dark background, date number, and day of week
     */
    fun generateIconBitmap(
        context: Context,
        calendar: Calendar = Calendar.getInstance(),
        size: Int = 512
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw rounded background (matching dark theme #1B1E26)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B1E26")
            style = Paint.Style.FILL
        }
        val cornerRadius = size * 0.22f // Modern squirclish rounded corner
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

        // 2. Draw subtle top header bar / separator
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C313E")
            style = Paint.Style.FILL
        }
        val headerHeight = size * 0.16f
        canvas.drawRoundRect(RectF(size * 0.08f, size * 0.08f, size * 0.92f, size * 0.08f + headerHeight), 16f, 16f, barPaint)

        // Month text inside the header bar
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthText = monthFormat.format(calendar.time).uppercase(Locale.getDefault())
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9AA0A6")
            textSize = size * 0.085f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val monthY = size * 0.08f + (headerHeight / 2f) - ((monthPaint.descent() + monthPaint.ascent()) / 2f)
        canvas.drawText(monthText, size / 2f, monthY, monthPaint)

        // 3. Draw date number (in bright iOS/Infinix blue #007AFF)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#007AFF")
            textSize = size * 0.44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dateY = size * 0.58f
        canvas.drawText(dayOfMonth, size / 2f, dateY, datePaint)

        // 4. Draw Day of Week (e.g. MON, TUE, WED in white #FFFFFF)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayText = dayFormat.format(calendar.time).uppercase(Locale.getDefault())
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dayY = size * 0.82f
        canvas.drawText(dayText, size / 2f, dayY, dayPaint)

        return bitmap
    }

    /**
     * Formats a unique date key for caching (YYYY-MM-DD).
     */
    fun getDateKey(calendar: Calendar = Calendar.getInstance()): String {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    /**
     * Renders and caches the dynamic calendar icon as a WebP file in the app's cache directory.
     * Persists the last generated date in SharedPreferences.
     */
    fun cacheIconAsWebp(
        context: Context,
        calendar: Calendar = Calendar.getInstance(),
        forceRegenerate: Boolean = false
    ): File? {
        val dateKey = getDateKey(calendar)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_GENERATED_DATE, null)

        val iconDir = File(context.cacheDir, "dynamic_icons").apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(iconDir, "calendar_icon_$dateKey.webp")

        if (!forceRegenerate && lastDate == dateKey && targetFile.exists() && targetFile.length() > 0) {
            Log.d(TAG, "Icon already generated and cached for $dateKey")
            return targetFile
        }

        return try {
            val bitmap = generateIconBitmap(context, calendar)
            FileOutputStream(targetFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, out)
                } else {
                    @Suppress("DEPRECATION")
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 95, out)
                }
            }
            bitmap.recycle()

            prefs.edit().putString(KEY_LAST_GENERATED_DATE, dateKey).apply()
            Log.d(TAG, "Successfully generated and cached WebP icon: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache dynamic WebP icon", e)
            null
        }
    }

    /**
     * Returns whether the icon has already been generated for today.
     */
    fun isGeneratedForToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_GENERATED_DATE, null)
        return lastDate == getDateKey()
    }
}
