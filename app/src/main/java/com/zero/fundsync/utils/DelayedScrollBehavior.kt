package com.zero.fundsync.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.appbar.AppBarLayout

class DelayedScrollBehavior(context: Context, attrs: AttributeSet?) : AppBarLayout.Behavior(context, attrs) {
    
    companion object {
        private const val SCROLL_THRESHOLD = 100 // Pixels to scroll before collapsing starts
    }
    
    private var scrolled = 0
    
    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        scrolled = 0
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }
    
    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        scrolled += dyConsumed
        if (scrolled > SCROLL_THRESHOLD) {
            super.onNestedScroll(
                coordinatorLayout,
                child,
                target,
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed,
                type,
                consumed
            )
        }
    }
    
    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (scrolled > SCROLL_THRESHOLD || dy < 0) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        }
    }
} 