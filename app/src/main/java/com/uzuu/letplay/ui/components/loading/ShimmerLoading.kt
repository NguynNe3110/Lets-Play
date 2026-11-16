package com.uzuu.letplay.ui.components.loading

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal allowing UI components or tests to override motion reduction policy.
 * When true, shimmering animations are replaced by static skeleton placeholders (P9.1).
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Checks whether system motion reduction is active, either through [LocalReducedMotion]
 * or via system-level animation scale settings.
 */
@Composable
fun isReducedMotion(): Boolean {
    val override = LocalReducedMotion.current
    if (override) return true
    val context = LocalContext.current
    return remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Remembers a native linear gradient [Brush] for shimmer animations (P9.1).
 * Uses [rememberInfiniteTransition] and [animateFloat] across a linear gradient.
 *
 * Automatically falls back to a static skeleton background when [enableShimmer] is false
 * or when motion reduction is detected.
 *
 * @param colors Optional list of gradient colors (start, middle highlight, end).
 * @param enableShimmer If false, renders a static solid skeleton brush without running animations.
 */
@Composable
fun rememberShimmerBrush(
    colors: List<Color>? = null,
    enableShimmer: Boolean = true
): Brush {
    val reducedMotion = isReducedMotion()
    val isDark = isSystemInDarkTheme()

    val defaultColors = if (isDark) {
        listOf(
            Color(0xFF2C2C2C),
            Color(0xFF3D3D3D),
            Color(0xFF2C2C2C)
        )
    } else {
        listOf(
            Color(0xFFE2E2E2),
            Color(0xFFF4F4F4),
            Color(0xFFE2E2E2)
        )
    }
    val effectiveColors = colors ?: defaultColors

    if (!enableShimmer || reducedMotion) {
        return Brush.linearGradient(
            colors = listOf(effectiveColors.first(), effectiveColors.first())
        )
    }

    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = effectiveColors,
        start = Offset(translateAnim - 600f, translateAnim - 600f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Modifier that clips to [shape] and draws the shimmer or static skeleton [brush].
 * Sets clear semantics so accessibility TalkBack announces "Loading" instead of blank decoration.
 */
fun Modifier.shimmer(
    shape: Shape = RoundedCornerShape(4.dp),
    brush: Brush
): Modifier = this
    .clip(shape)
    .background(brush)
    .clearAndSetSemantics {
        contentDescription = "Loading"
    }

/**
 * Primitive skeleton rectangular or rounded box placeholder.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .clearAndSetSemantics {
                contentDescription = "Loading"
            }
    )
}

/**
 * Primitive skeleton text line placeholder.
 */
@Composable
fun SkeletonTextLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(brush)
            .clearAndSetSemantics {
                contentDescription = "Loading"
            }
    )
}

/**
 * Primitive skeleton thumbnail placeholder.
 */
@Composable
fun SkeletonThumbnail(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(brush)
            .clearAndSetSemantics {
                contentDescription = "Loading"
            }
    )
}

/**
 * Primitive skeleton card container placeholder.
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    brush: Brush = rememberShimmerBrush(),
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        content()
    }
}

/**
 * Skeleton card placeholder matching the visual hierarchy of [ProductItemCard] (P9.1).
 */
@Composable
fun ProductCardSkeleton(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFF9F9F9)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Thumbnail image placeholder
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f),
                shape = RoundedCornerShape(8.dp),
                brush = brush
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category badge & inStock indicator row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(64.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(4.dp),
                    brush = brush
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(4.dp),
                    brush = brush
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title lines
            SkeletonTextLine(
                modifier = Modifier.fillMaxWidth(0.9f),
                height = 16.dp,
                brush = brush
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonTextLine(
                modifier = Modifier.fillMaxWidth(0.6f),
                height = 16.dp,
                brush = brush
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price & rating row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp),
                    brush = brush
                )
                SkeletonBox(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(4.dp),
                    brush = brush
                )
            }
        }
    }
}

/**
 * Grid of product card skeletons for initial loading state (P9.1).
 */
@Composable
fun ProductGridSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 6,
    brush: Brush = rememberShimmerBrush()
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count) {
            ProductCardSkeleton(brush = brush)
        }
    }
}
