package com.karl.kiosk

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.view.View
import android.widget.*
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.Models.TimekeeperBranch
import com.karl.kiosk.shared.preferences.session
import com.karl.kiosk.volley.singleton.volley_singleton
import com.valdesekamdem.library.mdtoast.MDToast
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap

class Login : AppCompatActivity() {


    private lateinit var session: session
    private lateinit var helper: helper

    var PERMISSION_CODE = 0
    private var locationManager: LocationManager? = null

    private lateinit var r_location: Runnable
    private var h_location = Handler()

    override fun onResume() {
        super.onResume()

        checkAndAskSettingsRequirements()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = session(this)
        helper = helper(this)

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val linkField = findViewById<EditText>(R.id.linkField)

        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {

            loginButton.isEnabled = false

            val email = emailField.text
            val password = passwordField.text
            val link = linkField.text

            checkCredentials(email.toString(), password.toString(), link.toString())
        }

        if (session.isLoggedIn() == "true") {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkCredentials(email: String, password: String, link: String) {

        val helper = helper(this)
        val internet: Boolean = helper.isNetworkConnected()

        if (internet) {

            if (!email.isEmpty() && !password.isEmpty() && !link.isEmpty()) {

                if (helper.isEmailValid(email)) {
                    sendCredentials(email, password, link)
                }
                if (!helper.isEmailValid(email)) {

                    MDToast.makeText(this, "Invalid Email.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    loginButton.isEnabled = true
                }

            } else {

                MDToast.makeText(this, "Please fill all fields.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                loginButton.isEnabled = true

            }
        }
        if (!internet) {

            MDToast.makeText(this, "Check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            loginButton.isEnabled = true
        }

    }


    private fun sendCredentials(email: String, password: String, link: String) {

        val internet: Boolean = helper.isNetworkConnected()

        if (internet) {
            val intent_splash_screen = Intent(application, IdleScreen::class.java)
            startActivityForResult(intent_splash_screen, 69)

            val IP: String = session.getIP()

            val url = "http://$IP/clock/api/kiosk-login"

            val stringRequest = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->

                    val strResp = response.toString()
                    val jsonObj = JSONObject(strResp)
                    val status = jsonObj.get("status").toString()

                    if (status.trim() == "success") {

                        val msg = jsonObj.get("msg") as JSONObject

                        setSessions(msg, email, password, link)

                        //ENABLE login button
                        loginButton.isEnabled = true

                        finishActivity(69)

                        MDToast.makeText(this, "Login Successful.", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()

                        showSelectLocationDialog()
                    }
                    if (status.trim() != "success") {

                        val message_string = jsonObj.get("msg").toString()

                        MDToast.makeText(this, message_string, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()

                        //clearAll()
                        loginButton.isEnabled = true

                        finishActivity(69)
                    }

                }, Response.ErrorListener { error ->

                    error.printStackTrace()

                    MDToast.makeText(this, "Server Error :(", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()

                    loginButton.isEnabled = true

                    finishActivity(69)
                }) {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()

                    params["username"] = email
                    params["password"] = password
                    params["link"] = link

                    return params
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                20 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
        }
        if (!internet) {

            MDToast.makeText(this, "Check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            loginButton.isEnabled = true

        }

    }

    private fun clearAll() {
        emailField.text?.clear()
        passwordField.text?.clear()
        linkField.text?.clear()

        emailField.requestFocus()
    }

    private fun checkAllPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
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
                || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestAllPermission()
            }
        }
    }

    private fun requestAllPermission() {

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

    private fun minuteInterval(minutes: Int) {
        if (minutes > 0) {
            r_location = Runnable {

                h_location.postDelayed(r_location, (1000 * minutes).toLong())

            }
            h_location.postDelayed(r_location, (1000 * minutes).toLong())
        }
    }

    private fun setSessions(message: JSONObject, email: String, password: String, link: String) {

        try {
            //CAST JSONObject values AS JSONArray
            val tenant_info_array = message.get("tenant_info") as JSONArray
            val user_array = message.get("user") as JSONArray
            val user_branch_array = message.get("user_branch") as JSONArray
            val token = message.getString("token")

            //CAST all first value of JSONARRAYS AS JSONObject
            val tenant_info_object = tenant_info_array.get(0) as JSONObject
            val user_object = user_array.get(0) as JSONObject
            val user_branch_object = user_branch_array.get(0) as JSONObject

            //GET VALUES database,table,api_token,location_id
            val d = tenant_info_object.get("database").toString()
            val t = tenant_info_object.get("tbl").toString()
            val company_name = tenant_info_object.get("company_name").toString()

            val api_token = user_object.get("api_token").toString()
            //val location_id = user_branch_object.get("location_id").toString()
            val timekeeper_id = user_object.get("user_id").toString()

            val user_name_array = user_object.get("name") as JSONArray
            val user_name_object = user_name_array.get(0) as JSONObject
            val fname = user_name_object.get("fname")
            val lname = user_name_object.get("lname")

            val full_name = "$fname $lname"

            //SET these values AS session
            session.setLink(link)
            session.setAPIToken(api_token)
            session.setDatabase(d)
            session.setTable(t)


            session.setTimekeeperName(full_name)
            session.setCompanyName(company_name)

            session.setTimekeeperID(timekeeper_id)
            session.setTimekeeperEmail(email)
            session.setTimekeeperPassword(password)

            //No Location check
            session.setCompanyLatitude("")
            session.setCompanyLongitude("")


            //Will be updated when selected on next screen
            //session.setLocationID(location_id)
            session.setToken(token)
            session.setTimekeeperLocations(user_branch_array)

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


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

                locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

                MDToast.makeText(this, "Permissions granted!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
            }
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()

        finish()
    }


    private fun showSelectLocationDialog() {
        val setLocationDialog = Dialog(this)
        setLocationDialog.setContentView(R.layout.dialog_timekeeper_location_select)

        val spnnr_locations = setLocationDialog.findViewById<Spinner>(R.id.spnnr_locations)
        val cv_select_location = setLocationDialog.findViewById<CardView>(R.id.cv_select_location)

        val timekeeperLocations = session.getTimekeeperLocations()
        val locOptions = ArrayList<String>()
        var pos = 0

        repeat(timekeeperLocations.length()) {
            val branch = timekeeperLocations[pos] as JSONObject

            locOptions.add(branch.getString("branch_name"))
            pos++
        }

        val adapter = ArrayAdapter<String>(this, R.layout.layout_spiiner_item_style, locOptions)
        spnnr_locations.adapter = adapter
        //Sets default value
        //spnnr_locations.setSelection(0, false)

        var selectedLocation = ""

        val branch = timekeeperLocations[0] as JSONObject
        //selectedLocation =
        spnnr_locations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val branch = timekeeperLocations[position] as JSONObject

                selectedLocation = branch.getString("branch_id")

            }
        }


        cv_select_location.setOnClickListener {
            if (selectedLocation.isNotEmpty()) {
                session.setLocationID(selectedLocation)

                redirectToMain()
            }
        }

        setLocationDialog.show()
    }

    private fun redirectToMain() {

        session.setLoggedIn("true")

        //Construct mainactivity Intent
        val intent_main_activity = Intent(this, MainActivity::class.java)
        //GO to Users Activity
        startActivity(intent_main_activity)
    }

}
