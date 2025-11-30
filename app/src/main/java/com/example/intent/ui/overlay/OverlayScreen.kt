package com.example.intent.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intent.ui.theme.PrimaryLight
import com.example.intent.ui.theme.PrimaryContainerLight

@Composable
fun OverlayScreen(
    delaySeconds: Int,
    onContinue: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMood by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    
    // Timer state
    var timeRemaining by remember { mutableStateOf(delaySeconds) }
    val isTimerFinished = timeRemaining <= 0

    LaunchedEffect(Unit) {
        visible = true
        while (timeRemaining > 0) {
            kotlinx.coroutines.delay(1000L)
            timeRemaining--
        }
    }

    // Material 3 Expressive Colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
            .padding(32.dp), // Generous padding for expressive design
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { -it } + androidx.compose.animation.fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp), // Generous spacing
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Lock Icon - Expressive size
                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = primaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Title - Display Large (Material 3 Expressive)
                Text(
                    text = "Mindfulness\nCheck-in",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 48.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Question 1 - Headline Small
                Text(
                    text = "Why are you opening this app?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Mood Buttons (Grid 2x2) - Expressive styling
                val moods = listOf(
                    "Learning / Productive",
                    "Thought of something",
                    "Bored / Doomscrolling",
                    "Anxiety / Stress"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExpressiveMoodButton(
                            text = moods[0],
                            isSelected = selectedMood == moods[0],
                            onClick = { selectedMood = moods[0] },
                            modifier = Modifier.weight(1f)
                        )
                        ExpressiveMoodButton(
                            text = moods[1],
                            isSelected = selectedMood == moods[1],
                            onClick = { selectedMood = moods[1] },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExpressiveMoodButton(
                            text = moods[2],
                            isSelected = selectedMood == moods[2],
                            onClick = { selectedMood = moods[2] },
                            modifier = Modifier.weight(1f)
                        )
                        ExpressiveMoodButton(
                            text = moods[3],
                            isSelected = selectedMood == moods[3],
                            onClick = { selectedMood = moods[3] },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Question 2
                Text(
                    text = "What is your specific intention?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Note Input - Expressive surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = { 
                            Text(
                                "Write a note...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = onSurfaceColor,
                            unfocusedTextColor = onSurfaceColor
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons - Expressive styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Don't Open Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(2.dp, primaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryColor
                        )
                    ) {
                        Text(
                            "Don't Open",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Continue/Wait Button
                    Button(
                        onClick = {
                            if (selectedMood != null && isTimerFinished) {
                                onContinue(selectedMood!!, note)
                            }
                        },
                        enabled = selectedMood != null && isTimerFinished,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        Text(
                            if (isTimerFinished) "Continue" else "Wait ${timeRemaining}s",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressiveMoodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(36.dp),
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = contentColor,
                lineHeight = 18.sp
            )
        }
    }
}
