package com.karl.kiosk

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.*
import com.android.volley.*
import com.android.volley.toolbox.StringRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.location.*
import com.google.gson.JsonElement
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.Interface.ImageFilePath
import com.karl.kiosk.Interface.UploadAPIs
import com.karl.kiosk.shared.preferences.session
import com.karl.kiosk.volley.singleton.volley_singleton
import com.valdesekamdem.library.mdtoast.MDToast
import kotlinx.android.synthetic.main.activity_enter_pin.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE
import java.util.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.experimental.and

class EnterPIN : AppCompatActivity() {

    val IMAGE_CAPTURE_REQUEST_CODE = 1
    var PERMISSION_CODE = 0
    private var locationManager: LocationManager? = null

    private lateinit var r: Runnable
    private lateinit var session: session
    private lateinit var helper: helper
    private var mHandler = Handler()
    private var r_pin1: String = ""
    private var r_pin2: String = ""
    private var r_pin3: String = ""
    private var r_pin4: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest

    private var id = ""
    private var fname = ""
    private var lname = ""
    private var name = ""
    private var user_image_url = ""
    private var time_in = ""
    private var time_out = ""
    private var user_pin = ""

    private var g_date = ""
    private lateinit var avatar: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var change_pin_dialog: AlertDialog

    private lateinit var action_change_pin: MenuItem

