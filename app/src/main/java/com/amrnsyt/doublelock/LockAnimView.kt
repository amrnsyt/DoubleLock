package com.amrnsyt.doublelock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.hypot
import kotlin.math.max

/**
 * Full-screen overlay that draws the "screen off" animation.
 * progress goes from 0 (screen untouched) to 1 (fully black).
 * tapX / tapY below zero means "use the screen center".
 */
class LockAnimView(
    context: Context,
    private val animation: LockAnimation,
    private val tapX: Float,
    private val tapY: Float
) : View(context) {

    var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val path = Path()
    private val lineThickness = 4f * resources.displayMetrics.density

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        when (animation) {
            LockAnimation.NONE -> Unit
            LockAnimation.FADE ->
                canvas.drawColor(Color.argb((easeInOut(progress) * 255f).toInt(), 0, 0, 0))
            LockAnimation.CURTAIN ->
                canvas.drawRect(0f, 0f, w, h * easeInOut(progress), black)
            LockAnimation.CIRCLE -> drawCircle(canvas, w, h)
            LockAnimation.CRT -> drawCrt(canvas, w, h)
        }
    }

    private fun drawCircle(canvas: Canvas, w: Float, h: Float) {
        val cx = if (tapX >= 0f) tapX else w / 2f
        val cy = if (tapY >= 0f) tapY else h / 2f
        val maxRadius = hypot(max(cx, w - cx), max(cy, h - cy))
        val radius = maxRadius * (1f - easeInOut(progress))
        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.addRect(0f, 0f, w, h, Path.Direction.CW)
        path.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.drawPath(path, black)
    }

    private fun drawCrt(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val half = lineThickness / 2f
        val p1 = (progress / 0.55f).coerceIn(0f, 1f)
        val p2 = ((progress - 0.55f) / 0.35f).coerceIn(0f, 1f)
        val p3 = ((progress - 0.9f) / 0.1f).coerceIn(0f, 1f)

        if (p3 > 0f) {
            canvas.drawColor(Color.BLACK)
            white.alpha = ((1f - p3) * 255f).toInt()
            canvas.drawRect(cx - half, cy - half, cx + half, cy + half, white)
            return
        }

        val holeW = lerp(w, lineThickness, easeInOut(p2))
        val holeH = lerp(h, lineThickness, easeIn(p1))
        val left = cx - holeW / 2f
        val right = cx + holeW / 2f
        val top = cy - holeH / 2f
        val bottom = cy + holeH / 2f

        path.reset()
        path.fillType = Path.FillType.EVEN_ODD
        path.addRect(0f, 0f, w, h, Path.Direction.CW)
        path.addRect(left, top, right, bottom, Path.Direction.CW)
        canvas.drawPath(path, black)

        white.alpha = if (p2 > 0f) 255 else (p1 * p1 * 0.9f * 255f).toInt()
        canvas.drawRect(left, top, right, bottom, white)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun easeIn(t: Float) = t * t
    private fun easeInOut(t: Float) = t * t * (3f - 2f * t)
}
