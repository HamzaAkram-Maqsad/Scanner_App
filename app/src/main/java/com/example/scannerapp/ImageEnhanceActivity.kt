package com.example.scannerapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Bitmap
import android.widget.Button
import android.widget.ImageView
import com.example.scannerapp.libraries.NativeClass
import com.example.scannerapp.helpers.MyConstants


class ImageEnhanceActivity : AppCompatActivity() {

    var imageView: ImageView? = null
    var selectedImageBitmap: Bitmap? = null

    var btnImageToBW: Button? = null
    var btnImageToMagicColor: Button? = null
    var btnImageToGray: Button? = null

    var nativeClass: NativeClass? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_enhance)
        initializeElement()
        initializeImage()
    }

    private fun initializeElement() {
        nativeClass = NativeClass()
        imageView = findViewById(R.id.imageView)
        btnImageToGray = findViewById(R.id.btnImageToGray)
        btnImageToBW = findViewById(R.id.btnImageToBW)
        btnImageToMagicColor = findViewById(R.id.btnImageToMagicColor)

    }

    private fun initializeImage() {
        selectedImageBitmap = MyConstants.selectedImageBitmap
        MyConstants.selectedImageBitmap = null
        imageView!!.setImageBitmap(selectedImageBitmap)
    }
}