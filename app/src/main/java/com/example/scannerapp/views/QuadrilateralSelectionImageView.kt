package com.example.scannerapp.views


import android.content.Context
import android.view.MotionEvent


import android.graphics.*

import android.util.AttributeSet
import android.widget.ImageView

import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.scannerapp.R



class QuadrilateralSelectionImageView : androidx.appcompat.widget.AppCompatImageView {
    private var mBackgroundPaint: Paint? = null
    private var mBorderPaint: Paint? = null
    private var mCirclePaint: Paint? = null
    private var mSelectionPath: Path? = null
    private var mBackgroundPath: Path? = null
    private var mUpperLeftPoint: PointF? = null
    private var mUpperRightPoint: PointF? = null
    private var mLowerLeftPoint: PointF? = null
    private var mLowerRightPoint: PointF? = null
    private var mLastTouchedPoint: PointF? = null

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        mBackgroundPaint = Paint()
        mBackgroundPaint?.color = -0x80000000
        mBorderPaint = Paint()
        mBorderPaint?.color = ContextCompat.getColor(context, R.color.black)
        mBorderPaint?.isAntiAlias = true
        mBorderPaint?.style = Paint.Style.STROKE
        mBorderPaint?.strokeWidth = 8f
        mCirclePaint = Paint()
        mCirclePaint?.color = ContextCompat.getColor(context, R.color.cardview_dark_background)
        mCirclePaint?.isAntiAlias = true
        mCirclePaint?.style = Paint.Style.STROKE
        mCirclePaint?.strokeWidth = 8f
        mSelectionPath = Path()
        mBackgroundPath = Path()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mUpperLeftPoint == null || mUpperRightPoint == null || mLowerRightPoint == null || mLowerLeftPoint == null) {
            setDefaultSelection()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mSelectionPath?.reset()
        mSelectionPath?.fillType = Path.FillType.EVEN_ODD
        mSelectionPath?.moveTo(mUpperLeftPoint!!.x, mUpperLeftPoint!!.y)
        mSelectionPath?.lineTo(mUpperRightPoint!!.x, mUpperRightPoint!!.y)
        mSelectionPath?.lineTo(mLowerRightPoint!!.x, mLowerRightPoint!!.y)
        mSelectionPath?.lineTo(mLowerLeftPoint!!.x, mLowerLeftPoint!!.y)
        mSelectionPath?.close()
        mBackgroundPath?.reset()
        mBackgroundPath?.fillType = Path.FillType.EVEN_ODD
        mBackgroundPath?.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        mBackgroundPath?.addPath(mSelectionPath!!)
        canvas.drawPath(mBackgroundPath!!, mBackgroundPaint!!)
        canvas.drawPath(mSelectionPath!!, mBorderPaint!!)
        canvas.drawCircle(mUpperLeftPoint!!.x, mUpperLeftPoint!!.y, 30f, mCirclePaint!!)
        canvas.drawCircle(mUpperRightPoint!!.x, mUpperRightPoint!!.y, 30f, mCirclePaint!!)
        canvas.drawCircle(mLowerRightPoint!!.x, mLowerRightPoint!!.y, 30f, mCirclePaint!!)
        canvas.drawCircle(mLowerLeftPoint!!.x, mLowerLeftPoint!!.y, 30f, mCirclePaint!!)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                var isConvex = false
                val eventPoint = PointF(event.x, event.y)

                // Determine if the shape will still be convex when we apply the users next drag
                when {
                    mLastTouchedPoint === mUpperLeftPoint -> {
                        isConvex = isConvexQuadrilateral(
                            eventPoint,
                            mUpperRightPoint,
                            mLowerRightPoint,
                            mLowerLeftPoint
                        )
                    }
                    mLastTouchedPoint === mUpperRightPoint -> {
                        isConvex = isConvexQuadrilateral(
                            mUpperLeftPoint,
                            eventPoint,
                            mLowerRightPoint,
                            mLowerLeftPoint
                        )
                    }
                    mLastTouchedPoint === mLowerRightPoint -> {
                        isConvex = isConvexQuadrilateral(
                            mUpperLeftPoint,
                            mUpperRightPoint,
                            eventPoint,
                            mLowerLeftPoint
                        )
                    }
                    mLastTouchedPoint === mLowerLeftPoint -> {
                        isConvex = isConvexQuadrilateral(
                            mUpperLeftPoint,
                            mUpperRightPoint,
                            mLowerRightPoint,
                            eventPoint
                        )
                    }
                }
                if (isConvex && mLastTouchedPoint != null) {
                    mLastTouchedPoint!![event.x] = event.y
                }
            }
            MotionEvent.ACTION_DOWN -> {
                val p = 100
                mLastTouchedPoint =
                    if (event.x < mUpperLeftPoint!!.x + p && event.x > mUpperLeftPoint!!.x - p && event.y < mUpperLeftPoint!!.y + p && event.y > mUpperLeftPoint!!.y - p) {
                        mUpperLeftPoint
                    } else if (event.x < mUpperRightPoint!!.x + p && event.x > mUpperRightPoint!!.x - p && event.y < mUpperRightPoint!!.y + p && event.y > mUpperRightPoint!!.y - p) {
                        mUpperRightPoint
                    } else if (event.x < mLowerRightPoint!!.x + p && event.x > mLowerRightPoint!!.x - p && event.y < mLowerRightPoint!!.y + p && event.y > mLowerRightPoint!!.y - p) {
                        mLowerRightPoint
                    } else if (event.x < mLowerLeftPoint!!.x + p && event.x > mLowerLeftPoint!!.x - p && event.y < mLowerLeftPoint!!.y + p && event.y > mLowerLeftPoint!!.y - p) {
                        mLowerLeftPoint
                    } else {
                        null
                    }
            }
        }
        invalidate()
        return true
    }

    /**
     * Translate the given point from view coordinates to image coordinates
     *
     * @param point The point to translate
     * @return The translated point
     */
    private fun viewPointToImagePoint(point: PointF?): PointF? {
        val matrix = Matrix()
        imageMatrix.invert(matrix)
        return mapPointToMatrix(point, matrix)
    }

    /**
     * Translate the given point from image coordinates to view coordinates
     *
     * @param imgPoint The point to translate
     * @return The translated point
     */
    private fun imagePointToViewPoint(imgPoint: PointF?): PointF? {
        return mapPointToMatrix(imgPoint, imageMatrix)
    }

    /**
     * Helper to map a given PointF to a given Matrix
     *
     * NOTE: http://stackoverflow.com/questions/19958256/custom-imageview-imagematrix-mappoints-and-invert-inaccurate
     *
     * @param point The point to map
     * @param matrix The matrix
     * @return The mapped point
     */
    private fun mapPointToMatrix(point: PointF?, matrix: Matrix): PointF? {
        val points = floatArrayOf(point!!.x, point.y)
        matrix.mapPoints(points)
        return if (points.size > 1) {
            PointF(points[0], points[1])
        } else {
            null
        }
    }
    /**
     * Returns a list of points representing the quadrilateral.  The points are converted to represent
     * the location on the image itself, not the view.
     *
     * @return A list of points translated to map to the image
     */
    /**
     * Set the points in order to control where the selection will be drawn.  The points should
     * be represented in regards to the image, not the view.  This method will translate from image
     * coordinates to view coordinates.
     *
     * NOTE: Calling this method will invalidate the view
     *
     * @param points A list of points. Passing null will set the selector to the default selection.
     */
    var points: List<PointF?>?
        get() {
            val list: MutableList<PointF?> = ArrayList()
            list.add(viewPointToImagePoint(mUpperLeftPoint))
            list.add(viewPointToImagePoint(mUpperRightPoint))
            list.add(viewPointToImagePoint(mLowerRightPoint))
            list.add(viewPointToImagePoint(mLowerLeftPoint))
            return list
        }
        set(points) {
            if (points != null) {
                mUpperLeftPoint = imagePointToViewPoint(points[0])
                mUpperRightPoint = imagePointToViewPoint(points[1])
                mLowerRightPoint = imagePointToViewPoint(points[2])
                mLowerLeftPoint = imagePointToViewPoint(points[3])
            } else {
                setDefaultSelection()
            }
            invalidate()
        }

    /**
     * Gets the coordinates representing a rectangles corners.
     *
     * The order of the points is
     * 0------->1
     * ^        |
     * |        v
     * 3<-------2
     *
     * @param rect The rectangle
     * @return An array of 8 floats
     */
    private fun getCornersFromRect(rect: RectF): FloatArray {
        return floatArrayOf(
            rect.left, rect.top,
            rect.right, rect.top,
            rect.right, rect.bottom,
            rect.left, rect.bottom
        )
    }

    /**
     * Sets the points into a default state (A rectangle following the image view frame with
     * padding)
     */
    private fun setDefaultSelection() {
        val rect = RectF()
        val padding = 100f
        rect.right = width - padding
        rect.bottom = height - padding
        rect.top = padding
        rect.left = padding
        val pts = getCornersFromRect(rect)
        mUpperLeftPoint = PointF(pts[0], pts[1])
        mUpperRightPoint = PointF(pts[2], pts[3])
        mLowerRightPoint = PointF(pts[4], pts[5])
        mLowerLeftPoint = PointF(pts[6], pts[7])
    }

    /**
     * Determine if the given points are a convex quadrilateral.  This is used to prevent the
     * selection from being dragged into an invalid state.
     *
     * @param ul The upper left point
     * @param ur The upper right point
     * @param lr The lower right point
     * @param ll The lower left point
     * @return True is the quadrilateral is convex
     */
    private fun isConvexQuadrilateral(ul: PointF?, ur: PointF?, lr: PointF?, ll: PointF?): Boolean {
        // http://stackoverflow.com/questions/9513107/find-if-4-points-form-a-quadrilateral
        val r = subtractPoints(ur, ll)
        val s = subtractPoints(ul, lr)
        val s_r_crossProduct = crossProduct(r, s).toDouble()
        val t = crossProduct(subtractPoints(lr, ll), s) / s_r_crossProduct
        val u = crossProduct(subtractPoints(lr, ll), r) / s_r_crossProduct
        return !(t < 0 || t > 1.0 || u < 0 || u > 1.0)
    }

    private fun subtractPoints(p1: PointF?, p2: PointF?): PointF {
        return PointF(p1!!.x - p2!!.x, p1.y - p2.y)
    }

    private fun crossProduct(v1: PointF, v2: PointF): Float {
        return v1.x * v2.y - v1.y * v2.x
    }
}