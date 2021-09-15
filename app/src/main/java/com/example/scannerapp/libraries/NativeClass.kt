package com.example.scannerapp.libraries
import android.graphics.Bitmap;
import com.example.scannerapp.helpers.MathUtils

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import com.labters.documentscanner.helpers.ImageUtils
import org.opencv.core.MatOfPoint2f
import com.labters.documentscanner.libraries.PerspectiveTransformation
import org.opencv.core.*
import java.util.*


class NativeClass {
    companion object {
        private const val THRESHOLD_LEVEL = 2
        private const val AREA_LOWER_THRESHOLD = 0.2
        private const val AREA_UPPER_THRESHOLD = 0.98
        private const val DOWNSCALE_IMAGE_SIZE = 600.0
        private val AreaDescendingComparator: Comparator<MatOfPoint2f?> =
            Comparator<MatOfPoint2f?> { o1, o2 ->
                val area1 = Imgproc.contourArea(o1)
                val area2 = Imgproc.contourArea(o2)
                Math.ceil(area2 - area1).toInt()
            }

        init {
            System.loadLibrary("opencv_java3")
        }
    }

    fun getScannedBitmap(
        bitmap: Bitmap?,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float,
        x4: Float,
        y4: Float
    ): Bitmap {
        val perspective = PerspectiveTransformation()
        val rectangle = MatOfPoint2f()
        rectangle.fromArray(
            Point(x1.toDouble(), y1.toDouble()), Point(
                x2.toDouble(), y2.toDouble()
            ), Point(x3.toDouble(), y3.toDouble()), Point(
                x4.toDouble(),
                y4.toDouble()
            )
        )
        val dstMat = perspective.transform(ImageUtils.bitmapToMat(bitmap), rectangle)
        return ImageUtils.matToBitmap(dstMat)
    }

    fun getPoint(bitmap: Bitmap?): MatOfPoint2f? {
        val src = ImageUtils.bitmapToMat(bitmap)

        // Downscale image for better performance.
        val ratio =
            DOWNSCALE_IMAGE_SIZE / Math.max(
                src.width(),
                src.height()
            )
        val downscaledSize =
            Size(src.width() * ratio, src.height() * ratio)
        val downscaled = Mat(downscaledSize, src.type())
        Imgproc.resize(src, downscaled, downscaledSize)
        val rectangles = getPoints(downscaled)
        if (rectangles.size == 0) {
            return null
        }
        Collections.sort(rectangles, AreaDescendingComparator)
        val largestRectangle = rectangles[0]
        return MathUtils.scaleRectangle(largestRectangle, 1f / ratio)
    }

    //public native float[] getPoints(Bitmap bitmap);
    fun getPoints(src: Mat): List<MatOfPoint2f> {

        // Blur the image to filter out the noise.
        val blurred = Mat()
        Imgproc.medianBlur(src, blurred, 9)

        // Set up images to use.
        val gray0 = Mat(blurred.size(), CvType.CV_8U)
        val gray = Mat()

        // For Core.mixChannels.
        val contours: List<MatOfPoint> = ArrayList()
        val rectangles: MutableList<MatOfPoint2f> = ArrayList()
        val sources: MutableList<Mat> = ArrayList()
        sources.add(blurred)
        val destinations: MutableList<Mat> = ArrayList()
        destinations.add(gray0)

        // To filter rectangles by their areas.
        val srcArea = src.rows() * src.cols()

        // Find squares in every color plane of the image.
        for (c in 0..2) {
            val ch = intArrayOf(c, 0)
            val fromTo = MatOfInt(*ch)
            Core.mixChannels(sources, destinations, fromTo)

            // Try several threshold levels.
            for (l in 0 until THRESHOLD_LEVEL) {
                if (l == 0) {
                    // HACK: Use Canny instead of zero threshold level.
                    // Canny helps to catch squares with gradient shading.
                    // NOTE: No kernel size parameters on Java API.
                    Imgproc.Canny(gray0, gray, 10.0, 20.0)

                    // Dilate Canny output to remove potential holes between edge segments.
                    Imgproc.dilate(gray, gray, Mat.ones(Size(3.0, 3.0), 0))
                } else {
                    val threshold = (l + 1) * 255 / THRESHOLD_LEVEL
                    Imgproc.threshold(
                        gray0,
                        gray,
                        threshold.toDouble(),
                        255.0,
                        Imgproc.THRESH_BINARY
                    )
                }

                // Find contours and store them all as a list.
                Imgproc.findContours(
                    gray,
                    contours,
                    Mat(),
                    Imgproc.RETR_LIST,
                    Imgproc.CHAIN_APPROX_SIMPLE
                )
                for (contour in contours) {
                    val contourFloat: MatOfPoint2f = MathUtils.toMatOfPointFloat(contour)
                    val arcLen = Imgproc.arcLength(contourFloat, true) * 0.02

                    // Approximate polygonal curves.
                    val approx = MatOfPoint2f()
                    Imgproc.approxPolyDP(contourFloat, approx, arcLen, true)
                    if (isRectangle(approx, srcArea)) {
                        rectangles.add(approx)
                    }
                }
            }
        }
        return rectangles
    }

    private fun isRectangle(polygon: MatOfPoint2f, srcArea: Int): Boolean {
        val polygonInt: MatOfPoint = MathUtils.toMatOfPointInt(polygon)
        if (polygon.rows() != 4) {
            return false
        }
        val area = Math.abs(Imgproc.contourArea(polygon))
        if (area < srcArea * AREA_LOWER_THRESHOLD || area > srcArea * AREA_UPPER_THRESHOLD) {
            return false
        }
        if (!Imgproc.isContourConvex(polygonInt)) {
            return false
        }

        // Check if the all angles are more than 72.54 degrees (cos 0.3).
        var maxCosine = 0.0
        val approxPoints = polygon.toArray()
        for (i in 2..4) {
            val cosine: Double = Math.abs(
                MathUtils.angle(
                    approxPoints[i % 4],
                    approxPoints[i - 2], approxPoints[i - 1]
                )
            )
            maxCosine = Math.max(cosine, maxCosine)
        }
        return if (maxCosine >= 0.3) {
            false
        } else true
    }
}