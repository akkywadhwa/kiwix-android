/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.kiwix.kiwixmobile.core.main

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.LinearLayout
import org.kiwix.kiwixmobile.core.R.layout
import org.kiwix.kiwixmobile.core.R.styleable

class AnimatedProgressBar : LinearLayout {
  private val mPaint = Paint()
  private val mRect = Rect()
  private var mProgress = 0
  private var mBidirectionalAnimate = true
  private var mDrawWidth = 0
  private var mProgressColor = 0

  constructor(context: Context, attrs: AttributeSet) : super(
    context,
    attrs
  ) {
    init(context, attrs)
  }

  constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int
  ) : super(context, attrs, defStyleAttr) {
    init(context, attrs)
  }

  /**
   * Initialize the AnimatedProgressBar
   *
   * @param context is the context passed by the constructor
   * @param attrs is the attribute set passed by the constructor
   */
  @Suppress("MagicNumber")
  private fun init(
    context: Context,
    attrs: AttributeSet
  ) {
    val array = context.theme
      .obtainStyledAttributes(attrs, styleable.AnimatedProgressBar, 0, 0)
    val backgroundColor: Int
    try {
      // Retrieve the style of the progress bar that the user hopefully set
      val defaultBackgroundColor = 0x424242
      val defaultProgressColor = 0x2196f3
      backgroundColor = array.getColor(
        styleable.AnimatedProgressBar_backgroundColor, defaultBackgroundColor
      )
      mProgressColor = array.getColor(
        styleable.AnimatedProgressBar_progressColor,
        defaultProgressColor
      )
      mBidirectionalAnimate =
        array.getBoolean(styleable.AnimatedProgressBar_bidirectionalAnimate, false)
    } finally {
      array.recycle()
    }
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    inflater.inflate(layout.progress_bar, this, true)
    setBackgroundColor(backgroundColor)
  }

  /**
   * Returns the current progress value between 0 and 100
   *
   * @return progress of the view
   */
  fun getProgress() = mProgress

  // calculate amount the width has to change
  // animate the width change
  // we don't need to go any farther if the progress is unchanged
  // save the progress
  // if the we only animate the view in one direction
  // then reset the view width if it is less than the
  // previous progress
  // progress cannot be less than 0
  // Set the drawing bounds for the ProgressBar
  // progress cannot be greater than 100

  /**
   * sets the progress as an integer value between 0 and 100. Values above or below that interval
   * will be adjusted to their nearest value within the interval, i.e. setting a value of 150 will
   * have the effect of setting the progress to 100. You cannot trick us.
   *
   * @param progress an integer between 0 and 100
   */
  @Suppress("MagicNumber")
  fun setProgress(progress: Int) {
    var progress = progress
    progress = when {
      progress > 100 -> 100
      progress < 0 -> 0
      else -> progress
    }
    if (this.alpha < 1.0f) {
      fadeIn()
    }
    val mWidth = this.measuredWidth
    // Set the drawing bounds for the ProgressBar
    mRect.left = 0
    mRect.top = 0
    mRect.bottom = this.bottom - this.top
    if (progress < mProgress && !mBidirectionalAnimate) {
      // if the we only animate the view in one direction
      // then reset the view width if it is less than the
      // previous progress
      mDrawWidth = 0
    } else if (progress == mProgress) {
      // we don't need to go any farther if the progress is unchanged
      if (progress == 100) {
        fadeOut()
      }
      return
    }
    // save the progress
    mProgress = progress
    // calculate amount the width has to change
    val deltaWidth = (mWidth * mProgress / 100 - mDrawWidth)
    // animate the width change
    animateView(mDrawWidth, mWidth, deltaWidth)
  }

  @Suppress("MagicNumber")
  override fun onDraw(canvas: Canvas) {
    mPaint.color = mProgressColor
    mPaint.strokeWidth = 10f
    mRect.right = mRect.left + mDrawWidth
    canvas.drawRect(mRect, mPaint)
  }

  /**
   * private method used to create and run the animation used to change the progress
   *
   * @param initialWidth is the width at which the progress starts at
   * @param maxWidth is the maximum width (total width of the view)
   * @param deltaWidth is the amount by which the width of the progress view will change
   */
  @Suppress("MagicNumber")
  private fun animateView(
    initialWidth: Int,
    maxWidth: Int,
    deltaWidth: Int
  ) {
    val fill: Animation = object : Animation() {
      override fun applyTransformation(
        interpolatedTime: Float,
        t: Transformation
      ) {
        val width = initialWidth + (deltaWidth * interpolatedTime).toInt()
        if (width <= maxWidth) {
          mDrawWidth = width
          invalidate()
        }
        if (1.0f - interpolatedTime < 0.0005) {
          if (mProgress >= 100) {
            fadeOut()
          }
        }
      }

      override fun willChangeBounds(): Boolean = false
    }
    fill.duration = 500
    fill.interpolator = DecelerateInterpolator()
    startAnimation(fill)
  }

  /**
   * fades in the progress bar
   */
  @Suppress("MagicNumber")
  private fun fadeIn() {
    val fadeIn = ObjectAnimator.ofFloat(this, "alpha", 1.0f)
    fadeIn.duration = 200
    fadeIn.interpolator = DecelerateInterpolator()
    fadeIn.start()
  }

  /**
   * fades out the progress bar
   */
  @Suppress("MagicNumber")
  private fun fadeOut() {
    val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 0.0f)
    fadeOut.duration = 200
    fadeOut.interpolator = DecelerateInterpolator()
    fadeOut.start()
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    var state: Parcelable? = state
    if (state is Bundle) {
      val bundle = state
      mProgress = bundle.getInt("progressState")
      state = bundle.getParcelable("instanceState")
    }
    super.onRestoreInstanceState(state)
  }

  override fun onSaveInstanceState(): Parcelable {
    val bundle = Bundle()
    bundle.putParcelable("instanceState", super.onSaveInstanceState())
    bundle.putInt("progressState", mProgress)
    return bundle
  }
}
