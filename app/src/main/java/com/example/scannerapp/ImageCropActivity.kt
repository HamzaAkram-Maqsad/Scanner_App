package com.example.scannerapp
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import com.labters.documentscanner.libraries.PolygonView
import android.view.Gravity
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import com.example.scannerapp.helpers.MyConstants
import org.opencv.core.MatOfPoint2f
import android.graphics.RectF
import android.view.View
import org.opencv.core.Point
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.content.Intent
import android.util.Log
import com.labters.documentscanner.libraries.NativeClass


class ImageCropActivity : AppCompatActivity() {

    var holderImageCrop: FrameLayout? = null
    var imageView: ImageView? = null
    var polygonView: PolygonView? = null
    var selectedImageBitmap: Bitmap? = null
    var btnImageEnhance: Button? = null

    var nativeClass: NativeClass? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)
        initializeElement()
        btnImageEnhance?.setOnClickListener {
            MyConstants.selectedImageBitmap = getCroppedImage()

            //create new intent to start process image

            //create new intent to start process image
            val intent = Intent(applicationContext, ImageEnhanceActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onStart() {
        super.onStart()

    }

    private fun initializeElement() {
        nativeClass = NativeClass()
        btnImageEnhance = findViewById(R.id.btnImageEnhance)
        holderImageCrop = findViewById(R.id.holderImageCrop)
        imageView = findViewById(R.id.imageView)
        polygonView = findViewById(R.id.polygonView)
        holderImageCrop?.post {
            initializeCropping()
        }

    }

    private fun initializeCropping() {
        selectedImageBitmap = MyConstants.selectedImageBitmap
        MyConstants.selectedImageBitmap = null
        val scaledBitmap: Bitmap? =
            scaledBitmap(selectedImageBitmap!!, holderImageCrop!!.width, holderImageCrop!!.height)
        imageView!!.setImageBitmap(scaledBitmap)
        val tempBitmap = (imageView!!.drawable as BitmapDrawable).bitmap
        val pointFs: Map<Int, PointF> = getEdgePoints(tempBitmap) as Map<Int, PointF>
        polygonView!!.points = pointFs
        polygonView!!.visibility = View.VISIBLE
        val padding = resources.getDimension(R.dimen.scanPadding).toInt()
        val layoutParams = FrameLayout.LayoutParams(
            tempBitmap.width + 2 * padding,
            tempBitmap.height + 2 * padding
        )
        layoutParams.gravity = Gravity.CENTER
        polygonView!!.layoutParams = layoutParams
    }


    protected fun getCroppedImage(): Bitmap? {
        val points = polygonView!!.points
        val xRatio = selectedImageBitmap!!.width.toFloat() / imageView!!.width
        val yRatio = selectedImageBitmap!!.height.toFloat() / imageView!!.height
        val x1 = points[0]!!.x * xRatio
        val x2 = points[1]!!.x * xRatio
        val x3 = points[2]!!.x * xRatio
        val x4 = points[3]!!.x * xRatio
        val y1 = points[0]!!.y * yRatio
        val y2 = points[1]!!.y * yRatio
        val y3 = points[2]!!.y * yRatio
        val y4 = points[3]!!.y * yRatio
        return nativeClass!!.getScannedBitmap(selectedImageBitmap, x1, y1, x2, y2, x3, y3, x4, y4)
    }

    private fun scaledBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap? {

        val m = Matrix()
        m.setRectToRect(
            RectF(0F, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()), RectF(
                0f, 0f,
                width.toFloat(),
                height.toFloat()
            ), Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun getEdgePoints(tempBitmap: Bitmap): Map<Int, PointF>? {
        //  Log.v("aashari-tag", "getEdgePoints")
        val pointFs = getContourEdgePoints(tempBitmap)
        return orderedValidEdgePoints(tempBitmap, pointFs)
    }

    private fun getContourEdgePoints(tempBitmap: Bitmap): List<PointF> {
        //  Log.v("aashari-tag", "getContourEdgePoints")
        val point2f = nativeClass!!.getPoint(tempBitmap)
        val points: List<Point> = point2f!!.toList()
        val result: MutableList<PointF> = ArrayList()
        for (i in points.indices) {
            result.add(PointF(points[i].x.toFloat(), points[i].y.toFloat()))
        }
        return result
    }

    private fun getOutlinePoints(tempBitmap: Bitmap): Map<Int, PointF> {

        val outlinePoints: MutableMap<Int, PointF> = HashMap()
        outlinePoints[0] = PointF(0f, 0f)
        outlinePoints[1] = PointF(tempBitmap.width.toFloat(), 0f)
        outlinePoints[2] = PointF(0f, tempBitmap.height.toFloat())
        outlinePoints[3] = PointF(tempBitmap.width.toFloat(), tempBitmap.height.toFloat())
        return outlinePoints
    }

    private fun orderedValidEdgePoints(
        tempBitmap: Bitmap,
        pointFs: List<PointF>
    ): Map<Int, PointF> {
        // Log.v("aashari-tag", "orderedValidEdgePoints")
        var orderedPoints = polygonView!!.getOrderedPoints(pointFs)
        if (!polygonView!!.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap)
        }
        return orderedPoints
    }
}