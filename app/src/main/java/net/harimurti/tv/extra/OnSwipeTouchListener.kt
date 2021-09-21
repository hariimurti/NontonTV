package net.harimurti.tv.extra

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import net.harimurti.tv.App
import kotlin.math.abs

open class OnSwipeTouchListener(private val view: View): View.OnTouchListener {
    private val context = App.context
    private val gestureDetector = GestureDetector(context, GestureListener())

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(motionEvent)
        return false
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val mHandler = Handler(Looper.getMainLooper())
        private val mRunnable = Runnable {
            isDoubleTapping = false
            clicksDouble = if(isLeft) clicksLeft else clicksRight
            onTapDoubleFinish(clicksDouble,isLeft)
            clicksDouble = 0
            clicksLeft = 0
            clicksRight = 0
        }
        private var isDoubleTapping = false
        private var doubleTapDelay: Long = 2000
        private var clicksDouble = 0
        private var clicksLeft = 0
        private var clicksRight = 0
        private var isLeft = false

        fun keepInDoubleTapMode() {
            isDoubleTapping = true
            mHandler.removeCallbacks(mRunnable)
            mHandler.postDelayed(mRunnable, doubleTapDelay)
        }

        override fun onDown(e: MotionEvent): Boolean {
            if (isDoubleTapping) {
                return true
            }
            return super.onDown(e)
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeDown()
                        } else {
                            onSwipeUp()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            return result
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            //doubleTap start
            if (!isDoubleTapping) {
                keepInDoubleTapMode()
            }
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            if (e.actionMasked == MotionEvent.ACTION_UP && isDoubleTapping) {
                keepInDoubleTapMode()
                if (e.x < view.width / 2) {
                    clicksLeft++
                    isLeft = true
                    onTapDoubleLeft(clicksLeft)
                } else{
                    clicksRight++
                    isLeft = false
                    onTapDoubleRight(clicksRight)
                }
                return true
            }
            return super.onDoubleTapEvent(e)
        }

        //when doubleTap start, single tap run as doubleTap seek
        /*override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (isDoubleTapping) {
                keepInDoubleTapMode()
                if (e.x < view.width / 2) {
                    clicksLeft++
                    isLeft = true
                    onTapDoubleLeft(clicksLeft)
                } else{
                    clicksRight++
                    isLeft = false
                    onTapDoubleRight(clicksRight)
                }
                return true
            }
            return super.onSingleTapUp(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (isDoubleTapping) return true
            return super.onSingleTapConfirmed(e)
        }*/
    }

    open fun onSwipeRight() {}

    open fun onSwipeLeft() {}

    open fun onSwipeUp() {}

    open fun onSwipeDown() {}

    open fun onTapDoubleLeft(click: Int) {}

    open fun onTapDoubleRight(click: Int) {}

    open fun onTapDoubleFinish(click: Int,isLeft: Boolean) {}
}