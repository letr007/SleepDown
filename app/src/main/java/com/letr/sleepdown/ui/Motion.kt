package com.letr.sleepdown.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens taken from the Material 3 easing and duration specs.
 *
 * M3 Expressive moved components to the motion physics system (springs) and keeps the
 * easing/duration system for screen-level transitions, so both live here: [press] and
 * friends for direct manipulation, the tween pairs for entering and leaving the screen.
 */
internal object Motion {
    private val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    private val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    private const val Short4 = 200
    private const val Medium2 = 300
    private const val Medium4 = 400

    /** Entering the screen: slower, decelerating, so the eye can follow what appeared. */
    fun <T> enter(): TweenSpec<T> = tween(durationMillis = Medium4, easing = EmphasizedDecelerate)

    /** Leaving the screen: the spec asks exits to be faster because they need less attention. */
    fun <T> exit(): TweenSpec<T> = tween(durationMillis = Short4, easing = EmphasizedAccelerate)

    /** Starts and ends on screen. */
    fun <T> onScreen(): TweenSpec<T> = tween(durationMillis = Medium2, easing = Standard)

    /** Small utility changes such as a chevron flipping. */
    fun <T> utility(): TweenSpec<T> = tween(durationMillis = Short4, easing = Standard)

    /**
     * Direct-manipulation feedback (press, pick up, drop). M3 Expressive models these with
     * springs rather than curves, so the token is a spring, not a duration.
     */
    val press: SpringSpec<Float> = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

    /** Softer spring for elements that grow or settle without being dragged. */
    val settle: SpringSpec<Float> = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
}
