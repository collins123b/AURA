package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AcademicHabitEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.FocusRecordEntity
import com.example.ui.theme.AuraMonoFontFamily
import com.example.ui.theme.LocalAuraColors
import com.example.ui.theme.OutfitFontFamily
import com.example.util.MediaStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats 24h "HH:mm" string (e.g. "14:30") into clean 12h AM/PM display (e.g. "2:30 PM").
 */
fun formatTime12Hour(time24: String): String {
    return try {
        val parts = time24.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val lt = LocalTime.of(h, m)
        lt.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    } catch (_: Exception) {
        time24
    }
}

/**
 * Computes 12-hour AM/PM departure time ("Leave by 8:20 AM") given 24h lecture start and campus walk buffer.
 */
fun computeLeaveByTime12Hour(startTime24: String, walkingMinutes: Int): String {
    return try {
        val parts = startTime24.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        val leaveTime = LocalTime.of(h, m).minusMinutes(walkingMinutes.coerceIn(0, 120).toLong())
        leaveTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    } catch (_: Exception) {
        formatTime12Hour(startTime24)
    }
}

/**
 * Iridescent Prism & Aurora Liquid Glass Background
 * Renders multi-layered celestial sapphire, glacial cyan, violet aurora ribbons,
 * diagonal glass light refraction, and frosted crystal grain.
 */
@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val auraColors = LocalAuraColors.current
    val isDark = auraColors.isDark
    val currentHour = remember { LocalTime.now().hour }

    // Dynamic Time-of-Day Aurora Mood (Morning Crisp Glacial, Afternoon Sapphire, Night Deep Indigo)
    val darkBackdropStops = when {
        currentHour in 5..11 -> listOf(
            Color(0xFF071226),
            Color(0xFF0C1D38),
            Color(0xFF09152B),
            Color(0xFF050A16)
        )
        currentHour in 12..17 -> listOf(
            Color(0xFF060B18),
            Color(0xFF0B162C),
            Color(0xFF081124),
            Color(0xFF050914)
        )
        else -> listOf(
            Color(0xFF050814),
            Color(0xFF091126),
            Color(0xFF070D1E),
            Color(0xFF040710)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        darkBackdropStops
                    } else {
                        listOf(
                            Color(0xFFF0F6FF),
                            Color(0xFFE0ECFC),
                            Color(0xFFECF4FE)
                        )
                    }
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Multi-Chromatic Aurora Ambient Refraction Orbs (responsive to active Theme Preset)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraColors.primaryAccent.copy(alpha = if (isDark) 0.26f else 0.16f),
                        auraColors.secondaryAccent.copy(alpha = if (isDark) 0.10f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.90f, h * 0.08f),
                    radius = w * 0.88f
                ),
                radius = w * 0.88f,
                center = Offset(w * 0.90f, h * 0.08f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x2E818CF8) else Color(0x1E6366F1),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.08f, h * 0.30f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.08f, h * 0.30f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isDark) Color(0x2A2DD4BF) else Color(0x2014B8A6),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.14f, h * 0.86f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.14f, h * 0.86f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraColors.secondaryAccent.copy(alpha = if (isDark) 0.16f else 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.65f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.85f, h * 0.65f)
            )

            // 2. Diagonal Soft Prism Light Beam
            val beamPath = Path().apply {
                moveTo(w * 0.25f, 0f)
                lineTo(w * 0.95f, 0f)
                lineTo(w * 0.45f, h * 0.55f)
                lineTo(-w * 0.15f, h * 0.35f)
                close()
            }
            drawPath(
                path = beamPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isDark) Color(0x12FFFFFF) else Color(0x45FFFFFF),
                        if (isDark) Color(0x085CE1FF) else Color(0x180A84FF),
                        Color.Transparent
                    ),
                    start = Offset(w * 0.6f, 0f),
                    end = Offset(w * 0.15f, h * 0.5f)
                )
            )

            // 3. Precision Concentric Orbital Lens Rings
            drawCircle(
                color = if (isDark) Color(0x18FFFFFF) else Color(0x50FFFFFF),
                radius = w * 0.40f,
                center = Offset(w * 0.90f, h * 0.10f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = if (isDark) Color(0x145CE1FF) else Color(0x260A84FF),
                radius = w * 0.66f,
                center = Offset(w * 0.90f, h * 0.10f),
                style = Stroke(width = 0.8.dp.toPx())
            )

            // 4. Subtle Starlight Frost Matrix
            val dotSpacing = 38.dp.toPx()
            val dotRadius = 0.9.dp.toPx()
            val dotColor = if (isDark) Color(0x16DCEBFF) else Color(0x1A0A84FF)

            var x = dotSpacing / 2
            while (x < w) {
                var y = dotSpacing / 2
                while (y < h) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += dotSpacing
                }
                x += dotSpacing
            }
        }
        content()
    }
}

