package com.example.scannerapp

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.scannerapp.helpers.MyConstants

import androidx.core.app.ActivityCompat.startActivityForResult

import android.content.Intent

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.provider.MediaStore
import java.io.FileNotFoundException
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    var imageView: ImageView? = null
    val REQUEST_IMAGE_CAPTURE = 1
    var selectedImage: Uri? = null
    var selectedBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        findViewById<Button>(R.id.gallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, MyConstants.GALLERY_IMAGE_LOADED)
        }
        imageView = findViewById(R.id.imageView)

        findViewById<Button>(R.id.scan).setOnClickListener {
            MyConstants.selectedImageBitmap = selectedBitmap
            val intent = Intent(applicationContext, ImageCropActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.camera).setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: ActivityNotFoundException) {
                // display error state to the user
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MyConstants.GALLERY_IMAGE_LOADED && resultCode == RESULT_OK && data != null) {
            selectedImage = data.data!!
            //selectedBitmap = data.extras?.get("data") as Bitmap


        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            selectedImage = data.data!!
        }
        loadImage()
    }

    private fun loadImage() {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(selectedImage!!)
            selectedBitmap = BitmapFactory.decodeStream(inputStream)
            imageView!!.setImageBitmap(selectedBitmap)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }
}
