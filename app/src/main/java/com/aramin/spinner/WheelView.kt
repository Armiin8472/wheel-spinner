package com.aramin.spinner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import org.json.JSONArray
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
    var spinning = false
        private set

    private var rotation = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 34f
        isFakeBoldText = true
    }

    private val palette = intArrayOf(
        Color.parseColor("#E74C3C"), Color.parseColor("#3498DB"),
        Color.parseColor("#2ECC71"), Color.parseColor("#F39C12"),
        Color.parseColor("#9B59B6"), Color.parseColor("#1ABC9C"),
        Color.parseColor("#E67E22"), Color.parseColor("#34495E"),
        Color.parseColor("#D35400"), Color.parseColor("#16A085")
    )

    fun spin() {
        if (spinning || items.size < 2) return
        spinning = true
        val turns = Random.nextLong(4, 8)
        val extra = Random.nextFloat() * 360f
        val target = rotation + turns * 360f + extra
        ValueAnimator.ofFloat(rotation, target).apply {
            duration = 4200L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                rotation = it.animatedValue as Float
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

    // Slice under the pointer at the TOP (270° in canvas coords)
    private fun winnerIndex(): Int {
        if (items.isEmpty()) return -1
        val n = items.size
        val slice = 360f / n
        val angle = (-rotation - 270f) % 360f
        var idx = (angle / slice).toInt()
        idx = ((idx % n) + n) % n
        return idx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - 12f
        val n = items.size

        if (n == 0) {
            paint.color = Color.parseColor("#22262E")
            canvas.drawCircle(cx, cy, r, paint)
            textPaint.textSize = 40f
            canvas.drawText("گزینه‌ای نیست", cx, cy, textPaint)
            return
        }

        val slice = 360f / n
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)

        for (i in 0 until n) {
            paint.color = palette[i % palette.size]
            val start = rotation + i * slice
            canvas.drawArc(rect, start, slice, true, paint)
        }

        // separators + labels
        for (i in 0 until n) {
            val mid = Math.toRadians((rotation + i * slice + slice / 2).toDouble())
            val lx = cx + (r * 0.62f) * cos(mid).toFloat()
            val ly = cy + (r * 0.62f) * sin(mid).toFloat()
            canvas.save()
            canvas.rotate((rotation + i * slice + slice / 2 + 90).toFloat(), lx, ly)
            val label = items[i].take(10)
            canvas.drawText(label, lx, ly, textPaint)
            canvas.restore()

            paint.color = Color.parseColor("#1A1D23")
            val a = Math.toRadians((rotation + i * slice).toDouble())
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A1D23"); strokeWidth = 3f
            }
            canvas.drawLine(cx, cy, cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat(), linePaint)
        }

        // border + hub
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.parseColor("#1A1D23")
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1A1D23")
        canvas.drawCircle(cx, cy, r * 0.12f, paint)

        // highlight winning slice when stopped
        if (!spinning) {
            val w = winnerIndex()
            if (w >= 0) {
                paint.color = Color.parseColor("#66FFD700")
                canvas.drawArc(rect, rotation + w * slice, slice, true, paint)
            }
        }

        // pointer at top
        paint.color = Color.parseColor("#FFD700")
        val p = Path().apply {
            moveTo(cx - 26f, 0f)
            lineTo(cx + 26f, 0f)
            lineTo(cx, 52f)
            close()
        }
        canvas.drawPath(p, paint)
    }
}