/**
 * Iridescent Liquid Glass Card - Refractive multi-layer glass surface with chromatic rim,
 * top-edge specular light reflection, optional ambient corner glow, and tactile spring press.
 */
@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    accentGlow: Color? = null,
    elevation: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val auraColors = LocalAuraColors.current
    val glassShape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardSpringScale"
    )

    val cardBrush = if (backgroundColor != null) {
        Brush.verticalGradient(
            listOf(
                backgroundColor.copy(alpha = (backgroundColor.alpha + 0.08f).coerceAtMost(0.96f)),
                backgroundColor
            )
        )
    } else if (auraColors.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xDE182748),
                Color(0xD40F1B33),
                Color(0xDC13213D)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 700f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF5FFFFFF),
                Color(0xE2EFF6FF)
            )
        )
    }

    val rimBrush = if (borderColor != null) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.75f),
                borderColor,
                borderColor.copy(alpha = 0.45f)
            )
        )
    } else if (auraColors.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x78FFFFFF),
                Color(0x365CE1FF),
                Color(0x22818CF8),
                Color(0x420A84FF)
            ),
            start = Offset(0f, 0f),
            end = Offset(600f, 500f)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0x450A84FF)
            )
        )
    }

    val baseModifier = modifier
        .scale(scale)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .shadow(
            elevation = elevation,
            shape = glassShape,
            spotColor = accentGlow?.copy(alpha = 0.45f)
                ?: if (auraColors.isDark) Color(0x99020612) else Color(0x240A84FF),
            ambientColor = accentGlow?.copy(alpha = 0.25f)
                ?: if (auraColors.isDark) Color(0x66020612) else Color(0x160A84FF)
        )
        .clip(glassShape)
        .background(cardBrush)
        .border(1.2.dp, rimBrush, glassShape)
        .drawWithContent {
            // Optional corner ambient glow
            if (accentGlow != null) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentGlow.copy(alpha = if (auraColors.isDark) 0.22f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.16f),
                        radius = size.minDimension * 0.85f
                    ),
                    radius = size.minDimension * 0.85f,
                    center = Offset(size.width * 0.88f, size.height * 0.16f)
                )
            }

            // Top-half frosted glass inner sheen
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        if (auraColors.isDark) Color(0x18FFFFFF) else Color(0x60FFFFFF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.42f
                )
            )
            drawContent()
            // Specular prism top-edge light catch
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xAAFFFFFF),
                        Color(0xAA5CE1FF),
                        Color.Transparent
                    )
                ),
                start = Offset(x = cornerRadius.toPx() * 0.50f, y = 1.dp.toPx()),
                end = Offset(x = size.width - (cornerRadius.toPx() * 0.50f), y = 1.dp.toPx()),
                strokeWidth = 1.6.dp.toPx()
            )
        }

    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(color = auraColors.secondaryAccent.copy(alpha = 0.25f)),
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        content()
    }
}

/**
 * Iridescent Liquid Glass Button
 * Features spring-physics tactile scale, multi-stop refractive lens gloss,
 * luminous top specular highlight, and frosted rim border.
 */
