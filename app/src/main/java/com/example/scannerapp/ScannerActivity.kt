package com.example.scannerapp


import android.graphics.Bitmap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.scannerapp.views.QuadrilateralSelectionImageView
import org.opencv.imgproc.Imgproc

import org.opencv.core.Mat

import org.opencv.core.CvType
import org.opencv.core.Point
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

import org.opencv.android.Utils
import org.opencv.core.Size
import android.graphics.PointF

import org.opencv.core.MatOfPoint2f

import org.opencv.core.MatOfPoint

import timber.log.Timber
import android.provider.MediaStore

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import java.io.IOException

import org.opencv.android.LoaderCallbackInterface
import com.afollestad.materialdialogs.MaterialDialog
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog


import com.github.chrisbanes.photoview.PhotoView

import org.opencv.core.Core.perspectiveTransform


import org.opencv.android.OpenCVLoader


class ScannerActivity : AppCompatActivity() {
    private lateinit var selectedView: QuadrilateralSelectionImageView
    private lateinit var scanButton: Button

    var mBitmap: Bitmap? = null
    var mResult: Bitmap? = null
    var mResultDialog: MaterialDialog? = null
    var builder: AlertDialog.Builder? = null

    //var mResultDialog: MaterialDialog? = null
    var mOpenCVLoaderCallback: OpenCVCallback? = null
    private var dialogview: View? = null

    private val MAX_HEIGHT by lazy { 500 }

    private val PICK_IMAGE_REQUEST = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        scanButton = findViewById(R.id.scanBtn)
        selectedView = findViewById(R.id.scanImageView)

        mOpenCVLoaderCallback = object : OpenCVCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }
        dialogview = layoutInflater.inflate(R.layout.document_dialog_layout, null)
        builder = AlertDialog.Builder(this)
        builder?.setTitle("Androidly Alert")
        builder?.setMessage("We have a message")
        builder?.setView(dialogview!!)
        builder?.setPositiveButton(android.R.string.yes) { dialog, which ->
            mResult = null
        }
        builder?.setNegativeButton(android.R.string.no) { dialog, which ->
            mResult = null
            dialog.cancel()
        }

        scanButton.setOnClickListener {
            if (mBitmap != null) {
                val orig = Mat()
                val list = selectedView.points
                Utils.bitmapToMat(mBitmap, orig)
                val transformed = perspectiveTransform(orig, list as List<PointF>)
                mResult = applyThreshold(transformed!!)
                if (builder != null) {
                    val photoView = dialogview?.findViewById<PhotoView>(R.id.photo_view)
                    photoView?.setImageBitmap(mResult!!)

                    builder?.show()
                }
                orig.release()
                transformed!!.release()
            }
        }

        findViewById<Button>(R.id.cameraBtn).setOnClickListener {
//            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

        initOpenCV()

    }


    private fun getResizedBitmap(bitmap: Bitmap, maxHeight: Int): Bitmap? {
        val ratio = bitmap.height / maxHeight.toDouble()
        val width = (bitmap.width / ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, maxHeight, false)
    }

    private fun findPoints(): List<PointF?>? {
        var result: MutableList<PointF?>? = null
        val image = Mat()
        val orig = Mat()
        Utils.bitmapToMat(getResizedBitmap(mBitmap!!, MAX_HEIGHT), image)
        Utils.bitmapToMat(mBitmap, orig)
        val edges = edgeDetection(image)
        val largest = findLargestContour(edges!!)
        if (largest != null) {
            val points: Array<Point?> = sortPoints(largest.toArray())
            result = ArrayList()
            result.add(
                PointF(
                    java.lang.Double.valueOf(points[0]!!.x).toFloat(), java.lang.Double.valueOf(
                        points[0]!!.y
                    ).toFloat()
                )
            )
            result.add(
                PointF(
                    java.lang.Double.valueOf(points[1]!!.x).toFloat(), java.lang.Double.valueOf(
                        points[1]!!.y
                    ).toFloat()
                )
            )
            result.add(
                PointF(
                    java.lang.Double.valueOf(points[2]!!.x).toFloat(), java.lang.Double.valueOf(
                        points[2]!!.y
                    ).toFloat()
                )
            )
            result.add(
                PointF(
                    java.lang.Double.valueOf(points[3]!!.x).toFloat(), java.lang.Double.valueOf(
                        points[3]!!.y
                    ).toFloat()
                )
            )
            largest.release()
        } else {
            Timber.d("Can't find rectangle!")
        }
        edges.release()
        image.release()
        orig.release()
        return result
    }

    private fun findLargestContour(src: Mat): MatOfPoint2f? {
        val contours: MutableList<MatOfPoint> = mutableListOf()
        Imgproc.findContours(src, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Get the 5 largest contours
        contours.sortWith({ o1, o2 ->
            val area1 = Imgproc.contourArea(o1)
            val area2 = Imgproc.contourArea(o2)
            (area2 - area1).toInt()
        })
        if (contours.size > 5) {

            contours.subList(0, contours.size - 1).clear()
        }
        var largest: MatOfPoint2f? = null
        for (contour in contours) {
            val approx = MatOfPoint2f()
            val c = MatOfPoint2f()
            contour.convertTo(c, CvType.CV_32FC2)
            Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true)
            if (approx.total() == 4L && Imgproc.contourArea(contour) > 150) {
                // the contour has 4 points, it's valid
                largest = approx
                break
            }
        }
        return largest
    }

    private fun perspectiveTransform(src: Mat, points: List<PointF>): Mat? {
        val point1 = Point(
            points[0].x.toDouble(),
            points[0].y.toDouble()
        )
        val point2 = Point(
            points[1].x.toDouble(),
            points[1].y.toDouble()
        )
        val point3 = Point(
            points[2].x.toDouble(),
            points[2].y.toDouble()
        )
        val point4 = Point(
            points[3].x.toDouble(),
            points[3].y.toDouble()
        )
        val pts: Array<Point?> = arrayOf(point1, point2, point3, point4)
        return fourPointTransform(src, sortPoints(pts))
    }

    private fun applyThreshold(src: Mat): Bitmap? {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY)

        // Some other approaches
