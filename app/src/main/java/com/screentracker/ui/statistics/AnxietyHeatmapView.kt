package com.screentracker.ui.statistics

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import java.text.SimpleDateFormat
import java.util.*

/**
 * 焦虑热力图自定义View
 * 方块碎裂效果：击碎焦虑，白色方块不消除
 */
class AnxietyHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: Map<String, Int> = emptyMap()
    private var startDate: Calendar = Calendar.getInstance()
    private var endDate: Calendar = Calendar.getInstance()

    private val CELL_GAP = 4f
    private val LABEL_WIDTH = 36f
    private val MONTH_LABEL_H = 18f
    private val BAR_H = 16f
    private val BAR_MARGIN_TOP = 12f
    private val DAYS = 90

    private var cellSize = 0f
    private var cornerRadius = 2f

    private fun sp(sp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var onCellClickListener: ((dateStr: String) -> Unit)? = null
    private val cellRects = mutableListOf<Pair<RectF, String>>()

    // =================== 格子数据 ===================
    private val gridCells = mutableListOf<GridCell>()
    private var hitOrder = listOf<Int>() // 击碎顺序（只包含非0格子）

    // =================== 动画状态 ===================
    private var currentHitIndex = 0
    private var animProgress = 0f
    private var hitAnimator: ValueAnimator? = null
    private var isAnimating = false
    private var animationStarted = false

    // 正在碎裂的格子
    private val breakingCell: Int
        get() = if (isAnimating) hitOrder.getOrNull(currentHitIndex) ?: -1 else -1

    // 已击碎的格子
    private val brokenCells = mutableSetOf<Int>()

    // 统计
    private val levelCounts = IntArray(5)
    private var totalHitNonZero = 0

    data class GridCell(
        val rect: RectF,
        val dateStr: String,
        val count: Int,
        val level: Int,
        val col: Int,
        val row: Int
    )

    init {
        textPaint.apply {
            color = Color.parseColor("#666666")
            textAlign = Paint.Align.LEFT
        }
    }

    fun setData(dailyCounts: Map<String, Int>) {
        data = dailyCounts

        endDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        startDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(DAYS - 1))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        resetAnimation()
        requestLayout()
        invalidate()

        // 不自动启动动画，等待用户点击按钮
    }

    private fun resetAnimation() {
        gridCells.clear()
        cellRects.clear()
        hitOrder = emptyList()
        brokenCells.clear()
        currentHitIndex = 0
        animProgress = 0f
        isAnimating = false
        animationStarted = false
        hitAnimator?.cancel()
        resetStats()
    }

    /** 手动启动动画 */
    fun startBreakAnimation() {
        if (isAnimating || animationStarted) return
        animationStarted = true
        isAnimating = true
        startAnimation()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val availableWidth = width - LABEL_WIDTH
        val weeks = (DAYS + 6) / 7 + 1
        cellSize = (availableWidth - (weeks + 1) * CELL_GAP) / weeks
        cellSize = cellSize.coerceAtLeast(6f)
        cornerRadius = (cellSize / 5f).coerceIn(2f, 5f)

        val gridHeight = (7 * (cellSize + CELL_GAP) + CELL_GAP).toInt()
        val totalHeight = gridHeight + MONTH_LABEL_H.toInt() + BAR_MARGIN_TOP.toInt() + BAR_H.toInt() + 8

        setMeasuredDimension(width, totalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cellSize <= 0f) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("M", Locale.getDefault())

        val today = Calendar.getInstance()
        val todayDow = today.get(Calendar.DAY_OF_WEEK)
        val todayRowIndex = if (todayDow == Calendar.SUNDAY) 6 else todayDow - Calendar.MONDAY

        val lastColMonday = today.clone() as Calendar
        lastColMonday.add(Calendar.DAY_OF_YEAR, -todayRowIndex)

        val startDow = startDate.get(Calendar.DAY_OF_WEEK)
        val startRowIndex = if (startDow == Calendar.SUNDAY) 6 else startDow - Calendar.MONDAY
        val startColMonday = startDate.clone() as Calendar
        startColMonday.add(Calendar.DAY_OF_YEAR, -startRowIndex)

        val colCount = ((lastColMonday.timeInMillis - startColMonday.timeInMillis) /
                (7L * 24 * 60 * 60 * 1000)).toInt() + 1

        val availableWidth = width - LABEL_WIDTH
        val actualGridWidth = colCount * (cellSize + CELL_GAP) + CELL_GAP
        val startX = LABEL_WIDTH + ((availableWidth - actualGridWidth) / 2f).coerceAtLeast(0f) + CELL_GAP

        val gridTop = 0f

        // 重建网格数据
        if (gridCells.isEmpty()) {
            buildGridCells(startColMonday, colCount, startX, gridTop, dateFormat, today)
            hitOrder = buildHitOrder()
        }

        // 更新统计
        updateStats()

        // 绘制所有格子
        for ((index, cell) in gridCells.withIndex()) {
            when {
                // count=0的白色格子始终显示白色，不参与动画
                cell.count == 0 -> {
                    paint.color = Color.WHITE
                    canvas.drawRoundRect(cell.rect, cornerRadius, cornerRadius, paint)
                    // 绘制细边框
                    paint.color = Color.parseColor("#EEEEEE")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(cell.rect, cornerRadius, cornerRadius, paint)
                    paint.style = Paint.Style.FILL
                }
                // 已击碎的显示白色背景
                index in brokenCells -> {
                    paint.color = Color.WHITE
                    canvas.drawRoundRect(cell.rect, cornerRadius, cornerRadius, paint)
                }
                // 正在碎裂的显示碎裂效果
                index == breakingCell -> {
                    drawBreakingCell(canvas, cell)
                }
                // 未击碎的显示原色
                else -> {
                    paint.color = getCellColor(cell.count)
                    canvas.drawRoundRect(cell.rect, cornerRadius, cornerRadius, paint)
                }
            }
        }

        // 绘制标签
        drawMonthLabels(canvas, startX, colCount, monthFormat, startColMonday)
        drawWeekLabels(canvas, gridTop)

        // 绘制占比条
        val gridHeight = 7 * (cellSize + CELL_GAP) + CELL_GAP
        drawProgressBar(canvas, gridHeight + MONTH_LABEL_H + BAR_MARGIN_TOP, availableWidth)
    }

    // 绘制正在碎裂的格子 - 使用统一的碎裂配色
    private fun drawBreakingCell(canvas: Canvas, cell: GridCell) {
        val centerX = cell.rect.centerX()
        val centerY = cell.rect.centerY()
        val progress = animProgress

        // 碎裂成4块，向四个角飞散
        val offsets = listOf(
            -1f to -1f, // 左上
            1f to -1f,  // 右上
            -1f to 1f,  // 左下
            1f to 1f    // 右下
        )

        // 统一使用橙红色系作为碎裂动画配色，视觉效果更明显
        val breakColors = listOf(
            Color.parseColor("#FF8A65"), // 浅橙
            Color.parseColor("#FF7043"), // 中橙
            Color.parseColor("#FF5722"), // 深橙
            Color.parseColor("#E64A19")  // 红橙
        )

        val pieceSize = cellSize * 0.4f * (1f - progress * 0.3f)
        val maxOffset = cellSize * 0.8f * progress

        for ((i, dxdy) in offsets.withIndex()) {
            val (dx, dy) = dxdy
            val offsetX = dx * maxOffset
            val offsetY = dy * maxOffset
            val rotation = progress * 45f * (if (dx > 0) 1 else -1)

            canvas.save()
            canvas.translate(centerX + offsetX, centerY + offsetY)
            canvas.rotate(rotation)

            // 使用统一配色
            paint.color = breakColors[i]
            val pieceRect = RectF(-pieceSize/2, -pieceSize/2, pieceSize/2, pieceSize/2)
            canvas.drawRoundRect(pieceRect, 2f, 2f, paint)

            // 碎片高光
            paint.color = Color.argb((150 * (1f - progress)).toInt(), 255, 255, 255)
            val highlightRect = RectF(-pieceSize/3, -pieceSize/3, pieceSize/3, 0f)
            canvas.drawRoundRect(highlightRect, 1f, 1f, paint)

            canvas.restore()
        }

        // 碎裂中心闪光效果
        if (progress < 0.5f) {
            val flashAlpha = ((1f - progress * 2) * 255).toInt()
            paint.color = Color.argb(flashAlpha, 255, 200, 150)
            canvas.drawCircle(centerX, centerY, cellSize * progress * 1.2f, paint)
        }

        // 碎屑粒子 - 橙色系
        if (progress > 0.1f && progress < 0.7f) {
            val particleAlpha = ((1f - kotlin.math.abs(progress - 0.4f) * 2.5f) * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(particleAlpha, 255, 150, 80)
            for (i in 0..7) {
                val angle = Math.PI * 2 * i / 8 + progress * 2
                val distance = cellSize * (0.3f + progress * 0.8f)
                val px = centerX + kotlin.math.cos(angle).toFloat() * distance
                val py = centerY + kotlin.math.sin(angle).toFloat() * distance
                canvas.drawCircle(px, py, cellSize * 0.1f * (1f - progress), paint)
            }
        }
    }

    private fun buildGridCells(
        startColMonday: Calendar, colCount: Int, startX: Float, gridTop: Float,
        dateFormat: SimpleDateFormat, today: Calendar
    ) {
        val colCalendar = startColMonday.clone() as Calendar

        for (col in 0 until colCount) {
            for (day in 0 until 7) {
                val isInRange = colCalendar.timeInMillis >= startDate.timeInMillis &&
                        colCalendar.timeInMillis <= endDate.timeInMillis
                val isFuture = colCalendar.timeInMillis > today.timeInMillis

                if (isInRange && !isFuture) {
                    val dateStr = dateFormat.format(colCalendar.time)
                    val count = data[dateStr] ?: 0
                    val level = getLevel(count)

                    val rect = RectF(
                        startX + col * (cellSize + CELL_GAP),
                        gridTop + day * (cellSize + CELL_GAP),
                        startX + col * (cellSize + CELL_GAP) + cellSize,
                        gridTop + day * (cellSize + CELL_GAP) + cellSize
                    )

                    gridCells.add(GridCell(rect, dateStr, count, level, col, day))
                    cellRects.add(rect to dateStr)
                }
                colCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    // 构建击碎顺序：只包含非0格子，按level从低到高
    private fun buildHitOrder(): List<Int> {
        if (gridCells.isEmpty()) return emptyList()

        val result = mutableListOf<Int>()

        // 只选择非0格子，按level分组
        val levelGroups = (1..5).map { level ->
            gridCells.withIndex()
                .filter { it.value.level == level && it.value.count > 0 }
                .map { it.index }
                .shuffled()
        }

        // 从level 1到level 5依次加入
        for (level in 0..4) {
            result.addAll(levelGroups[level])
        }

        return result
    }

    private fun drawMonthLabels(
        canvas: Canvas, startX: Float, colCount: Int,
        monthFormat: SimpleDateFormat, startCal: Calendar
    ) {
        val gridHeight = 7 * (cellSize + CELL_GAP) + CELL_GAP
        textPaint.textSize = sp(10f)
        textPaint.color = Color.parseColor("#888888")
        textPaint.textAlign = Paint.Align.LEFT

        val cal = startCal.clone() as Calendar
        var lastMonth = ""

        for (col in 0 until colCount) {
            val monthLabel = monthFormat.format(cal.time)
            if (monthLabel != lastMonth) {
                val x = startX + col * (cellSize + CELL_GAP)
                canvas.drawText(monthLabel, x, gridHeight + MONTH_LABEL_H - 4f, textPaint)
                lastMonth = monthLabel
            }
            cal.add(Calendar.DAY_OF_YEAR, 7)
        }
    }

    private fun drawWeekLabels(canvas: Canvas, gridTop: Float) {
        val weekLabels = listOf("一", "三", "五", "日")
        val weekDayIndices = listOf(0, 2, 4, 6)
        textPaint.textSize = sp(10f)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.parseColor("#888888")

        for (i in weekLabels.indices) {
            val rowIndex = weekDayIndices[i]
            val y = gridTop + rowIndex * (cellSize + CELL_GAP) + cellSize / 2f + textPaint.textSize / 3f
            canvas.drawText(weekLabels[i], LABEL_WIDTH - 3f, y, textPaint)
        }
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawProgressBar(canvas: Canvas, topY: Float, availableWidth: Float) {
        val barTop = topY
        val barBottom = barTop + BAR_H
        val barR = BAR_H / 2f

        val totalNonZero = gridCells.count { it.count > 0 }.coerceAtLeast(1)
        val hitProgress = totalHitNonZero.toFloat() / totalNonZero
        val filledWidth = availableWidth * hitProgress

        val levelColors = listOf(
            Color.parseColor("#C8E6C9"),
            Color.parseColor("#81C784"),
            Color.parseColor("#FDD835"),
            Color.parseColor("#FF8A65"),
            Color.parseColor("#E57373")
        )

        // 背景
        paint.color = Color.parseColor("#E8E8E8")
        val bgRect = RectF(LABEL_WIDTH, barTop, LABEL_WIDTH + availableWidth, barBottom)
        canvas.drawRoundRect(bgRect, barR, barR, paint)

        // 已击碎部分
        val total = totalHitNonZero.coerceAtLeast(1)
        var currentX = LABEL_WIDTH

        for (level in 1..5) {
            val count = levelCounts[level - 1]
            if (count > 0) {
                val proportion = count.toFloat() / total
                val segWidth = filledWidth * proportion

                if (segWidth > 0f) {
                    val rect = RectF(currentX, barTop, currentX + segWidth, barBottom)
                    paint.color = levelColors[level - 1]
                    canvas.drawRoundRect(rect, barR, barR, paint)
                    currentX += segWidth
                }
            }
        }
    }

    private fun updateStats() {
        resetStats()

        // 统计已击碎的非0格子
        for (idx in brokenCells) {
            val cell = gridCells.getOrNull(idx) ?: continue
            if (cell.count > 0) {
                totalHitNonZero++
                val arrayIdx = (cell.level - 1).coerceIn(0, 4)
                levelCounts[arrayIdx]++
            }
        }
    }

    private fun resetStats() {
        levelCounts.fill(0)
        totalHitNonZero = 0
    }

    private fun getLevel(count: Int): Int {
        return when {
            count == 0 -> 0
            count <= 40 -> 1
            count <= 70 -> 2
            count <= 100 -> 3
            count <= 140 -> 4
            else -> 5
        }
    }

    private fun getCellColor(count: Int): Int {
        return when {
            count == 0 -> Color.WHITE
            count <= 40 -> Color.parseColor("#C8E6C9")
            count <= 70 -> Color.parseColor("#81C784")
            count <= 100 -> Color.parseColor("#FDD835")
            count <= 140 -> Color.parseColor("#FF8A65")
            else -> Color.parseColor("#E57373")
        }
    }

    private fun startAnimation() {
        hitAnimator?.cancel()
        currentHitIndex = 0
        brokenCells.clear()
        animProgress = 0f
        resetStats()

        if (hitOrder.isEmpty()) {
            // 没有可击碎的格子，直接结束
            isAnimating = false
            return
        }

        // 每个格子碎裂动画200ms
        val hitDuration = 200L

        animateNextHit(hitDuration)
    }

    private fun animateNextHit(hitDuration: Long) {
        if (currentHitIndex >= hitOrder.size) {
            // 全部击碎，保持2秒后reset
            postDelayed({
                isAnimating = false
                animationStarted = false
                brokenCells.clear()
                currentHitIndex = 0
                animProgress = 0f
                resetStats()
                invalidate()
            }, 2000L)
            return
        }

        animProgress = 0f
        hitAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = hitDuration
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                animProgress = anim.animatedFraction
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // 动画结束，标记当前格子为已击碎
                    val hitIdx = hitOrder.getOrNull(currentHitIndex)
                    if (hitIdx != null) {
                        brokenCells.add(hitIdx)
                    }
                    currentHitIndex++
                    // 继续击碎下一个
                    animateNextHit(hitDuration)
                }
            })
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                for ((rect, dateStr) in cellRects) {
                    if (rect.contains(x, y)) {
                        onCellClickListener?.invoke(dateStr)
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hitAnimator?.cancel()
    }
}
