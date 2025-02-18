package com.example.mortartargeter3

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.min

class SquareLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Choose the smaller dimension so that the layout is a square that fits in the available space.
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }
}
