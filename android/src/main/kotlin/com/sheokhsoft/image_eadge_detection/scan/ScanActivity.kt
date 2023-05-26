package com.sheokhsoft.image_eadge_detection.scan

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.exifinterface.media.ExifInterface
import com.sheokhsoft.image_eadge_detection.EdgeDetectionHandler
import com.sheokhsoft.image_eadge_detection.R
import com.sheokhsoft.image_eadge_detection.REQUEST_CODE
import com.sheokhsoft.image_eadge_detection.base.BaseActivity
import com.sheokhsoft.image_eadge_detection.view.PaperRectangle
//import kotlinx.android.synthetic.main.activity_scan.*
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import java.io.*

class ScanActivity : BaseActivity(), IScanView.Proxy {

    private lateinit var mPresenter: ScanPresenter;

    override fun provideContentViewId(): Int = R.layout.activity_scan

    override fun initPresenter() {
        val initialBundle = intent.getBundleExtra(EdgeDetectionHandler.INITIAL_BUNDLE) as Bundle;
        mPresenter = ScanPresenter(this, this, initialBundle)
    }

    override fun prepare() {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "loading opencv error, exit")
            finish()
        }

        findViewById<View>(R.id.shut).setOnClickListener {
            if (mPresenter.canShut) {
                mPresenter.shut()
            }
        }

        findViewById<View>(R.id.flash).visibility =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        // to hidde the flashLight button from  SDK versions which we do not handle the permission for!
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
                        //
                        baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
                ) View.VISIBLE else View.GONE;
        findViewById<View>(R.id.flash).setOnClickListener {
            mPresenter.toggleFlash();
        }

        val initialBundle = intent.getBundleExtra(EdgeDetectionHandler.INITIAL_BUNDLE) as Bundle;
        // NullPointerException here in case directly FROM_GALLERY
        if(! initialBundle.containsKey(EdgeDetectionHandler.FROM_GALLERY)){
            this.title = initialBundle.getString(EdgeDetectionHandler.SCAN_TITLE) as String
        }
        //
        findViewById<View>(R.id.gallery).visibility =
                if (initialBundle.getBoolean(EdgeDetectionHandler.CAN_USE_GALLERY, true))
                    View.VISIBLE
                else View.GONE;

        findViewById<View>(R.id.gallery).setOnClickListener {
            pickupFromGallery()
        };

        if (initialBundle.containsKey(EdgeDetectionHandler.FROM_GALLERY) && initialBundle.getBoolean(
                        EdgeDetectionHandler.FROM_GALLERY,
                        false
                )
        ) {
            pickupFromGallery()
        }
    }

    fun pickupFromGallery() {
        mPresenter.stop()
        val gallery = Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply{
            type="image/*"
        }
        ActivityCompat.startActivityForResult(this, gallery, 1, null);
    }


    override fun onStart() {
        super.onStart()
        mPresenter.start()
    }

    override fun onStop() {
        super.onStop()
        mPresenter.stop()
    }

    override fun exit() {
        finish()
    }

    override fun getCurrentDisplay(): Display? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            this.display
        } else {
            this.windowManager.defaultDisplay
        }
    }

    override fun getSurfaceView(): SurfaceView = findViewById<SurfaceView>(R.id.surface)

    override fun getPaperRect(): PaperRectangle = findViewById<PaperRectangle>(R.id.paper_rect)

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                if (intent.hasExtra(EdgeDetectionHandler.FROM_GALLERY) && intent.getBooleanExtra(
                                EdgeDetectionHandler.FROM_GALLERY, false
                        )
                )
                    finish()
            }
        }

        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                val uri: Uri = data!!.data!!
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    onImageSelected(uri)
                }
            } else {
                if (intent.hasExtra(EdgeDetectionHandler.FROM_GALLERY) && intent.getBooleanExtra(
                                EdgeDetectionHandler.FROM_GALLERY,
                                false
                        )
                )
                    finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun onImageSelected(imageUri: Uri) {
        try {
            val iStream: InputStream = contentResolver.openInputStream(imageUri)!!

            val exif = ExifInterface(iStream);
            var rotation = -1
            val orientation: Int = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = Core.ROTATE_90_CLOCKWISE
                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = Core.ROTATE_180
                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = Core.ROTATE_90_COUNTERCLOCKWISE
            }
            val mimeType = contentResolver.getType(imageUri)
            var imageWidth = 0.0
            var imageHeight = 0.0

            if (mimeType?.startsWith("image/png") == true) {
                val source = ImageDecoder.createSource(contentResolver, imageUri)
                val drawable = ImageDecoder.decodeDrawable(source)

                imageWidth = drawable.intrinsicWidth.toDouble()
                imageHeight = drawable.intrinsicHeight.toDouble()

                if (rotation == Core.ROTATE_90_CLOCKWISE || rotation == Core.ROTATE_90_COUNTERCLOCKWISE) {
                    imageWidth = drawable.intrinsicHeight.toDouble()
                    imageHeight = drawable.intrinsicWidth.toDouble()
                }
            } else {
                imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).toDouble()
                imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).toDouble()
                if (rotation == Core.ROTATE_90_CLOCKWISE || rotation == Core.ROTATE_90_COUNTERCLOCKWISE) {
                    imageWidth = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0).toDouble()
                    imageHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0).toDouble()
                }
            }

            val inputData: ByteArray? = getBytes(contentResolver.openInputStream(imageUri)!!)
            val mat = Mat(Size(imageWidth, imageHeight), CvType.CV_8U)
            mat.put(0, 0, inputData)
            val pic = Imgcodecs.imdecode(mat, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
            if (rotation > -1) Core.rotate(pic, pic, rotation)
            mat.release()

            mPresenter.detectEdge(pic);
        } catch (error: Exception) {
            val intent = Intent()
            intent.putExtra("RESULT", error.toString())
            setResult(400, intent)
            finish()
        }

    }

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray? {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }
}