@Composable
fun AuraGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color? = null,
    borderColor: Color? = null,
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 8.dp,
    testTag: String = "aura_glass_button",
    content: @Composable RowScope.() -> Unit
) {
    val auraColors = LocalAuraColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val fillBrush = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0x14FFFFFF), Color(0x0AFFFFFF)))
        containerColor != null -> Brush.verticalGradient(
            listOf(
                containerColor.copy(alpha = (containerColor.alpha + 0.18f).coerceAtMost(0.95f)),
                containerColor
            )
        )
        isPrimary -> Brush.linearGradient(
            colors = listOf(
                Color(0xF038BDF8), // Glacial cyan highlight
                Color(0xF50A84FF), // Electric sapphire core
                Color(0xEC4F46E5)  // Deep royal indigo prism base
            ),
            start = Offset(0f, 0f),
            end = Offset(340f, 200f)
        )
        else -> Brush.verticalGradient(
            colors = if (auraColors.isDark) {
                listOf(
                    Color(0x38FFFFFF),
                    Color(0x1EDCEBFF)
                )
            } else {
                listOf(
                    Color(0xF0FFFFFF),
                    Color(0xD8EAF3FF)
                )
            }
        )
    }

    val rimBrush = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0x18FFFFFF), Color(0x0CFFFFFF)))
        borderColor != null -> Brush.verticalGradient(listOf(Color(0xBBFFFFFF), borderColor))
        isPrimary -> Brush.verticalGradient(
            listOf(
                Color(0xDDFFFFFF),
                Color(0x755CE1FF),
                Color(0x884F46E5)
            )
        )
        else -> Brush.verticalGradient(
            listOf(
                Color(0x88FFFFFF),
                Color(0x32FFFFFF),
                Color(0x485CE1FF)
            )
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag)
            .shadow(
                elevation = if (enabled) elevation else 0.dp,
                shape = shape,
                spotColor = if (isPrimary) Color(0x990A84FF) else Color(0x55000818),
                ambientColor = if (isPrimary) Color(0x6638BDF8) else Color(0x33000818)
            )
            .clip(shape)
            .background(fillBrush)
            .border(1.2.dp, rimBrush, shape)
            .drawWithContent {
                if (enabled) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x48FFFFFF),
                                Color(0x0AFFFFFF),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.54f
                        ),
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height * 0.52f),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                    )
                }
                drawContent()
                if (enabled) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xDDFFFFFF),
                                Color(0xDDFFFFFF),
                                Color.Transparent
                            )
                        ),
                        start = Offset(x = cornerRadius.toPx() * 0.45f, y = 1.2.dp.toPx()),
                        end = Offset(x = size.width - (cornerRadius.toPx() * 0.45f), y = 1.2.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.White.copy(alpha = 0.35f)),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun AuraGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color? = null,
    borderColor: Color? = null,
    textColor: Color? = null,
    cornerRadius: Dp = 18.dp,
    testTag: String = "aura_glass_button"
) {
    val resolvedTextColor = textColor ?: when {
        !enabled -> LocalAuraColors.current.textMuted
        isPrimary -> Color.White
        else -> LocalAuraColors.current.textPrimary
    }
    AuraGlassButton(
        onClick = onClick,
        modifier = modifier,
        isPrimary = isPrimary,
        enabled = enabled,
        containerColor = containerColor,
        borderColor = borderColor,
        cornerRadius = cornerRadius,
        testTag = testTag
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedTextColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = resolvedTextColor,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 0.15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Circular Liquid Glass Jewel Icon Button (for FABs, Play/Pause, Header actions, Reset)
 */
@Composable
fun AuraGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    isPrimary: Boolean = false,
    containerColor: Color? = null,
    testTag: String = "aura_glass_icon_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconGlassScale"
    )
    val auraColors = LocalAuraColors.current

    val bgBrush = when {
        containerColor != null -> Brush.verticalGradient(
            listOf(containerColor.copy(alpha = 0.92f), containerColor)
        )
        isPrimary -> Brush.linearGradient(
            listOf(Color(0xF238BDF8), Color(0xF50A84FF), Color(0xEB4F46E5))
        )
        auraColors.isDark -> Brush.verticalGradient(
            listOf(Color(0x3AFFFFFF), Color(0x1EDCEBFF))
        )
        else -> Brush.verticalGradient(
            listOf(Color(0xF2FFFFFF), Color(0xDCEAF4FF))
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .testTag(testTag)
            .shadow(
                elevation = if (isPrimary) 12.dp else 6.dp,
                shape = CircleShape,
                spotColor = if (isPrimary) Color(0xAA0A84FF) else Color(0x55000000),
                ambientColor = if (isPrimary) Color(0x6638BDF8) else Color(0x33000000)
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color(0xCCFFFFFF), Color(0x455CE1FF))),
                CircleShape
            )
            .drawWithContent {
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x48FFFFFF), Color.Transparent),
                        startY = 0f,
                        endY = size.toPx() * 0.5f
                    )
                )
                drawContent()
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) Color.White else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Interactive Frosted Liquid Glass Time Selector with 12-Hour AM / PM Dial Picker!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraTimePickerField(
    label: String,
    time24: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "aura_time_picker_field"
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    val parsedParts = remember(time24) {
        val parts = time24.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        h to m
    }

    val display12h = remember(time24) { formatTime12Hour(time24) }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x2EFFFFFF), Color(0x1ADCEBFF))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x305CE1FF))),
                RoundedCornerShape(16.dp)
            )
            .clickable { showPickerDialog = true }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LocalAuraColors.current.primaryAccent.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = label,
                    tint = LocalAuraColors.current.secondaryAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = LocalAuraColors.current.textSecondary
                )
                Text(
                    text = display12h,
                    fontFamily = AuraMonoFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalAuraColors.current.textPrimary
                )
            }
        }
    }

    if (showPickerDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = parsedParts.first,
            initialMinute = parsedParts.second,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showPickerDialog = false },
            title = {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LocalAuraColors.current.textPrimary
                    )
                    Text(
                        text = "Select hour, minute, and AM or PM",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(
                        state = pickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Color(0xFF132038),
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = LocalAuraColors.current.textPrimary,
                            selectorColor = LocalAuraColors.current.primaryAccent,
                            containerColor = Color(0xFF101B30),
                            periodSelectorBorderColor = Color(0x55FFFFFF),
                            periodSelectorSelectedContainerColor = LocalAuraColors.current.primaryAccent,
                            periodSelectorUnselectedContainerColor = Color(0x1EFFFFFF),
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContentColor = LocalAuraColors.current.textSecondary,
                            timeSelectorSelectedContainerColor = Color(0x380A84FF),
                            timeSelectorUnselectedContainerColor = Color(0x1EFFFFFF),
                            timeSelectorSelectedContentColor = LocalAuraColors.current.secondaryAccent,
                            timeSelectorUnselectedContentColor = LocalAuraColors.current.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                AuraGlassButton(
                    text = "Confirm Time",
                    isPrimary = true,
                    onClick = {
                        val formatted24 = String.format(
                            Locale.US,
                            "%02d:%02d",
                            pickerState.hour,
                            pickerState.minute
                        )
                        onTimeSelected(formatted24)
                        showPickerDialog = false
                    },
                    testTag = "${testTag}_confirm"
                )
            },
            dismissButton = {
                AuraGlassButton(
                    text = "Cancel",
                    onClick = { showPickerDialog = false },
                    testTag = "${testTag}_cancel"
                )
            },
            containerColor = Color(0xF20E192E),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x385CE1FF))),
                RoundedCornerShape(28.dp)
            )
        )
    }
}

