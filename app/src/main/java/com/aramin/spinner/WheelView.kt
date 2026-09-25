package com.aramin.spinner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class WheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var items: MutableList<String> = mutableListOf()
        set(value) { field = value; invalidate() }

    var onFinished: ((String) -> Unit)? = null
    var onTick: (() -> Unit)? = null
    var spinning = false
        private set

    private var rotation = 0f
    private var lastTickIndex = -1
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 34f
        isFakeBoldText = true
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1D23"); strokeWidth = 3f
    }

    private val palette = intArrayOf(
        Color.parseColor("#E74C3C"), Color.parseColor("#3498DB"),
        Color.parseColor("#2ECC71"), Color.parseColor("#F39C12"),
        Color.parseColor("#9B59B6"), Color.parseColor("#1ABC9C"),
        Color.parseColor("#E67E22"), Color.parseColor("#34495E"),
        Color.parseColor("#D35400"), Color.parseColor("#16A085"),
        Color.parseColor("#C0392B"), Color.parseColor("#8E44AD")
    )

    fun spin() {
        if (spinning || items.size < 2) return
        spinning = true
        lastTickIndex = -1
        val turns = Random.nextLong(4, 8)
        val extra = Random.nextFloat() * 360f
        val target = rotation + turns * 360f + extra
        ValueAnimator.ofFloat(rotation, target).apply {
            duration = 4500L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                rotation = it.animatedValue as Float
                // tick sound when a slice passes the pointer
                val w = winnerIndex()
                if (w != lastTickIndex) { lastTickIndex = w; onTick?.invoke() }
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    spinning = false
                    rotation %= 360f
                    val winner = winnerIndex()
                    invalidate()
                    if (winner >= 0) onFinished?.invoke(items[winner])
                }
            })
            start()
        }
    }

    // Slice sitting under the pointer at the TOP (270° in canvas coords)
    private fun winnerIndex(): Int {
        if (items.isEmpty()) return -1
        val n = items.size
        val slice = 360f / n
        val a = (270f - rotation) % 360f
        var idx = (a / slice).toInt()
        idx = ((idx % n) + n) % n
        return idx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - 12f
        val n = items.size
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)

        if (n == 0) {
            paint.shader = LinearGradient(
                cx - r, cy - r, cx + r, cy + r,
                Color.parseColor("#2A2F3A"), Color.parseColor("#1A1D26"),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
            textPaint.textSize = 40f
            canvas.drawText("گزینه‌ای نیست", cx, cy, textPaint)
            return
        }

        val slice = 360f / n

        for (i in 0 until n) {
            paint.color = palette[i % palette.size]
            canvas.drawArc(rect, rotation + i * slice, slice, true, paint)
        }

        // separators + labels
        for (i in 0 until n) {
            val startA = rotation + i * slice
            val a0 = Math.toRadians(startA.toDouble())
            canvas.drawLine(cx, cy, cx + r * cos(a0).toFloat(), cy + r * sin(a0).toFloat(), linePaint)

            val midDeg = startA + slice / 2
            val mid = Math.toRadians(midDeg.toDouble())
            val lx = cx + (r * 0.62f) * cos(mid).toFloat()
            val ly = cy + (r * 0.62f) * sin(mid).toFloat()
            canvas.save()
            canvas.rotate((midDeg + 90).toFloat(), lx, ly)
            canvas.drawText(items[i].take(10), lx, ly - 10f, textPaint)
            canvas.restore()
        }

        // border ring with gold gradient feel
        paint.shader = LinearGradient(
            cx - r, cy - r, cx + r, cy + r,
            Color.parseColor("#F1C40F"), Color.parseColor("#B7950B"),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        canvas.drawCircle(cx, cy, r + 2f, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL

        // hub
        paint.color = Color.parseColor("#1A1D23")
        canvas.drawCircle(cx, cy, r * 0.11f, paint)
        paint.color = Color.parseColor("#F1C40F")
        canvas.drawCircle(cx, cy, r * 0.05f, paint)

        // winning slice highlight (same index the result uses — guaranteed aligned)
        if (!spinning) {
            val w = winnerIndex()
            if (w >= 0) {
                paint.color = Color.parseColor("#55FFFFFF")
                canvas.drawArc(rect, rotation + w * slice, slice, true, paint)
            }
        }

        // pointer: pinned to the very top edge of the view, pointing DOWN into the wheel.
        // The tip sits inside the wheel (circle top is at y = cy - r = 12+ when square).
        val tipY = (cy - r) + 46f
        paint.color = Color.parseColor("#FFD700")
        val ptr = android.graphics.Path().apply {
            moveTo(cx - 30f, 2f)
            lineTo(cx + 30f, 2f)
            lineTo(cx, tipY)
            close()
        }
        canvas.drawPath(ptr, paint)
        paint.color = Color.parseColor("#7D6608")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawPath(ptr, paint)
        paint.style = Paint.Style.FILL
    }
}
