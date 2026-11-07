package com.cabincrew.academy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cabincrew.academy.data.db.Difficulty
import com.cabincrew.academy.ui.theme.AppTheme
import com.cabincrew.academy.ui.theme.EyebrowStyle

/** True on tablets in landscape / large windows. */
@Composable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp >= 840

@Composable
fun isMediumLayout(): Boolean = LocalConfiguration.current.screenWidthDp >= 600

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, AppTheme.extended.cardBorder)
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, elevation = elevation) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border, elevation = elevation) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondary) {
    Text(text.uppercase(), modifier = modifier, style = EyebrowStyle, color = color)
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(AppTheme.extended.gold))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Navy gradient banner with a thin gold underline – the app's signature header. */
@Composable
fun HeroBanner(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ext = AppTheme.extended
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(ext.heroStart, ext.heroEnd)))
    ) {
        Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp), content = content)
        Box(
            Modifier.fillMaxWidth().height(3.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, ext.gold, Color.Transparent)))
        )
    }
}

@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    supporting: String? = null,
) {
    AppCard(modifier, contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
            }
        }
        if (supporting != null) {
            Spacer(Modifier.height(6.dp))
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 460.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = if (destructive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError,
                ) else ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/** Standard screen with top app bar and content constrained to a readable width on large tablets. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    subtitle: String? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (subtitle != null) Text(
                            subtitle, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

/** Centers content and caps its width so lines stay readable on 12" tablets. */
@Composable
fun ContentWidth(maxWidth: Dp = 1100.dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) { content() }
    }
}

@Composable
fun DifficultyBadge(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val color = when (difficulty) {
        Difficulty.EASY -> AppTheme.extended.success
        Difficulty.MEDIUM -> AppTheme.extended.warning
        Difficulty.HARD -> AppTheme.extended.danger
    }
    Pill(difficulty.label, color, modifier)
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

enum class OptionState { NEUTRAL, SELECTED, CORRECT, INCORRECT, MISSED }

/** One multiple choice answer, with an A/B/C letter badge and animated feedback colours. */
@Composable
fun OptionCard(
    index: Int,
    text: String,
    state: OptionState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val ext = AppTheme.extended
    val cs = MaterialTheme.colorScheme
    val targetBorder = when (state) {
        OptionState.NEUTRAL -> ext.cardBorder
        OptionState.SELECTED -> cs.primary
        OptionState.CORRECT, OptionState.MISSED -> ext.success
        OptionState.INCORRECT -> ext.danger
    }
    val targetBg = when (state) {
        OptionState.NEUTRAL -> cs.surfaceContainerLowest
        OptionState.SELECTED -> cs.primaryContainer
        OptionState.CORRECT -> ext.successContainer
        OptionState.MISSED -> ext.successContainer.copy(alpha = 0.5f)
        OptionState.INCORRECT -> ext.dangerContainer
    }
    val border by animateColorAsState(targetBorder, tween(250), label = "border")
    val bg by animateColorAsState(targetBg, tween(250), label = "bg")
    val badgeBg = when (state) {
        OptionState.NEUTRAL -> cs.surfaceVariant
        OptionState.SELECTED -> cs.primary
        OptionState.CORRECT, OptionState.MISSED -> ext.success
        OptionState.INCORRECT -> ext.danger
    }
    val badgeFg = if (state == OptionState.NEUTRAL) cs.onSurfaceVariant else Color.White
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.fillMaxWidth().border(if (state == OptionState.NEUTRAL) 1.dp else 2.dp, border, shape),
        shape = shape,
        color = bg,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
                Text(('A' + index).toString(), color = badgeFg, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(14.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when (state) {
                OptionState.CORRECT, OptionState.MISSED ->
                    Icon(Icons.Filled.CheckCircle, "Correct answer", tint = ext.success)
                OptionState.INCORRECT -> Icon(Icons.Filled.Cancel, "Your answer (incorrect)", tint = ext.danger)
                else -> {}
            }
        }
    }
}

/** Lays children in a responsive grid of [columns] equal-width cells. */
@Composable
fun <T> ResponsiveGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 14.dp,
    /** When true, items in an incomplete last row stretch to fill it. */
    fillLastRow: Boolean = false,
    itemContent: @Composable (T) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { Box(Modifier.weight(1f)) { itemContent(it) } }
                if (!fillLastRow) repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun columnsForWidth(minCell: Dp = 260.dp): Int {
    val w = LocalConfiguration.current.screenWidthDp.dp
    return (w / minCell).toInt().coerceIn(1, 4)
}

fun formatPercent(v: Float): String = "${(v * 100).toInt()}%"

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun rememberSnackbarHost() = remember { SnackbarHostState() }