/**
 * Prismatic Capsule Badge / Tag
 */
@Composable
fun AuraBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x24FFFFFF),
    textColor: Color = LocalAuraColors.current.secondaryAccent,
    icon: ImageVector? = null,
    testTag: String = "aura_glass_badge"
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0x48FFFFFF),
                        textColor.copy(alpha = 0.45f)
                    )
                ),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontFamily = OutfitFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.35.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Bento Prism Metric Stat Card
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    accentBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    testTag: String = "glass_stat_card"
) {
    val resolvedBrush = accentBrush ?: Brush.linearGradient(
        listOf(
            accentColor.copy(alpha = 0.90f),
            accentColor.copy(alpha = 0.55f)
        )
    )

    AuraCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        accentGlow = accentColor,
        onClick = onClick,
        testTag = testTag
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(11.dp),
                            spotColor = accentColor.copy(alpha = 0.65f)
                        )
                        .clip(RoundedCornerShape(11.dp))
                        .background(resolvedBrush)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.55f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = value,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.5).sp,
                    color = LocalAuraColors.current.textPrimary,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = LocalAuraColors.current.textPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Editorial Section Header with Glowing Prism Bar & Liquid Glass Action Pill
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "glass_section_header"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (subtitle != null) 34.dp else 20.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LocalAuraColors.current.secondaryAccent,
                            LocalAuraColors.current.primaryAccent
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalAuraColors.current.textPrimary,
                fontSize = 17.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAuraColors.current.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            AuraGlassButton(
                text = actionLabel,
                onClick = onActionClick,
                cornerRadius = 50.dp,
                textColor = LocalAuraColors.current.secondaryAccent,
                testTag = "${testTag}_action"
            )
        }
    }
}

/**
 * Orbital Halo Student Avatar
 * Displays the student's uploaded profile photo or luminous gradient monogram,
 * framed by an iridescent double halo ring and optional tactile camera badge.
 */
