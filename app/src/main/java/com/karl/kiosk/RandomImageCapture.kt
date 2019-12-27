package com.karl.kiosk

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.karl.kiosk.Camera.preview.CameraPreview
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import android.os.CountDownTimer
import android.view.Surface
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.shared.preferences.session
import com.valdesekamdem.library.mdtoast.MDToast
import kotlinx.android.synthetic.main.activity_random_image_capture.*
import java.nio.file.Files


class RandomImageCapture : AppCompatActivity() {

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null
    private var g_name = ""
    private var g_date = ""
    private var g_time = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_image_capture)

//        val helper = helper(this)
//
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//
//        if (helper.isTablet()){
//            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        }


        g_name = intent.getSerializableExtra("Name") as String
        g_date = intent.getSerializableExtra("Date") as String
        g_time = intent.getSerializableExtra("Time") as String

        //val blink = AnimationUtils.loadAnimation(this,R.anim.blink)

        if (checkCameraHardware(this)) {
            mCamera = getCameraInstance()

            val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val rotation = display.getRotation()

            when(rotation) {
                Surface.ROTATION_0->mCamera?.setDisplayOrientation(90)
                Surface.ROTATION_90->mCamera?.setDisplayOrientation(0)
                Surface.ROTATION_180->mCamera?.setDisplayOrientation(270)
                Surface.ROTATION_270->mCamera?.setDisplayOrientation(180)
            }

            mPreview = mCamera?.let {
                // Create our Preview view
                CameraPreview(this, it)
            }

            // Set the Preview view as the content of our activity.
            mPreview?.also {
                val preview: FrameLayout = findViewById(R.id.preview)
                preview.addView(it)
            }

            object : CountDownTimer(1000 * 5 , 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds_left = millisUntilFinished / 1000
                    timer.setText(seconds_left.toString())
                }

                override fun onFinish() {
                    //timer.setText("Smile! :)")
                    //mPreview?.startAnimation(blink)
                    mCamera?.takePicture(null, null, mPicture)
                }
            }.start()
        }
    }

    private val mPicture = Camera.PictureCallback { data, _ ->
        val pictureFile: File = getOutputMediaFile(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) ?: run {
            Log.d("ENS", ("Error creating media file, check storage permissions"))
            return@PictureCallback
        }

        try {

            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()

            val i = Intent()
            i.putExtra("URI",pictureFile.absolutePath)
            setResult(RESULT_OK, i)

            finish()
        } catch (e: FileNotFoundException) {
            Log.d("FNF", "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.d("NACCSS", "Error accessing file: ${e.message}")
        }
    }

    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open(1) // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val session = session(this)
        val company = session.getCompanyName()

        val e_dir = Environment.getExternalStorageDirectory()
        val dir = "$e_dir/Caimito Apps/Kiosk/$company"
        val folder_name = "Captures"

        val mediaStorageDir = File(
            dir,
            folder_name
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs()
        }

        // Create a media file name

        return when (type) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {

                //MDToast.makeText(this,"Image saved!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()

                val timeStamp = "$g_time-$g_date"
                val filePath = "${mediaStorageDir.path}${File.separator}${g_name}_$timeStamp.jpg"
                return File(filePath)
            }
            else -> return null
        }
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        MDToast.makeText(this,"Please wait until picture is taken.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera?.release()
    }
}
