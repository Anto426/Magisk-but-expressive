package com.topjohnwu.magisk.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState as composeAnimateColorAsState
import androidx.compose.animation.core.animateDpAsState as composeAnimateDpAsState
import androidx.compose.animation.core.animateFloatAsState as composeAnimateFloatAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object MagiskMotion {
    @Composable
    fun animateColor(
        targetValue: Color,
        animationSpec: AnimationSpec<Color> = colorSpec(),
        label: String = "color"
    ): State<Color> = composeAnimateColorAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        label = label
    )

    @Composable
    fun animateFloat(
        targetValue: Float,
        animationSpec: AnimationSpec<Float> = standardTween(),
        label: String = "float"
    ): State<Float> = composeAnimateFloatAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        label = label
    )

    @Composable
    fun animateDp(
        targetValue: Dp,
        animationSpec: AnimationSpec<Dp> = standardTween(),
        label: String = "dp"
    ): State<Dp> = composeAnimateDpAsState(
        targetValue = targetValue,
        animationSpec = animationSpec,
        label = label
    )

    @Composable
    fun animateSelectionScale(
        selected: Boolean,
        selectedScale: Float = 1.06f,
        label: String = "selectionScale"
    ): State<Float> = animateFloat(
        targetValue = if (selected) selectedScale else 1f,
        animationSpec = selectionSpring(),
        label = label
    )

    @Composable
    fun animateSelectionBorderWidth(
        selected: Boolean,
        selectedWidth: Dp = 3.dp,
        label: String = "selectionBorderWidth"
    ): State<Dp> = animateDp(
        targetValue = if (selected) selectedWidth else 0.dp,
        animationSpec = selectionSpring(),
        label = label
    )

    fun <T> contentSizeSpring(): SpringSpec<T> = spring(
        dampingRatio = MotionTokens.DampingNoBounce,
        stiffness = MotionTokens.StiffnessLow
    )

    fun <T> cardContentSpring(): SpringSpec<T> = spring(
        dampingRatio = MotionTokens.DampingNoBounce,
        stiffness = MotionTokens.StiffnessMediumLow
    )

    fun <T> selectionSpring(): SpringSpec<T> = spring(
        dampingRatio = MotionTokens.DampingMediumBouncy,
        stiffness = MotionTokens.StiffnessMedium
    )

    fun <T> noBounceSpring(
        stiffness: Float = MotionTokens.StiffnessLow
    ): SpringSpec<T> = spring(
        dampingRatio = MotionTokens.DampingNoBounce,
        stiffness = stiffness
    )

    fun <T> lowBounceSpring(
        stiffness: Float = MotionTokens.StiffnessMediumLow
    ): SpringSpec<T> = spring(
        dampingRatio = MotionTokens.DampingLowBouncy,
        stiffness = stiffness
    )

    fun <T> standardTween(
        durationMillis: Int = MotionTokens.DurationStandard,
        delayMillis: Int = 0,
        easing: Easing = FastOutSlowInEasing
    ): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = easing
    )

    fun <T> mediumTween(
        delayMillis: Int = 0,
        easing: Easing = FastOutSlowInEasing
    ): TweenSpec<T> = standardTween(
        durationMillis = MotionTokens.DurationMedium,
        delayMillis = delayMillis,
        easing = easing
    )

    fun <T> quickTween(
        durationMillis: Int = MotionTokens.DurationQuick,
        easing: Easing = FastOutLinearInEasing
    ): TweenSpec<T> = standardTween(
        durationMillis = durationMillis,
        easing = easing
    )

    fun colorSpec(): AnimationSpec<Color> = standardTween(
        durationMillis = MotionTokens.DurationEmphasized
    )

    fun quickColorSpec(): AnimationSpec<Color> = standardTween(
        durationMillis = MotionTokens.DurationMenu
    )

    fun fadeContent(): ContentTransform = fadeIn(
        animationSpec = mediumTween()
    ) togetherWith fadeOut(
        animationSpec = quickTween()
    )

    fun floatingBarEnter(): EnterTransition = fadeIn(
        animationSpec = standardTween()
    ) + slideInVertically(
        animationSpec = standardTween()
    ) { it / 2 }

    fun floatingBarExit(): ExitTransition = fadeOut(
        animationSpec = mediumTween()
    ) + slideOutVertically(
        animationSpec = mediumTween()
    ) { it / 2 }

    fun expandedControlEnter(): EnterTransition = fadeIn(
        animationSpec = mediumTween(delayMillis = MotionTokens.DelaySm)
    ) + scaleIn(
        initialScale = 0.85f,
        animationSpec = noBounceSpring(stiffness = MotionTokens.StiffnessMediumLow)
    )

    fun expandedControlExit(): ExitTransition = fadeOut(
        animationSpec = quickTween()
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = quickTween()
    )

    fun expandedContentEnter(): EnterTransition = expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = noBounceSpring(stiffness = MotionTokens.StiffnessLow)
    ) + slideInVertically(
        initialOffsetY = { it / 10 },
        animationSpec = standardTween(durationMillis = MotionTokens.DurationExpand)
    ) + fadeIn(
        animationSpec = standardTween(
            delayMillis = MotionTokens.DelayXs
        )
    )

    fun expandedContentExit(): ExitTransition = shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = standardTween(
            durationMillis = MotionTokens.DurationCollapse,
            easing = FastOutLinearInEasing
        )
    ) + fadeOut(
        animationSpec = quickTween()
    )

    fun expandablePanelEnter(): EnterTransition = expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = noBounceSpring(stiffness = MotionTokens.StiffnessLow)
    ) + fadeIn(
        animationSpec = mediumTween(delayMillis = MotionTokens.DelayXs)
    )

    fun expandablePanelExit(): ExitTransition = shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = standardTween(
            durationMillis = MotionTokens.DurationCollapse,
            easing = FastOutLinearInEasing
        )
    ) + fadeOut(
        animationSpec = quickTween()
    )

    fun simpleExpandEnter(): EnterTransition = expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = noBounceSpring(stiffness = MotionTokens.StiffnessLow)
    ) + fadeIn(
        animationSpec = standardTween()
    )

    fun simpleExpandExit(): ExitTransition = shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = standardTween(durationMillis = MotionTokens.DurationCollapse)
    ) + fadeOut(
        animationSpec = quickTween()
    )

    fun fadeVisibilityEnter(
        delayMillis: Int = 0
    ): EnterTransition = fadeIn(
        animationSpec = mediumTween(delayMillis = delayMillis)
    )

    fun fadeVisibilityExit(): ExitTransition = fadeOut(
        animationSpec = quickTween()
    )

    fun fabEnter(): EnterTransition = scaleIn(
        animationSpec = cardContentSpring()
    ) + fadeIn(
        animationSpec = standardTween()
    )

    fun fabExit(): ExitTransition = scaleOut(
        animationSpec = quickTween()
    ) + fadeOut(
        animationSpec = quickTween()
    )

    private fun trailingActionEnter(delayMillis: Int): EnterTransition = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 3 },
        animationSpec = standardTween(delayMillis = delayMillis)
    ) + fadeIn(
        animationSpec = mediumTween(delayMillis = delayMillis)
    )

    fun staggeredTrailingActionEnter(position: Int): EnterTransition = trailingActionEnter(
        delayMillis = when (position) {
            1 -> MotionTokens.Stagger1
            2 -> MotionTokens.Stagger2
            3 -> MotionTokens.Stagger3
            else -> 0
        }
    )

    fun trailingActionExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth / 3 },
        animationSpec = quickTween()
    ) + fadeOut(
        animationSpec = quickTween(durationMillis = MotionTokens.DurationTiny)
    )

    fun selectionIndicatorEnter(): EnterTransition = fadeIn(
        animationSpec = standardTween(durationMillis = MotionTokens.DurationSelectionIn)
    ) + scaleIn(
        initialScale = 0.3f,
        animationSpec = selectionSpring()
    )

    fun selectionIndicatorExit(): ExitTransition = fadeOut(
        animationSpec = quickTween(durationMillis = MotionTokens.DurationSelectionOut)
    ) + scaleOut(
        targetScale = 0.3f,
        animationSpec = quickTween(durationMillis = MotionTokens.DurationSelectionOut)
    )

    fun routeEnter(
        _scope: AnimatedContentTransitionScope<*>,
        direction: AnimatedContentTransitionScope.SlideDirection,
        rootTabDistance: Int?
    ): EnterTransition {
        if (rootTabDistance == 0) return EnterTransition.None
        val offsetDivisor = if (rootTabDistance == null) 2 else 3
        return slideInHorizontally(
            initialOffsetX = {
                val offset = it / offsetDivisor
                if (direction == AnimatedContentTransitionScope.SlideDirection.Left) {
                    offset
                } else {
                    -offset
                }
            },
            animationSpec = standardTween(durationMillis = MotionTokens.DurationRouteEnter)
        ) + fadeIn(
            animationSpec = standardTween(
                durationMillis = MotionTokens.DurationRouteEnter,
                easing = LinearOutSlowInEasing
            )
        )
    }

    fun routeExit(
        _scope: AnimatedContentTransitionScope<*>,
        direction: AnimatedContentTransitionScope.SlideDirection,
        rootTabDistance: Int?
    ): ExitTransition {
        if (rootTabDistance == 0) return ExitTransition.None
        val offsetDivisor = if (rootTabDistance == null) 2 else 3
        return slideOutHorizontally(
            targetOffsetX = {
                val offset = it / offsetDivisor
                if (direction == AnimatedContentTransitionScope.SlideDirection.Left) {
                    -offset
                } else {
                    offset
                }
            },
            animationSpec = standardTween(
                durationMillis = MotionTokens.DurationRouteExit,
                easing = FastOutLinearInEasing
            )
        ) + fadeOut(
            animationSpec = standardTween(
                durationMillis = MotionTokens.DurationRouteExit,
                easing = FastOutLinearInEasing
            )
        )
    }

    suspend fun scrollToItem(
        listState: LazyListState,
        index: Int
    ) {
        listState.animateScrollToItem(index)
    }

}