//        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
//        Imgproc.threshold(src, src, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        Imgproc.GaussianBlur(src, src, Size(5.0, 5.0), 0.0)
        Imgproc.adaptiveThreshold(
            src,
            src,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11,
            2.0
        )
        val bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, bm)
        return bm
    }

    private fun edgeDetection(src: Mat): Mat? {
        val edges = Mat()
        Imgproc.cvtColor(src, edges, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(edges, edges, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(edges, edges, 75.0, 200.0)
        return edges
    }

    private fun fourPointTransform(src: Mat, pts: Array<Point?>): Mat? {
        val ratio = src.size().height / MAX_HEIGHT.toDouble()
        val ul: Point = pts[0]!!
        val ur: Point = pts[1]!!
        val lr: Point = pts[2]!!
        val ll: Point = pts[3]!!
        val widthA = Math.sqrt(Math.pow(lr.x - ll.x, 2.0) + Math.pow(lr.y - ll.y, 2.0))
        val widthB = Math.sqrt(Math.pow(ur.x - ul.x, 2.0) + Math.pow(ur.y - ul.y, 2.0))
        val maxWidth = Math.max(widthA, widthB) * ratio
        val heightA = Math.sqrt(Math.pow(ur.x - lr.x, 2.0) + Math.pow(ur.y - lr.y, 2.0))
        val heightB = Math.sqrt(Math.pow(ul.x - ll.x, 2.0) + Math.pow(ul.y - ll.y, 2.0))
        val maxHeight = Math.max(heightA, heightB) * ratio
        val resultMat = Mat(
            java.lang.Double.valueOf(maxHeight).toInt(),
            java.lang.Double.valueOf(maxWidth).toInt(),
            CvType.CV_8UC4
        )
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        val dstMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(
            0,
            0,
            ul.x * ratio,
            ul.y * ratio,
            ur.x * ratio,
            ur.y * ratio,
            lr.x * ratio,
            lr.y * ratio,
            ll.x * ratio,
            ll.y * ratio
        )
        dstMat.put(0, 0, 0.0, 0.0, maxWidth, 0.0, maxWidth, maxHeight, 0.0, maxHeight)
        val M = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        Imgproc.warpPerspective(src, resultMat, M, resultMat.size())
        srcMat.release()
        dstMat.release()
        M.release()
        return resultMat
    }

    private fun sortPoints(src: Array<Point?>): Array<Point?> {
        val srcPoints: List<Point> = (src?.toList() ?: arrayListOf()) as List<Point>
        val result = arrayOf<Point?>(null, null, null, null)
        val sumComparator: Comparator<Point> = Comparator<Point> { o1, o2 ->
            java.lang.Double.valueOf(o1?.y!!.plus(o1?.x!!)).compareTo(o2?.y!! + o2?.x!!)
        }
        val differenceComparator: Comparator<Point> =
            Comparator<Point> { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
            }
        result[0] = Collections.min(srcPoints, sumComparator) // Upper left has the minimal sum
        result[2] = Collections.max(srcPoints, sumComparator) // Lower right has the maximal sum
        result[1] = Collections.min(
            srcPoints,
            differenceComparator
        ) // Upper right has the minimal difference
        result[3] = Collections.max(
            srcPoints,
            differenceComparator
        ) // Lower left has the maximal difference
        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data!!
            Toast.makeText(this, "uri $uri ", Toast.LENGTH_SHORT).show()
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                selectedView.setImageBitmap(getResizedBitmap(mBitmap!!, MAX_HEIGHT))
                val points = findPoints()
                selectedView.points = points
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "error!!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "$requestCode $requestCode $data ", Toast.LENGTH_SHORT).show()
        }
    }


    private fun initOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Timber.d("Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVLoaderCallback)
        } else {
            Timber.d("OpenCV library found inside package. Using it!")
            mOpenCVLoaderCallback!!.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onResume() {
        super.onResume()
        initOpenCV()
    }

}