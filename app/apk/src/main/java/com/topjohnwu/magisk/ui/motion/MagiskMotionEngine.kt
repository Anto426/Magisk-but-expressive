package com.topjohnwu.magisk.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.topjohnwu.magisk.utils.AccessibilityUtils

enum class MagiskMotionDuration(val millis: Int) {
    Instant(0),
    Short(140),
    Medium(220),
    Long(320)
}

@Stable
data class MagiskMotionProfile(
    val enabled: Boolean = true
) {
    fun duration(token: MagiskMotionDuration): Int {
        return if (enabled) token.millis else 0
    }
}

val LocalMagiskMotionProfile = compositionLocalOf { MagiskMotionProfile() }

object MotionCenter {

    @Composable
    fun profile(): MagiskMotionProfile = LocalMagiskMotionProfile.current

    @Composable
    fun <T> tweenSpec(duration: MagiskMotionDuration = MagiskMotionDuration.Medium): FiniteAnimationSpec<T> {
        val profile = profile()
        return if (profile.enabled) {
            tween(durationMillis = profile.duration(duration), easing = EaseInOutCubic)
        } else {
            snap()
        }
    }

    @Composable
    fun fadeEnter(duration: MagiskMotionDuration = MagiskMotionDuration.Short): EnterTransition {
        val profile = profile()
        return if (profile.enabled) fadeIn(tween(profile.duration(duration))) else EnterTransition.None
    }

    @Composable
    fun fadeExit(duration: MagiskMotionDuration = MagiskMotionDuration.Short): ExitTransition {
        val profile = profile()
        return if (profile.enabled) fadeOut(tween(profile.duration(duration))) else ExitTransition.None
    }

    @Composable
    fun expandEnter(duration: MagiskMotionDuration = MagiskMotionDuration.Medium): EnterTransition {
        val profile = profile()
        return if (profile.enabled) {
            fadeIn(tween(profile.duration(MagiskMotionDuration.Short))) +
                    expandVertically(animationSpec = tween(profile.duration(duration)))
        } else {
            EnterTransition.None
        }
    }

    @Composable
    fun shrinkExit(duration: MagiskMotionDuration = MagiskMotionDuration.Short): ExitTransition {
        val profile = profile()
        return if (profile.enabled) {
            fadeOut(tween(profile.duration(duration))) +
                    shrinkVertically(animationSpec = tween(profile.duration(duration)))
        } else {
            ExitTransition.None
        }
    }

    @Composable
    fun contentTransform(): ContentTransform {
        val profile = profile()
        return if (profile.enabled) {
            fadeIn(tween(profile.duration(MagiskMotionDuration.Short)))
                .togetherWith(fadeOut(tween(profile.duration(MagiskMotionDuration.Short))))
        } else {
            EnterTransition.None.togetherWith(ExitTransition.None)
        }
    }

    @Composable
    fun scaleEnter(duration: MagiskMotionDuration = MagiskMotionDuration.Short): EnterTransition {
        val profile = profile()
        return if (profile.enabled) {
            fadeIn(tween(profile.duration(duration))) +
                    scaleIn(
                        initialScale = 0.82f,
                        animationSpec = tween(profile.duration(duration), easing = EaseInOutCubic)
                    )
        } else {
            EnterTransition.None
        }
    }

    @Composable
    fun scaleExit(duration: MagiskMotionDuration = MagiskMotionDuration.Short): ExitTransition {
        val profile = profile()
        return if (profile.enabled) {
            fadeOut(tween(profile.duration(duration))) +
                    scaleOut(
                        targetScale = 0.82f,
                        animationSpec = tween(profile.duration(duration), easing = EaseInOutCubic)
                    )
        } else {
            ExitTransition.None
        }
    }

    @Composable
    fun iconTransform(): ContentTransform {
        return scaleEnter().togetherWith(scaleExit())
    }

    fun navigationEnter(isPop: Boolean, enabled: Boolean): EnterTransition {
        if (!enabled) return EnterTransition.None
        return if (isPop) {
            slideInHorizontally(
                initialOffsetX = { -(it * 0.15f).toInt() },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                initialAlpha = 0.5f,
                animationSpec = tween(50, delayMillis = 66, easing = LinearEasing)
            )
        } else {
            slideInHorizontally(
                initialOffsetX = { (it * 0.15f).toInt() },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeIn(
                initialAlpha = 0.5f,
                animationSpec = tween(200, delayMillis = 66, easing = LinearEasing)
            )
        }
    }

    fun navigationExit(isPop: Boolean, enabled: Boolean): ExitTransition {
        if (!enabled) return ExitTransition.None
        return if (isPop) {
            slideOutHorizontally(
                targetOffsetX = { (it * 0.10f).toInt() },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(50, easing = LinearEasing)
            )
        } else {
            slideOutHorizontally(
                targetOffsetX = { -(it * 0.10f).toInt() },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(50, easing = LinearEasing)
            )
        }
    }
}

@Composable
fun ProvideMotionCenter(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val profile = remember(context) {
        MagiskMotionProfile(
            enabled = AccessibilityUtils.isAnimationEnabled(context.contentResolver)
        )
    }
    CompositionLocalProvider(
        LocalMagiskMotionProfile provides profile,
        content = content
    )
}

@Composable
fun MagiskAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = MotionCenter.expandEnter(),
        exit = MotionCenter.shrinkExit()
    ) {
        content()
    }
}

@Composable
fun <T> MagiskAnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val transform = MotionCenter.contentTransform()
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = { transform },
        label = "MagiskAnimatedContent",
    ) { value ->
        content(value)
    }
}

@Composable
fun MagiskAutoScrollToLatest(
    itemCount: Int,
    state: LazyListState,
    enabled: Boolean = true,
    always: Boolean = false,
    trailingThreshold: Int = 4
) {
    val profile = MotionCenter.profile()
    LaunchedEffect(itemCount, enabled, always, profile.enabled) {
        val last = itemCount - 1
        if (!enabled || last < 0) return@LaunchedEffect
        val closeToEnd =
            always || !state.canScrollForward || state.firstVisibleItemIndex >= last - trailingThreshold
        if (closeToEnd) {
            if (profile.enabled) {
                state.animateScrollToItem(last)
            } else {
                state.scrollToItem(last)
            }
        }
    }
}