    //FOR ACTIVITY RESULT
    private var g_entered_pin = ""
    private var g_time = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_pin)

        //Creates Session and Helper Object
        instantiateHelpersAndSessions()

        //Starts clock
        runClock(mHandler)

        g_date = session.getDate()
        today.text = session.getReadableDate()

        //Creates Location listener
        instantiateLocationVariables()

        //Retrieves Data passed from intent (From Users_Adapter class)
        getUserDataFromIntent()

        //Sets users name and picture
        setUserNameAndPicture()

        // Time Fields
        setUserTime()

        //Sets buttons and listeners
        setButtonAndPINSListener()
    }

    private fun instantiateHelpersAndSessions() {
        session = session(this)
        helper = helper(this)
    }

    // Runs Clock
    private fun runClock(mHandler: Handler) {

        session = session(applicationContext)
        val time12hf = session.getFormattedTime(12, "hh:mm:ss a")

        clockView.text = time12hf.replace("p.m.", "PM").replace("a.m.", "AM")

        r = object : Runnable {

            override fun run() {

                val time12hf = session.getFormattedTime(12, "hh:mm:ss a")

                clockView.text = time12hf.replace("p.m.", "PM").replace("a.m.", "AM")

                mHandler.postDelayed(r, 1000)
            }
        }
        mHandler.postDelayed(r, 1000)

    }

    // For retrieving locations
    private fun instantiateLocationVariables() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 5000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.maxWaitTime = 5000
    }

    // User InfoField
    private fun setUserNameAndPicture() {
        val nameField = findViewById<TextView>(R.id.userIDTF)
        nameField.text = name
        avatar = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.userAvatar)

        Glide.with(this)
            .load(user_image_url)
            //For placeholder
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.blank_image)
                    .centerCrop()
                    .fitCenter()
            )
            .into(avatar)
        avatar.setOnClickListener {
            finish()
        }
    }

    // Sets user time in and time out for the day
    private fun setUserTime() {
        val time_in_field = findViewById<TextView>(R.id.time_in_field)
        val time_out_field = findViewById<TextView>(R.id.time_out_field)

        //User has time in
        if (time_in != "null" && time_in != "") {
            //User has time out
            if (time_out != "null" && time_out != "") {

                //If time was adjusted User time will be not have seconds
                val ti_sdf = if (time_in.length == 5)
                //Parse time with this pattern
                    SimpleDateFormat("HH:mm")
                else
                //Parse time with this pattern
                    SimpleDateFormat("HH:mm:ss")

                val to_sdf = if (time_out.length == 5)
                //Parse time with this pattern
                    SimpleDateFormat("HH:mm")
                else
                //Parse time with this pattern
                    SimpleDateFormat("HH:mm:ss")

                val time_in12hf = ti_sdf.parse(time_in)
                val time_out12hf = to_sdf.parse(time_out)

                avatar.borderColor = ContextCompat.getColor(
                    this,
                    R.color.colorWasActive
                )

                time_in_field.text = "Time in:  " + SimpleDateFormat("KK:mm:ss a").format(time_in12hf)
                time_out_field.text = "Time out: " + SimpleDateFormat("KK:mm:ss a").format(time_out12hf)
                buttonCheck.setImageResource(R.drawable.ic_check_circle_dark_gray_24dp)
                buttonCheck.isEnabled = FALSE
            }
            //User has no time out
            else {
                val ti_sdf = if (time_in.length == 5)
                //Parse time with this pattern
                    SimpleDateFormat("HH:mm")
                else
                //Parse time with this pattern
                    SimpleDateFormat("HH:mm:ss")

                val time_in12hf = ti_sdf.parse(time_in)

                time_in_field.text = "Time in:  " + SimpleDateFormat("KK:mm:ss a").format(time_in12hf)
                time_out_field.text = "Time out: --:--:-- --"
                buttonCheck.setImageResource(R.drawable.ic_check_circle_orange_24dp)

                avatar.borderColor = ContextCompat.getColor(
                    this,
                    R.color.colorLimeGreen
                )
            }
        }

        //Blank time in and time out
        if (time_in == "null" || time_in == "") {
            time_in_field.text = "Time in:  --:--:-- --"
        }
        if (time_out == "null" || time_out == "") {
            time_out_field.text = "Time out: --:--:-- --"
        }
    }

    // When Employee image is clicked in the MainActivity
    // It passes a String key an value of the selected user
    private fun getUserDataFromIntent() {
        id = intent.getSerializableExtra("userId") as String
        fname = intent.getSerializableExtra("firstName") as String
        lname = intent.getSerializableExtra("lastName") as String
        name = "$fname $lname"
        user_image_url = intent.getSerializableExtra("userImagePath") as String
        time_in = intent.getSerializableExtra("user_time_in") as String
        time_out = intent.getSerializableExtra("user_time_out") as String
        user_pin = intent.getSerializableExtra("user_pin") as String
    }

    // When check button is pressed,
    // Check if all fields are filled
    private fun checkPINFields(PIN1: String, PIN2: String, PIN3: String, PIN4: String): String {

        val pin1HasValue = PIN1 != ""
        val pin2HasValue = PIN2 != ""
        val pin3HasValue = PIN3 != ""
        val pin4HasValue = PIN4 != ""

        if (pin1HasValue && pin2HasValue && pin3HasValue && pin4HasValue) {

            return PIN1 + PIN2 + PIN3 + PIN4

        } else {
            MDToast.makeText(this, "Please fill all fields.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            return ""
        }
    }

    // When Pin button is pressed
    // Sets pressed value to first empty pin
    private fun setTo(PIN1: TextView, PIN2: TextView, PIN3: TextView, PIN4: TextView, value: String) {
        if (value == "clear") {
            if (PIN4.text.toString().trim().length > 0) {

                PIN4.text = ""
                r_pin4 = ""

            } else {
                if (PIN3.text.toString().trim().length > 0) {

                    PIN3.text = ""
                    r_pin3 = ""

                } else {
                    if (PIN2.text.toString().trim().length > 0) {
                        PIN2.text = ""
                        r_pin2 = ""
                    } else {

                        PIN1.text = ""
                        r_pin1 = ""
                    }
                }
            }

        } else {
            if (PIN1.text.toString() == "") {

                PIN1.text = "*"
                r_pin1 = value
            } else {
                if (PIN2.text.toString().trim() == "") {

                    PIN2.text = "*"
                    r_pin2 = value

                } else {

                    if (PIN3.text.toString().trim() == "") {

                        PIN3.text = "*"
                        r_pin3 = value

                    } else {

                        if (PIN4.text.toString().trim() == "") {

                            PIN4.text = "*"
                            r_pin4 = value
                        }

                    }

                }
            }
        }
    }

    // MD5 hash checker
    // Compares hashed pin to hashed entered pin
    // Returns 1 if correct
    private fun checkUserPIN(user_pin: String, entered_password: String): Int {

        val md = MessageDigest.getInstance("MD5")
        md.update(entered_password.trim().toByteArray(Charsets.UTF_8))
        val digest = md.digest()
        val hexString = StringBuffer(digest.size * 2)

        for (i in digest) {

            val b = i and 0xFF.toByte()

            if (b < 0x10) {
                hexString.append('0')
            }
            hexString.append(Integer.toHexString(b.toInt()))
        }

        val hs = hexString.toString().replace("0ffffff", "")

        if (hs == user_pin) {
            return 1
        } else {
            clearAll()
            return 0
        }
    }

    // Initializes buttons
    // Sets Listeners
    private fun setButtonAndPINSListener() {
        // PIN Fields
        val PIN1 = findViewById<TextView>(R.id.PINField1)
        val PIN2 = findViewById<TextView>(R.id.PINField2)
        val PIN3 = findViewById<TextView>(R.id.PINField3)
        val PIN4 = findViewById<TextView>(R.id.PINField4)

        //Buttons
        val button0 = findViewById<Button>(R.id.button0)
        val button1 = findViewById<Button>(R.id.button1)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)
        val button6 = findViewById<Button>(R.id.button6)
        val button7 = findViewById<Button>(R.id.button7)
        val button8 = findViewById<Button>(R.id.button8)
        val button9 = findViewById<Button>(R.id.button9)
        val buttonClear = findViewById<ImageView>(R.id.buttonClear)
        val buttonCheck = findViewById<ImageView>(R.id.buttonCheck)

        //Button pad Onclick
        button0.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "0")
        }
        button1.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "1")
        }
        button2.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "2")
        }
        button3.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "3")
        }
        button4.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "4")
        }
        button5.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "5")
        }
        button6.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "6")
        }
        button7.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "7")
        }
        button8.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "8")
        }
        button9.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "9")
        }
        buttonClear.setOnClickListener {
            setTo(PIN1, PIN2, PIN3, PIN4, "clear")
        }

        buttonCheck.setOnClickListener {

            buttonCheck.isEnabled = FALSE

            val time = session.getFormattedTime(24, "HH:mm:ss")
            val entered_pin = checkPINFields(r_pin1, r_pin2, r_pin3, r_pin4)

            if (entered_pin.isNotEmpty()) {

                val status = checkUserPIN(user_pin, entered_pin)

                //Correct Pin
                if (status == 1) {


                    //If user has time-in
                    if (time_in != "null") {

                        //If time-in and time-out difference >= 5 minutes
                        if (helper.checkTimeDifference(time_in, time, 500)) {

                            takePictureRandomly(entered_pin, time)
                        }
                        //If time-in and time-out difference < 5 minutes
                        else {

                            helper.log(
                                "Name: $name \n" +
                                        "Time: $time \n" +
                                        "Date: $g_date \n" +
                                        "Status: Failed \n" +
                                        "Desc: Logged-in in less than 5 minutes.\n" +
                                        "--------------------"
                            )

                            MDToast.makeText(
                                this,
                                "Please wait 5 minutes before you clock-out.",
                                MDToast.LENGTH_LONG,
                                MDToast.TYPE_ERROR
                            ).show()
                            buttonCheck.isEnabled = TRUE
                        }
                    }
                    //User has not time yet
                    else {
                        takePictureRandomly(entered_pin, time)
                    }
                }
                //Wrong Pin
                if (status == 0) {

                    helper.log("Name: $name \nTime: $time \nDate: $g_date \nStatus: Failed \nDesc: Wrong PIN.\n--------------------")

                    MDToast.makeText(this, "Try Again.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    buttonCheck.isEnabled = TRUE
                }
            } else {
                buttonCheck.isEnabled = TRUE
            }
        }
    }

    // For image capture
    private fun takePictureRandomly(entered_pin: String, time: String) {

        // Retrieves captured percentage
        val percentage = session.getCapturePercentage().toInt()

        // If random number is in range of capture percentage
        // Takes picture using RandomImageCapture class
        if (helper.randomChanceByPercentage(percentage)) {
            g_entered_pin = entered_pin
            g_time = time

            val camera_intent = Intent(this, RandomImageCapture::class.java)
            camera_intent.putExtra("Name", name)
            camera_intent.putExtra("Date", g_date)
            camera_intent.putExtra("Time", g_time)

            // Confirm modal
            val confirmTakePicture = Dialog(this)
            confirmTakePicture.setContentView(R.layout.dialog_confirm_take_picture)
            confirmTakePicture.show()

            val button_take_picture: CardView = confirmTakePicture.findViewById(R.id.cv_take_picture);

            button_take_picture.setOnClickListener {
                // Takes Image
                // After successful image capture
                // onActivityResult is triggered
                startActivityForResult(camera_intent, IMAGE_CAPTURE_REQUEST_CODE)
            }
        } else {

            getUserLocation(id, entered_pin, time)
        }
    }

    // User location
    private fun getUserLocation(id: String, entered_pin: String, time: String) {

        val last_location = session.getLastLocation()

        //Has last location
        if (last_location != "") {


            //Last location is 20 meters away
            val isNearCompany = helper.isLocation20metersAway(last_location)

            if (isNearCompany) {
                helper.log(
                    "Name: $name \n" +
                            "Time: $time \n" +
                            "Date: $g_date \n" +
                            "Status: Success \n" +
                            "Desc: Login successful and has location data.\n" +
                            "--------------------"
                )

                //sendUserTime(id, entered_pin, time, session.getLastLocation())
                if (fileType == null || originalFile == null) {
                    sendUserTime(id, entered_pin, time, session.getLastLocation())
                } else {
                    sendUserTimeWithImage(id, entered_pin, time, session.getLastLocation())
                }

            }
            //Last location further than 20 meters
            else {

                val jarray_last_location = JSONArray("$last_location")
                val jobject_last_location = jarray_last_location.get(0) as JSONObject
                val s_lat = jobject_last_location.get("latitude").toString()
                val s_long = jobject_last_location.get("longitude").toString()
                val lat = s_lat.toDouble()
                val long = s_long.toDouble()

                val readable_address = helper.toStringAddress(lat, long)

                helper.log(
                    "Name: $name \n" +
                            "Time: $time \n" +
                            "Date: $g_date \n" +
                            "Status: Failed \n" +
                            "Desc: Login successful but wrong location.\n" +
                            "Location: $readable_address \n" +
                            "--------------------"
                )

                MDToast.makeText(
                    this, "Invalid location!\n" +
                            "Please stay within 20 meters of your company before logging in.",
                    MDToast.LENGTH_LONG, MDToast.TYPE_ERROR
                ).show()

                buttonCheck.isEnabled = TRUE
            }
            //Last location is empty
        } else {
            helper.log("Name: $name \nTime: $time \nDate: $g_date \nStatus: Success \nDesc: Login successful but no location data.\n--------------------")
            //sendUserTime(id, entered_pin, time, "")
            if (fileType == null || originalFile == null) {
                sendUserTime(id, entered_pin, time, session.getLastLocation())
            } else {
                sendUserTimeWithImage(id, entered_pin, time, session.getLastLocation())
            }
        }
    }

    // Sends to backend with image
    private fun sendUserTimeWithImage(p_id: String, p_pin_input: String, p_time: String, p_location: String) {

        val intent_splash_screen = Intent(this, IdleScreen::class.java)
        startActivityForResult(intent_splash_screen, 69)


        val user_id = RequestBody.create(MultipartBody.FORM, p_id)
        val pin = RequestBody.create(MultipartBody.FORM, p_pin_input);
        val date = RequestBody.create(MultipartBody.FORM, session.getDate());
        val time = RequestBody.create(MultipartBody.FORM, p_time);
        val location = RequestBody.create(MultipartBody.FORM, p_location);
        val reference = RequestBody.create(MultipartBody.FORM, "web_kiosk");
        val api_token = RequestBody.create(MultipartBody.FORM, session.getAPIToken());
        val link = RequestBody.create(MultipartBody.FORM, session.getLink());


        val filePart = RequestBody.create(
            fileType, originalFile
        )


        val file: MultipartBody.Part = MultipartBody.Part.createFormData(
            "image",
            originalFile!!.name,
            filePart
        )


        val retrofit = Retrofit.Builder()
                .baseUrl("http://".plus(session.getIP()).plus("/"))
                .client(OkHttpClient.Builder()
                        .connectTimeout(1, TimeUnit.MINUTES)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        val client = retrofit.create(UploadAPIs::class.java)

        val headers = HashMap<String, String>();
        headers.put("d", session.getHeaders("d"))
        headers.put("t", session.getHeaders("t"))
        headers.put("token", session.getToken()!!)

        Log.d("params-headers","token:" + session.getAPIToken())
        Log.d("params-headers","user-id:" + p_id)
        Log.d("params-headers","date:" + session.getDate())
        Log.d("params-headers","location:" + p_location)
        Log.d("params-headers","time:" + p_time)

        val call = client.checKPinWithImage(
            //"http://".plus(session.getIP()).plus("/clock/api/check"),
            "http://".plus(session.getIP()).plus("/adminbackend/api/photo-approval-store"),
            headers, user_id, pin, date, time, location, reference, api_token, link, file, session.getLink()
        )


        val that = this

        call.enqueue(object : retrofit2.Callback<JsonElement> {
            override fun onResponse(
                call: retrofit2.Call<JsonElement>,
                response: retrofit2.Response<JsonElement>
            ) {
                if (response.body() != null) {
                    //if (response.body()!!.getString("status") == "success") {
                    val rJO = JSONObject(response.body().toString())

                    if (rJO.getString("status") == "success") {
                        clearAll()

                        addTimetoLocalUsers(p_id, p_time, session.getDate(), true)

                        MDToast.makeText(that, "Success.", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
                        buttonCheck.isEnabled = TRUE
                        finishActivity(69)
                        finish()
                    } else {
                        MDToast.makeText(that, rJO.getString("msg"), MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                        buttonCheck.isEnabled = TRUE
                        finishActivity(69)
                        //finish()
                    }
                } else {
                    //Saves to pending updates
                    onDataIsUnsendable(p_id, p_time, session.getDate(), p_pin_input, p_location)
                    //Closes loading screen
                    finishActivity(69)
                    //Reenables check button
                    buttonCheck.isEnabled = TRUE
                    //Closes Enter PIN activity
                    finish()
                }

            }

            override fun onFailure(call: retrofit2.Call<JsonElement>, t: Throwable) {
                //Saves to pending updates
                onDataIsUnsendable(p_id, p_time, session.getDate(), p_pin_input, p_location)
                //Closes loading screen
                finishActivity(69)
                //Reenables check button
                buttonCheck.isEnabled = TRUE
                //Closes Enter PIN activity
                finish()
            }
        })
    }

    // Sends to backend with no image
    private fun sendUserTime(id: String, pin_input: String, time: String, location: String) {

        val date: String = session.getDate()
        val internet: Boolean = helper.isNetworkConnected()

        if (internet) {

            val intent_splash_screen = Intent(this, IdleScreen::class.java)
            startActivityForResult(intent_splash_screen, 69)

            val IP: String = session.getIP()

            val url = "http://$IP/clock/api/check"

            val stringRequest = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->

                    var strResp = response.toString()
                    val jsonObj = JSONObject(strResp)
                    val status = jsonObj.get("status").toString()
                    val message = jsonObj.get("msg").toString()

                    if (status.trim() == "success") {
                        clearAll()

                        addTimetoLocalUsers(id, time, date, true)

                        MDToast.makeText(this, "Success.", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
                        buttonCheck.isEnabled = TRUE
                        finishActivity(69)
                        finish()
                    }
                    if (status.trim() != "success") {

                        if (message == "Access Denied: invalid token!") {
                            MDToast.makeText(
                                this,
                                "Invalid Token!\nSomeone else is using this account.Timekeeper will be logged-out",
                                MDToast.LENGTH_LONG,
                                MDToast.TYPE_ERROR
                            ).show()
                            session.setMustLogout(true)
                        }

                        MDToast.makeText(this, message, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                        buttonCheck.isEnabled = TRUE
                        finishActivity(69)
                        finish()
                    }

                }, Response.ErrorListener { error ->

                    //Will use pending updates instead
//                    session.setMustRefresh(true)
//
//                    var time_flag = 0
//
//                    if (time_out != "null") {
//                        time_flag = 1
//                    }
//
//                    val item_to_be_checked = JSONObject()
//                    item_to_be_checked.put("user_id", id)
//                    item_to_be_checked.put("date", date)
//                    item_to_be_checked.put("time", time)
//                    item_to_be_checked.put("time_flag", time_flag)
//
//                    helper.addToBeChecked(item_to_be_checked)
                    //Will use pending updates instead

                    //Saves to pending updates
                    onDataIsUnsendable(id, time, date, pin_input, location)
                    //Closes loading screen
                    finishActivity(69)
                    //Reenables check button
                    buttonCheck.isEnabled = TRUE
                    //Closes Enter PIN activity
                    finish()
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()

                    params["user_id"] = id
                    params["pin"] = pin_input
                    params["date"] = date
                    params["time"] = time
                    params["location"] = location
                    params["reference"] = "web_kiosk"
                    params["api_token"] = session.getAPIToken()

                    params["link"] = session.getLink()

                    return params
                }

                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()

                    params["d"] = session.getHeaders("d")
                    params["t"] = session.getHeaders("t")
                    params["token"] = session.getToken()!!

                    return params
                }
            }
            stringRequest.retryPolicy = DefaultRetryPolicy(
                30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
        }

        if (!internet) {
            if (onDataIsUnsendable(id, time, date, pin_input, location)) {
                buttonCheck.isEnabled = TRUE
                finish()
            } else {
                MDToast.makeText(this, "Error! Cannot save time locally.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR)
                    .show()
                buttonCheck.isEnabled = TRUE
            }

            //MDToast.makeText(this,"Check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
        }
    }

    // If cannot send
    // Returns boolean
    private fun onDataIsUnsendable(id: String, time: String, date: String, pin: String, location: String): Boolean {

        val has_clocked_out_today = (addTimetoLocalUsers(id, time, date, false))
        addToPendingUpdates(id, time, date, pin, has_clocked_out_today, location)

        return true
    }

    // Sets user time to user even if data was not sent
    private fun addTimetoLocalUsers(user_id: String, time: String, date: String, was_data_sent: Boolean): Boolean {

        val users = session.getUsers()
        var users_array = JSONArray(users)
        var has_clocked_out_today = false

        findLocalUser@ for (i in 0..(users_array.length() - 1)) {

            var user = users_array.get(i) as JSONObject

            if (user_id == user.get("user_id").toString()) {

                var edtr_array = user.get("edtr") as JSONArray

                if (edtr_array.length() > 0) {

                    var edtr_object = edtr_array.get(0) as JSONObject

                    //Changing IF condition order may result to executing both conditions
                    if (edtr_object.get("date_in") == date) {
                        if (edtr_object.get("time_out").toString() != "null") {
                            has_clocked_out_today = true

                            break@findLocalUser
                        }

                        if (edtr_object.get("time_out").toString() == "null") {
                            edtr_object.put("time_out", time)
                            if (!was_data_sent) {
                                break@findLocalUser
                            }
                            break@findLocalUser
                        }
                    }
                    //Changing IF condition order may result to executing both conditions
                }
                if (edtr_array.length() == 0) {

                    var user_edtr = JSONObject()

                    user_edtr.put("date_in", date)
                    user_edtr.put("time_in", time)
                    user_edtr.put("time_out", "null")

                    if (!was_data_sent) {
                        //MDToast.makeText(this,"Time in was saved.[Local]", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()
                    }
                    edtr_array.put(user_edtr)
                    break@findLocalUser
                }
            }
        }
        session.setUsers(users_array.toString())
        return has_clocked_out_today
    }

    // Creates a pending item
    private fun addToPendingUpdates(
        user_id: String,
        time: String,
        date: String,
        pin: String,
        has_clocked_out_today: Boolean,
        location: String
    ): Boolean {

        var edtr_array = JSONArray(session.getPendingUpdates())

        var iso_date = date
        val location_id = session.getLocationID()

        if (!has_clocked_out_today) {

            if (edtr_array.length() == 0) {

                var edtr_object = JSONObject()
                edtr_object.put("date", iso_date)
                edtr_object.put("pin", pin)
                edtr_object.put("reference", "web_kiosk")
                edtr_object.put("time", time)
                edtr_object.put("location", location)
                edtr_object.put("location_id", location_id)
                edtr_object.put("user_id", user_id)
                //edtr_object.put("name", g_name)

                edtr_array.put(edtr_object)

                session.setPendingUpdates(edtr_array.toString())
                MDToast.makeText(this, "Time in was saved.[Local]", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()

            } else {
                var time_on_day_counter = 0

                for (i in 0..(edtr_array.length() - 1)) {

                    var pending_update = edtr_array.get(i) as JSONObject

                    if (pending_update.get("user_id") == user_id && pending_update.get("date") == date) {
                        time_on_day_counter++
                    }
                }

                when (time_on_day_counter) {
                    0 -> {
                        var edtr_object = JSONObject()
                        edtr_object.put("date", iso_date)
                        edtr_object.put("pin", pin)
                        edtr_object.put("reference", "web_kiosk")
                        edtr_object.put("time", time)
                        edtr_object.put("user_id", user_id)
                        edtr_object.put("location", location)
                        edtr_object.put("location_id", location_id)

                        edtr_array.put(edtr_object)

                        session.setPendingUpdates(edtr_array.toString())

                        MDToast.makeText(this, "Time in was saved.[Local]", MDToast.LENGTH_LONG, MDToast.TYPE_INFO)
                            .show()
                    }
                    1 -> {
                        var edtr_object = JSONObject()
                        edtr_object.put("date", iso_date)
                        edtr_object.put("pin", pin)
                        edtr_object.put("reference", "web_kiosk")
                        edtr_object.put("time", time)
                        edtr_object.put("user_id", user_id)
                        edtr_object.put("location", location)
                        edtr_object.put("location_id", location_id)

                        edtr_array.put(edtr_object)

                        session.setPendingUpdates(edtr_array.toString())
                        MDToast.makeText(this, "Time in was saved.[Local]", MDToast.LENGTH_LONG, MDToast.TYPE_INFO)
                            .show()
                    }
                    2 -> {
                        MDToast.makeText(
                            this,
                            "You already clocked out today.[Local]",
                            MDToast.LENGTH_LONG,
                            MDToast.TYPE_ERROR
                        ).show()
                    }
                    else -> {
                        MDToast.makeText(this, "Error", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    }
                }

            }
        }

        return true
    }


    // Resets PINS
    private fun clearAll() {

        val PIN1 = findViewById<TextView>(R.id.PINField1)
        val PIN2 = findViewById<TextView>(R.id.PINField2)
        val PIN3 = findViewById<TextView>(R.id.PINField3)
        val PIN4 = findViewById<TextView>(R.id.PINField4)

        PIN1.text = ""
        PIN2.text = ""
        PIN3.text = ""
        PIN4.text = ""
        r_pin1 = ""
        r_pin2 = ""
        r_pin3 = ""
        r_pin4 = ""

        PIN1.requestFocus()
    }

    // If any permission is not granted
    // Blocks usage if denied
    private fun checkAllPermissions() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            requestAllPermission()
        }
    }

    // Requests user to provide app permissions
    private fun requestAllPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                ),
                PERMISSION_CODE
            )
        }
    }

    // Checks Date, Timezone and GPS is enabled
    private fun checkAndAskSettingsRequirements() {

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
        val location_not_enabled = !locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val dateTime_not_automatic = !helper.isDateTimeAutomatic()
        val timezone_not_Automatic = !helper.isTimeZoneAutomatic()

        if (location_not_enabled) {
            showGPSDisabledAlertToUser()
        } else {
            if (dateTime_not_automatic || timezone_not_Automatic) {
                showDateTimeDisabledAlertToUser()
            } else {
                checkAllPermissions()
            }
        }
    }

    // Request disabled settings modal
    private fun showDateTimeDisabledAlertToUser() {
        val alertDialogBuilder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
        alertDialogBuilder.setMessage("Automatic time is disabled in your device.\nPlease enable it to use the app.")
            .setCancelable(false)
            .setPositiveButton(
                "Go to Settings Page"
            ) { dialog, id ->
                val callGPSSettingIntent = Intent(
                    android.provider.Settings.ACTION_DATE_SETTINGS
                )
                startActivity(callGPSSettingIntent)
            }
        //alertDialogBuilder.setNegativeButton("Cancel",
        //{ dialog, id -> dialog.cancel() })
        val alert = alertDialogBuilder.create()
        alert.show()
    }

    // Shows if gps is disabled
    private fun showGPSDisabledAlertToUser() {
        val alertDialogBuilder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
        alertDialogBuilder.setMessage("GPS is disabled in your device.\nPlease enable it and select High accuracy.")
            .setCancelable(false)
            .setPositiveButton(
                "Go to Settings Page"
            ) { dialog, id ->
                val callGPSSettingIntent = Intent(
                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
                startActivity(callGPSSettingIntent)
            }
        //alertDialogBuilder.setNegativeButton("Cancel",
        //{ dialog, id -> dialog.cancel() })
        val alert = alertDialogBuilder.create()
        alert.show()
    }

    override fun onResume() {
        super.onResume()

        checkAndAskSettingsRequirements()
    }

    // Triggered After Requesting Persmission to App
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CODE) {

            val granted = PackageManager.PERMISSION_GRANTED

            if (grantResults.isNotEmpty() &&
                grantResults[0] == granted &&
                grantResults[1] == granted &&
                grantResults[2] == granted &&
                grantResults[3] == granted &&
                grantResults[4] == granted
            ) {
                MDToast.makeText(this, "Permissions granted!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.enter_pin_appbar, menu)
        action_change_pin = menu.findItem(R.id.action_change_pin)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_change_pin -> {

            changePinDialog()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private var originalFile: File? = null
    private var fileType: MediaType? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {


            //Image uri (incomplete path)
            //val targetUri = Uri.parse(data?.getSerializableExtra("URI") as String)

            val stringPath = data?.getSerializableExtra("URI").toString()
            val targetUri = Uri.parse(stringPath)
            //Image uri (complete path)

            //val realPath: String = ImageFilePath.getPath(this, targetUri)
            //Get file type
            originalFile = File(stringPath)

            val te = getMimeType(stringPath)
            fileType = MediaType.parse(te)

            getUserLocation(id, g_entered_pin, g_time)
        }

//        if (requestCode == IMAGE_CAPTURE_REQUEST_CODE  && resultCode  == RESULT_OK) {
//            getUserLocation(id,g_entered_pin,g_time)
//        }
    }

    // Cannot retrieve file type using file
    // Returns Static
    // Always image file type
    fun getMimeType(path: String): String {
        var type = "image/jpeg" // Default Value
        val extension = MimeTypeMap.getFileExtensionFromUrl(path);

        // Not Working
        // if (extension != null) {
        //    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        // }
        return type
    }

    // DIALOGS
    private fun changePinDialog() {
        var change_pin_dialog_builder = AlertDialog.Builder(this, R.style.ChangePinAlertDialogCustom)
        change_pin_dialog_builder.setTitle("Change PIN:")

        var layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        var l_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        l_lp.setMargins(0, 0, 0, 10)

        var input = EditText(this)
        var input2 = EditText(this)
        var input3 = EditText(this)

        var i_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        i_lp.setMargins(10, 0, 10, 0)
        i_lp.gravity = Gravity.CENTER

        input.hint = "Old pin"
        input2.hint = "New pin"
        input3.hint = "Confirm pin"

        input.maxLines = 1
        input2.maxLines = 1
        input3.maxLines = 1

        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input2.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input3.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        input.setHintTextColor(getResources().getColor(R.color.colorSilver))
        input2.setHintTextColor(getResources().getColor(R.color.colorSilver))
        input3.setHintTextColor(getResources().getColor(R.color.colorSilver))

        input.setTextColor(getResources().getColor(R.color.colorSilver))
        input2.setTextColor(getResources().getColor(R.color.colorSilver))
        input3.setTextColor(getResources().getColor(R.color.colorSilver))

        var save = Button(this)
        var s_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        save.text = "Save"
        save.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorDarkGray
            )
        )
        s_lp.setMargins(0, 0, 10, 10)
        s_lp.gravity = Gravity.END

        layout.layoutParams = l_lp

        input.layoutParams = i_lp
        input2.layoutParams = i_lp
        input3.layoutParams = i_lp

        save.layoutParams = s_lp

        input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
        input2.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
        input3.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))

        input.typeface = helper.getFont("normal")
        input2.typeface = helper.getFont("normal")
        input3.typeface = helper.getFont("normal")

        layout.addView(input)
        layout.addView(input2)
        layout.addView(input3)
        layout.addView(save)

        change_pin_dialog_builder.setView(layout)

        save.setOnClickListener {

            val old_pin = input.text.toString()
            val new_pin = input2.text.toString()
            val new_pin2 = input3.text.toString()

            if (old_pin != "" && new_pin != "" && new_pin2 != "") {
                if (old_pin.length == 4 && new_pin.length == 4 && new_pin.length == 4) {

                    if (new_pin == new_pin2) {
                        sendChangePIN(old_pin, new_pin, new_pin2)
                    } else {
                        MDToast.makeText(this, "New pins does not match.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR)
                            .show()
                    }
                } else {
                    MDToast.makeText(
                        this,
                        "Please make sure all pins have 4 characters.",
                        MDToast.LENGTH_LONG,
                        MDToast.TYPE_ERROR
                    ).show()
                }
            } else {
                MDToast.makeText(this, "Please fill all fields.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            }
        }

        change_pin_dialog = change_pin_dialog_builder.create()
        change_pin_dialog.show()
    }

    // Change PIN API
    private fun sendChangePIN(old_pin: String, pin: String, pin_confirmation: String) {

        val internet: Boolean = helper.isNetworkConnected()

        if (internet) {
            val intent_splash_screen = Intent(application, IdleScreen::class.java)
            startActivityForResult(intent_splash_screen, 69)

            val IP: String = session.getIP()

            val url = "http://$IP/adminbackend/api/employee/changepin/$id"

            val stringRequest = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->

                    var strResp = response.toString()
                    val jsonObj = JSONObject(strResp)
                    val status = jsonObj.get("status").toString()

                    if (status.trim() == "success") {

                        MDToast.makeText(
                            this,
                            "Success!\nYour pin was changed.\nPlease refresh before entering your PIN.",
                            MDToast.LENGTH_LONG,
                            MDToast.TYPE_SUCCESS
                        ).show()
                        session.setMustRefresh(true)
                        finishActivity(69)
                        change_pin_dialog.cancel()
                    }
                    if (status.trim() != "success") {

                        val message_string = jsonObj.get("msg").toString()
                        MDToast.makeText(this, message_string, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                        finishActivity(69)
                    }


                }, Response.ErrorListener { error ->

                    error.printStackTrace()
                    MDToast.makeText(
                        this,
                        "Server Error! Please try again later.",
                        MDToast.LENGTH_LONG,
                        MDToast.TYPE_ERROR
                    ).show()
                    finishActivity(69)
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()

                    params["api_token"] = session.getAPIToken()
                    params["link"] = session.getLink()
                    params["old_pin"] = old_pin
                    params["pin"] = pin
                    params["pin_confirmation"] = pin_confirmation

                    return params
                }

                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()

                    params["d"] = session.getHeaders("d")
                    params["t"] = session.getHeaders("t")
                    params["token"] = session.getToken()!!

                    return params
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                60 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
        } else {
            MDToast.makeText(this, "Please check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR)
                .show()
        }
    }
}