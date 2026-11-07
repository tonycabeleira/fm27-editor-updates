package com.cabincrew.academy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cabincrew.academy.ui.theme.AppTheme

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.secondary,
    trackColor: Color = AppTheme.extended.chartTrack,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(trackColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

/** Two-segment ring: correct vs incorrect. */
@Composable
fun SplitRing(
    correct: Int,
    incorrect: Int,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 16.dp,
    content: @Composable () -> Unit = {},
) {
    val total = (correct + incorrect).coerceAtLeast(1)
    val anim = remember { Animatable(0f) }
    LaunchedEffect(correct, incorrect) { anim.snapTo(0f); anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val good = AppTheme.extended.success
    val bad = AppTheme.extended.danger
    val track = AppTheme.extended.chartTrack
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(track, -90f, 360f, false, tl, arcSize, style = Stroke(stroke))
            if (correct + incorrect > 0) {
                val gSweep = 360f * correct / total * anim.value
                val bSweep = 360f * incorrect / total * anim.value
                drawArc(good, -90f, gSweep, false, tl, arcSize, style = Stroke(stroke))
                drawArc(bad, -90f + gSweep, bSweep, false, tl, arcSize, style = Stroke(stroke))
            }
        }
        content()
    }
}

/** Horizontal bar with animated fill. */
@Composable
fun MeterBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing), label = "meter")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(AppTheme.extended.chartTrack)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(height))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)))
        )
    }
}

data class BarDatum(val label: String, val value: Float, val highlight: Boolean = false)

/** Vertical bar chart with labels underneath. Values are drawn relative to [maxValue]. */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    maxValue: Float = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f),
    chartHeight: Dp = 160.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.secondary,
    valueFormatter: (Float) -> String = { if (it == 0f) "" else it.toInt().toString() },
    goalLine: Float? = null,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(data) { anim.snapTo(0f); anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val track = AppTheme.extended.chartTrack
    val gold = AppTheme.extended.gold
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(chartHeight), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            data.forEach { d ->
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        valueFormatter(d.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        Canvas(Modifier.fillMaxSize()) {
                            val w = size.width.coerceAtMost(28.dp.toPx())
                            val left = (size.width - w) / 2
                            drawRoundRect(track, Offset(left, 0f), Size(w, size.height), androidx.compose.ui.geometry.CornerRadius(w / 3))
                            val h = size.height * (d.value / maxValue).coerceIn(0f, 1f) * anim.value
                            if (h > 0f) drawRoundRect(
                                if (d.highlight) highlightColor else barColor,
                                Offset(left, size.height - h), Size(w, h),
                                androidx.compose.ui.geometry.CornerRadius(w / 3),
                            )
                            if (goalLine != null && goalLine > 0f) {
                                val y = size.height * (1f - (goalLine / maxValue).coerceIn(0f, 1f))
                                drawLine(gold, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            data.forEach { d ->
                Text(
                    d.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Clip,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Line chart for percentage values (0..100) with an optional dashed threshold line. */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    threshold: Float? = null,
    chartHeight: Dp = 180.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(values) { anim.snapTo(0f); anim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    val gold = AppTheme.extended.gold
    val grid = AppTheme.extended.chartTrack
    val dotInner = MaterialTheme.colorScheme.surfaceContainerLowest
    Canvas(modifier.fillMaxWidth().height(chartHeight)) {
        val padV = 10.dp.toPx()
        val h = size.height - padV * 2
        fun yFor(v: Float) = padV + h * (1f - v.coerceIn(0f, 100f) / 100f)
        listOf(0f, 25f, 50f, 75f, 100f).forEach { g ->
            drawLine(grid, Offset(0f, yFor(g)), Offset(size.width, yFor(g)), 1.dp.toPx())
        }
        threshold?.let {
            drawLine(gold, Offset(0f, yFor(it)), Offset(size.width, yFor(it)), 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
        }
        if (values.isEmpty()) return@Canvas
        val step = if (values.size == 1) 0f else size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            Offset(if (values.size == 1) size.width / 2 else i * step, yFor(v * anim.value + (1 - anim.value) * 0f))
        }
        val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        val fill = Path().apply {
            addPath(line)
            lineTo(pts.last().x, size.height); lineTo(pts.first().x, size.height); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(line, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        pts.forEach {
            drawCircle(lineColor, 5.dp.toPx(), it)
            drawCircle(dotInner, 2.5.dp.toPx(), it)
        }
    }
}
