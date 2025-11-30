package com.example.intent.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ThemeToggle(
    themeMode: Int, // 0=Light, 1=Dark, 2=PitchBlack
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 96.dp
    val trackHeight = 40.dp
    val thumbSize = 32.dp
    val padding = 4.dp

    val backgroundColor = Color(0xFF454545) // Dark Grey Track
    val thumbColor = Color(0xFFD0E0FF) // Light Blue Thumb
    val iconColor = Color(0xFF001D3D) // Dark Blue Icon
    
    // Calculate target offset
    // 0 -> Left (padding)
    // 1 -> Middle (trackWidth/2 - thumbSize/2)
    // 2 -> Right (trackWidth - thumbSize - padding)
    
    val targetOffset = when (themeMode) {
        0 -> padding
        1 -> (trackWidth - thumbSize) / 2
        else -> trackWidth - thumbSize - padding
    }

    // Track current drag offset
    var dragOffset by remember { mutableStateOf(targetOffset) }
    var isDragging by remember { mutableStateOf(false) }

    // Animate thumb position
    val thumbOffsetX by animateDpAsState(
        targetValue = if (isDragging) dragOffset else targetOffset,
        animationSpec = tween(durationMillis = 300),
        label = "ThumbOffset"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(backgroundColor)
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(trackHeight / 2))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        // Snap to nearest state based on final position
                        val currentOffsetPx = dragOffset.toPx()
                        val trackWidthPx = trackWidth.toPx()
                        
                        val leftThreshold = (trackWidthPx / 3)
                        val rightThreshold = (trackWidthPx * 2 / 3)
                        
                        val newMode = when {
                            currentOffsetPx < leftThreshold -> 0
                            currentOffsetPx < rightThreshold -> 1
                            else -> 2
                        }
                        
                        // Only trigger onToggle if mode changed
                        if (newMode != themeMode) {
                            val steps = (newMode - themeMode + 3) % 3
                            repeat(steps) { onToggle() }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newOffset = (dragOffset.value + dragAmount / density).dp
                    val maxOffset = trackWidth - thumbSize - padding
                    dragOffset = newOffset.coerceIn(padding, maxOffset)
                }
            }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb Layer
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .size(thumbSize)
                .shadow(4.dp, CircleShape)
                .background(thumbColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (themeMode) {
                    0 -> Icons.Outlined.LightMode
                    1 -> Icons.Outlined.DarkMode
                    else -> Icons.Filled.DarkMode
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