@Composable
fun AuraUserAvatar(
    imagePath: String,
    studentName: String,
    size: Dp = 54.dp,
    showCameraBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "aura_user_avatar"
) {
    val auraColors = LocalAuraColors.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = imagePath) {
        value = if (imagePath.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                MediaStorageHelper.loadBitmapSafe(imagePath, maxDimension = 512)
            }
        }
    }

    val initials = remember(studentName) {
        val parts = studentName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "AU"
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer Iridescent Halo Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(10.dp, CircleShape, spotColor = Color(0x880A84FF))
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF5CE1FF),
                            Color(0xFF0A84FF),
                            Color(0xFF818CF8),
                            Color(0xFF2DD4BF),
                            Color(0xFF5CE1FF)
                        )
                    )
                )
                .padding(2.2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF152647),
                                Color(0xFF0A84FF),
                                Color(0xFF3730A3)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val loaded = bitmap
                if (loaded != null) {
                    Image(
                        bitmap = loaded.asImageBitmap(),
                        contentDescription = "$studentName profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (size.value * 0.34f).sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        if (showCameraBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.36f).coerceIn(18.dp, 28.dp))
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(auraColors.secondaryAccent, auraColors.primaryAccent)
                        )
                    )
                    .border(1.4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change profile photo",
                    tint = Color.White,
                    modifier = Modifier.size((size * 0.19f).coerceIn(10.dp, 15.dp))
                )
            }
        }
    }
}

/**
 * 28-Day Exam Readiness & Study Consistency Heatmap Card
 * Combines SM-2 flashcard retention, deep focus minutes, and daily academic habits into a glowing 4x7 matrix.
 */
@Composable
fun ExamReadinessHeatmapCard(
    habits: List<AcademicHabitEntity>,
    focusRecords: List<FocusRecordEntity>,
    flashcards: List<FlashcardEntity>,
    modifier: Modifier = Modifier,
    testTag: String = "exam_readiness_heatmap_card"
) {
    val auraColors = LocalAuraColors.current
    val today = remember { LocalDate.now() }

    val dayScores = remember(habits, focusRecords, flashcards, today) {
        val habitByDate = habits.associateBy { it.dateString }
        val focusByDate = focusRecords.groupBy { rec ->
            runCatching {
                Instant.ofEpochMilli(rec.timestampEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
            }.getOrDefault("")
        }
        (27 downTo 0).map { daysAgo ->
            val dateStr = today.minusDays(daysAgo.toLong()).toString()
            val h = habitByDate[dateStr]
            val focusMins = (focusByDate[dateStr]?.sumOf { it.durationMinutes } ?: 0) + (h?.focusMinutesLogged ?: 0)
            var intensity = 0
            if (h != null) {
                if (h.attendedLectures) intensity += 1
                if (h.flashcardsReviewed) intensity += 1
                if (h.syllabusProgressMade) intensity += 1
                if (h.deepFocusCompleted) intensity += 1
            }
            if (focusMins >= 25) intensity += 1
            if (focusMins >= 60) intensity += 1
            intensity.coerceIn(0, 4)
        }
    }

    val readinessScore = remember(dayScores, flashcards) {
        val activeDays = dayScores.count { it > 0 }
        val consistencyPart = ((activeDays / 14f).coerceAtMost(1f) * 50f).toInt()
        val matureCards = flashcards.count { it.repetitions >= 2 || it.boxLevel >= 3 }
        val cardPart = if (flashcards.isEmpty()) 28 else ((matureCards.toFloat() / flashcards.size) * 50f).toInt().coerceAtLeast(18)
        (consistencyPart + cardPart).coerceIn(15, 100)
    }

    AuraCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        accentGlow = auraColors.secondaryAccent,
        testTag = testTag
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "28-Day Exam Readiness Heatmap",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = auraColors.textPrimary
                    )
                    Text(
                        text = "${dayScores.count { it > 0 }} active study days • ${flashcards.count { it.intervalDays >= 3 }} mature SM-2 cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = auraColors.textSecondary,
                        fontSize = 11.sp
                    )
                }
                AuraBadge(
                    text = "$readinessScore% READY",
                    backgroundColor = auraColors.success.copy(alpha = 0.22f),
                    textColor = auraColors.success
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                dayScores.chunked(7).forEach { weekSlice ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        weekSlice.forEach { level ->
                            val cellColor = when (level) {
                                0 -> Color(0x1AFFFFFF)
                                1 -> Color(0x5538BDF8)
                                2 -> Color(0x880A84FF)
                                3 -> Color(0xCC2DD4BF)
                                else -> Color(0xFF34D399)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(cellColor)
                                    .border(
                                        0.7.dp,
                                        if (level > 0) Color(0x66FFFFFF) else Color(0x18FFFFFF),
                                        RoundedCornerShape(5.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

