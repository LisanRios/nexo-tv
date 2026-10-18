package com.example.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoFocusBorder
import com.example.ui.theme.NexoFocusGlow
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoSurfaceBorder

/**
 * TV Focus modifier: applies high-contrast border and smooth subtle scale
 * on D-pad navigation focus without clipping adjacent elements.
 */
@Composable
fun Modifier.tvFocusable(
    testTag: String,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    scaleOnFocus: Float = 1.04f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(targetValue = if (isFocused) scaleOnFocus else 1.0f, label = "tvFocusScale")

    return this
        .testTag(testTag)
        .scale(scale)
        .border(
            width = if (isFocused) 3.dp else 1.dp,
            brush = if (isFocused) {
                Brush.horizontalGradient(listOf(NexoFocusBorder, NexoBlueLight))
            } else {
                Brush.linearGradient(listOf(NexoSurfaceBorder, NexoSurfaceBorder))
            },
            shape = shape
        )
        .background(
            color = if (isFocused) NexoFocusGlow else Color.Transparent,
            shape = shape
        )
        .focusable(interactionSource = interactionSource)
}

/**
 * Clean monogram avatar box for sports websites and channels.
 * Avoids blurry/missing network logos and complies with no fake branding.
 */
@Composable
fun SiteMonogram(
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val initials = title.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifEmpty { "TV" }

    val hash = title.hashCode()
    val gradientColors = when (Math.abs(hash) % 4) {
        0 -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
        1 -> listOf(Color(0xFF065F46), Color(0xFF10B981))
        2 -> listOf(Color(0xFF581C87), Color(0xFF8B5CF6))
        else -> listOf(Color(0xFF0F172A), Color(0xFF334155))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun SportBadge(
    sport: String,
    modifier: Modifier = Modifier
) {
    val color = when (sport.lowercase()) {
        "fútbol" -> Color(0xFF10B981)
        "básquet" -> Color(0xFFF59E0B)
        "tenis" -> Color(0xFFEAB308)
        "automovilismo" -> Color(0xFFEF4444)
        "combate" -> Color(0xFFEC4899)
        else -> NexoBlueLight
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = sport,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
