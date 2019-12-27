package com.karl.kiosk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONArray
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import android.app.AlertDialog
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.Gravity.*
import android.view.View.*
import android.widget.*
import com.android.volley.DefaultRetryPolicy
import com.karl.kiosk.Adapters.Users_Adapter
import com.karl.kiosk.R.layout.activity_main
import com.karl.kiosk.Services.LocationService
import com.karl.kiosk.shared.preferences.session
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.Models.user
import com.karl.kiosk.volley.singleton.volley_singleton
import com.valdesekamdem.library.mdtoast.MDToast
import java.util.*
import java.lang.Boolean.TRUE
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), RecyclerViewClickListener {

    override fun onItemClicked(pos: Int) {

        var user = if (filtered_user_list.size > 0) filtered_user_list[pos] else userList[pos]

        val intent = Intent(this, EnterPIN::class.java)
        intent.putExtra("userId", user.id)
        intent.putExtra("firstName", user.f_name)
        intent.putExtra("lastName", user.l_name)
        intent.putExtra("userImagePath", user.userImagePath)
        intent.putExtra("user_time_in", user.time_in)
        intent.putExtra("user_time_out", user.time_out)
        intent.putExtra("user_pin", user.emp_pin)

        startActivity(intent)
    }


    private lateinit var session: session
    private lateinit var helper: helper
    private lateinit var r: Runnable
    private lateinit var r2: Runnable
    private lateinit var action_offline_mode: MenuItem
    private lateinit var action_refresh_button: MenuItem

    private lateinit var timekeeper_logout_dialog: AlertDialog
    private lateinit var set_percentage_dialog: AlertDialog
    private lateinit var about_us_dialog: AlertDialog

    //For Adapter
    private var userList = ArrayList<user>()
    private var filtered_user_list = ArrayList<user>()
    private lateinit var myRV: RecyclerView
    private lateinit var myRVAdapter: Users_Adapter

    //Handlers used by runnables
    private var mHandler = Handler()
    private var tHandler = Handler()
    private var isSendingUpdates = false
    private var isGettingUsers = false
    private var isCheckingPendingUpdates = false

    private lateinit var locationService: Intent
    private var isTimerRunning = false
    private lateinit var timer: CountDownTimer

    //When App is resumed
    override fun onResume() {
        super.onResume()

        //Debug 08/22
        if (isTimerRunning) {
            timer.cancel()
        }
        //Debug 08/22

        //Shows timekeeper email
        //When email clicked: company/Name
        setTimekeeperInfo()

        checkIfMustLogout()

        search_bar.text.clear()
        button_all.performClick()

        //If flagged must refresh, calls getUsersFromAPI
        mustRefresh()

        //Must be called after inside removePeding items
        //Send Pending Updates when Updates > 0 and Not sending
        //sendIfthereArePendingUpdates()

        //Calls getUsersFromAPI daily
        dailyRefresh()

        setLastOpenedTime()

        //setLastOpenedTime()

        //Resets userList and recyclerView
        recyclerViewReset()
    }

    override fun onPause() {
        super.onPause()

        //Debug 08/22
        //If app is inactive for 6 hours
        timer = object : CountDownTimer(1000 * 60 * 60 * 6, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isTimerRunning = true
            }

            override fun onFinish() {
                isTimerRunning = false

                stopService(locationService)
                System.exit(0)
            }
        }.start()
        //Debug 08/22
    }

    override fun onStart() {
        super.onStart()

        //Debug 08/22
        //Starts location listener on start
        locationService = Intent(this, LocationService::class.java)
        startService(locationService)
        //Debug 08/22

    }

    //When app starts
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_main)

        //Sets Sessions and Helpers
        instantiateHelpersAndSessions()

        //Shows keyboard only when search is pressed
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        //Show loading screen
        whenLoading()

        //When app is opened on a different date
        //dailyRefresh()

        //Sort buttons
        groupSortButtons()

        //For the clock
        runClock(tHandler)

        //Checks internet every second
        internetListener(mHandler)

        //For search bar
        searchBarListener(this)

        //Calls getUsersFromAPI function when empty else -> adds users to recycler view
        requestUsersIfEmpty()
    }

    //Instantiate Global variables
    private fun instantiateHelpersAndSessions() {
        session = session(this)
        helper = helper(this)
        myRV = findViewById<RecyclerView>(R.id.my_recycler_view)
        myRV.setHasFixedSize(true)
        myRV.setItemViewCacheSize(20)
    }



    //Calls getUsersFromAPI function when empty else -> adds users to recycler view
    private fun requestUsersIfEmpty() {
        if (session.getUsers() == "[]") {
            whenLoading()
            getUsersFromAPI()
        } else {
            whenLoading()
            recyclerViewReset()
        }
    }

    //Sets the timekeeper's Email and information
    private fun setTimekeeperInfo() {
        //Timekeeper info
        timekeeper_name.text = session.getTimekeeperEmail()
        timekeeper_name.setOnClickListener {

            val company_name = session.getCompanyName()
            val timekeeper_name = session.getTimekeeperName()

            MDToast.makeText(this, "$company_name\n$timekeeper_name", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()
        }
    }

    //Checks internet connection.
    private fun internetListener(mHandler: Handler) {

        displayInternetStatus()

        r = Runnable {

            //Shows "Online"{green} or "You are Offline."{gray}
            displayInternetStatus()

            mHandler.postDelayed(r, 2500)

        }
        mHandler.postDelayed(r, 2500)
    }

    //Shows "Online"{green} or "You are Offline."{gray}
    private fun displayInternetStatus() {

        val internet: Boolean = helper.isNetworkConnected()
        val internetStatusLayout = findViewById<LinearLayout>(R.id.internetStatusLayout)
        val internetStatus = findViewById<TextView>(R.id.internetStatus)
        internetStatusLayout.visibility = VISIBLE

        if (!internet) {
            internetStatus.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorGray
                )
            )
            internetStatusIcon.setImageResource(R.drawable.ic_offline_pin_black_24dp)
            internetStatus.text = "Offline."

        }
        if (internet) {
            internetStatus.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorLimeGreen
                )
            )
            internetStatusIcon.setImageResource(R.drawable.ic_online_pin_limegreen_24dp)
            internetStatus.text = "Online!"
        }
    }

    //Creates thread that updates clock every second
    private fun runClock(tHandler: Handler) {

        session = session(applicationContext)
        val time12hf = session.getFormattedTime(12, "hh:mm:ss a")
        clockView.text = time12hf.replace("p.m.","PM").replace("a.m.","AM")

        r2 = Runnable {
            session = session(applicationContext)
            val time12hf = session.getFormattedTime(12, "hh:mm:ss a")

            clockView.text = time12hf.replace("p.m.","PM").replace("a.m.","AM")
            tHandler.postDelayed(r2, 1000)
        }
        tHandler.postDelayed(r2, 1000)
    }

    //GET all users from API
    private fun getUsersFromAPI() {

        hideRefreshButton(true)
        whenLoading()
        //val queue = volley_singleton.getInstance(this.applicationContext).requestQueue
        val root_IP = session.getIP()
        val location_id = session.getLocationID()

        val api_token = session.getAPIToken()

        val link = session.getLink()

        val url = "http://$root_IP/adminbackend/api/location/employee/$location_id?api_token=$api_token&link=$link"

        val stringRequest = object : StringRequest(Request.Method.GET, url,
            Response.Listener { response ->

                val response_object = JSONObject(response)
                isGettingUsers = true
                filterButtonsEnable(true)
                filterButtonsEnable(true)

                if (response_object.get("status").toString() == "success") {

                    recyclerViewClear()

                    mainLayout.gravity = NO_GRAVITY
                    users_layout.gravity = START
                    val stringUsers = response_object.get("msg").toString()
                    session.setMustRefresh(false)
                    session.clearUsers()
                    session.setUsers(stringUsers)
                    helper.addCompanyBySession(stringUsers)

                    //Test
                    helper.updateCompanyEmployees(stringUsers)
                    //Test

                    refresh_layout.visibility = INVISIBLE

                    hideRefreshButton(false)
                    addUserstoRecyclerView(myRV)

                    removePendingItems()
                }
                if (response_object.get("status").toString() != "success") {
                    if (response_object.get("msg").toString() == "Access Denied: invalid token!") {
                        MDToast.makeText(
                            this,
                            "Invalid Token!\nSomeone else is using this account.\nTimekeeper will be logged-out",
                            MDToast.LENGTH_LONG,
                            MDToast.TYPE_ERROR
                        ).show()
                        timekeeperLocalLogout()
                    }
                    //session.clearUsers()
                    whenRecyclerViewFilled()
                    hideRefreshButton(false)
                }
            },
            Response.ErrorListener {
                isGettingUsers = true

                //Only set to false
                //isCheckingPendingUpdates = false

                filterButtonsEnable(true)
                if (userList.size > 0) {
                    whenRecyclerViewFilled()
                    hideRefreshButton(false)
                } else {
                    whenEmptyUsers()
                }
            }) {
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()

                params["d"] = session.getHeaders("d")
                params["t"] = session.getHeaders("t")
                params["token"] = session.getToken()!!

                return params
            }
        }

        if (helper.isNetworkConnected()) {
            isGettingUsers = true
            filterButtonsEnable(false)
            stringRequest.retryPolicy = DefaultRetryPolicy(
                30 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            volley_singleton.getInstance(applicationContext).addToRequestQueue(stringRequest)
        } else {
            MDToast.makeText(
                this,
                "Cannot refresh users at this time.\nPLease check your internet connection.",
                MDToast.LENGTH_LONG,
                MDToast.TYPE_ERROR
            ).show()
            whenRecyclerViewFilled()
            hideRefreshButton(false)
        }
    }

    //Enables/ Disables sort buttons
    private fun filterButtonsEnable(boolean: Boolean) {
        button_all.isEnabled = boolean
        button_on_site.isEnabled = boolean
        button_not_on_site.isEnabled = boolean
    }

    //When search field is editted
    private fun searchBarListener(context: Context) {

        search_bar.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {

                    val filter = search_bar.text.toString()

                    if (filter.isNotEmpty()) {
                        //button_all.performClick()
                        filterUserList(filter)
                    } else {
                        //Crashes app
                        //button_all.performClick()
                        recyclerViewReset()
                    }
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
            }
        )
    }

    //Hides Refresh button if initialized
    private fun hideRefreshButton(hide: Boolean) {

        if (::action_refresh_button.isInitialized) {
            if (hide) {
                action_refresh_button.isVisible = false
            } else {
                action_refresh_button.isVisible = true
            }
        }


    }

    //Shows Loading screen
    private fun whenLoading() {

        users_layout.gravity = CENTER
        myRV.visibility = GONE
        refresh_layout.visibility = GONE
        no_result.visibility = GONE

        fetching_users.visibility = VISIBLE

    }

    //Shows refresh button
    private fun whenEmptyUsers() {

        //MDToast.makeText(this,"Check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()

        users_layout.gravity = CENTER
        myRV.visibility = GONE
        fetching_users.visibility = GONE
        no_result.visibility = GONE
        refresh_layout.visibility = VISIBLE
        refresh_layout.isEnabled = TRUE

        refresh_layout.setOnClickListener {
            //refresh_layout.visibility = INVISIBLE
            //refresh_layout.isEnabled = FALSE
            getUsersFromAPI()
        }
    }

    //Shows All users
    private fun whenRecyclerViewFilled() {

        users_layout.gravity = TOP
        myRV.visibility = VISIBLE
        refresh_layout.visibility = GONE
        fetching_users.visibility = GONE
        no_result.visibility = GONE
    }

    //Shows no result
    private fun whenNoResult() {

        users_layout.gravity = CENTER
        myRV.visibility = GONE
        refresh_layout.visibility = GONE
        fetching_users.visibility = GONE
        no_result.visibility = VISIBLE
    }

    //Sorts Users List by Name
    private fun filterUserList(filter: String) {

        filtered_user_list = ArrayList<user>()
        val employees = userList
        val len = employees.size
        var i = 0

        if (len > 0) {

            repeat(len) {

                val employee = employees.get(i)
                val f_name = employee.f_name
                val l_name = employee.l_name
                val w_name = "$f_name $l_name"


                val hasChar = w_name.contains(filter, ignoreCase = true)

                if (hasChar) {

                    filtered_user_list.add(employee)

                }
                i++
            }

            if (filtered_user_list.size == 0) {
                whenNoResult()
            } else {
                addToAdapter(filtered_user_list)
            }
        } else {
            addToAdapter(filtered_user_list)
        }
    }

    //Adds local JSON Userlist to RecyclerView
    private fun addUserstoRecyclerView(myRV: RecyclerView) {

        whenLoading()

        val IP = session.getIP()
        val stringUsers = session.getUsers()
        val link = session.getLink()

        val jsonArray = JSONArray(stringUsers)

        var i = 0

        repeat(jsonArray.length()) {

            var person = jsonArray.get(i) as JSONObject

            var employee = person.get("employee") as JSONObject
            var id = person.get("user_id").toString()
            var f_name = employee.get("fname") as String
            var l_name = employee.get("lname") as String
            var image_file_name = employee.get("image") as String

            var image_path = "http://$IP/adminbackend/public/assets/$link/images/users/$image_file_name"

            var emp_pin = person.get("emp_pin") as String
            var edtr_array = person.get("edtr") as JSONArray

            val user = edtrBuilder(edtr_array, id, f_name, l_name, image_path, emp_pin)
            userList.add(user)
            i++
        }
        addToAdapter(userList)
    }

    //Adds null string values to null because of kotlin null safety
    private fun edtrBuilder(
        edtr_array: JSONArray,
        id: String,
        f_name: String,
        l_name: String,
        image_path: String,
        emp_pin: String
    ): user {

        val default_time = "null"
        var user = user(id, f_name, l_name, image_path, default_time, default_time, emp_pin)

        if (edtr_array.length() > 0) {

            var employee_edtr = edtr_array.get(0) as JSONObject
            var today = session.getDate()
            var edtr_date = employee_edtr.get("date_in")

            if (employee_edtr.get("time_out") == default_time && edtr_date == today) {
                var time_in = employee_edtr.get("time_in").toString()

                user = user(id, f_name, l_name, image_path, time_in, default_time, emp_pin)
            }
            if (employee_edtr.get("time_out") != default_time && edtr_date == today) {
                var time_in = employee_edtr.get("time_in").toString()
                var time_out = employee_edtr.get("time_out").toString()

                user = user(id, f_name, l_name, image_path, time_in, time_out, emp_pin)
            }

            return user
        } else {
            return user
        }
    }

    //Add Users ArrayList to User
    private fun addToAdapter(user_list: ArrayList<user>) {

        myRVAdapter = Users_Adapter(this, user_list, this)
        Collections.sort(userList, kotlin.Comparator { o1, o2 -> return@Comparator o1.f_name.compareTo(o2.f_name) })

        if (user_list.size == 0) {
            //whenEmptyUsers()
            whenNoResult()
        } else {

            val orientation = resources.configuration.orientation

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                //myRV.layoutManager = GridLayoutManager(this, 5)
                myRV.layoutManager = GridAutofitLayoutManager(this, helper.integertoDP(100).toInt())
            }
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                //myRV.layoutManager = GridLayoutManager(this, 7)
                myRV.layoutManager = GridAutofitLayoutManager(this,helper.integertoDP(100).toInt())
            }
            myRV.adapter = myRVAdapter
            whenRecyclerViewFilled()
        }
    }

    //When Back pressed
    override fun onBackPressed() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity()
        } else {
            System.exit(1)
        }
    }

    //Resets Users List View
    private fun recyclerViewReset() {

        myRV.removeAllViews()
        userList.clear()
        filtered_user_list.clear()
        addUserstoRecyclerView(myRV)
    }

    //Removes all views from RecyclerView
    private fun recyclerViewClear() {

        session.clearUsers()
        myRV.removeAllViews()
        userList.clear()
        filtered_user_list.clear()
    }

    //Removes pending items when user employee has same time
    private fun removePendingItems() {

        //val pending_list = JSONArray(session.toBeChecked())
        val pending_updates_array = JSONArray(session.getPendingUpdates())

        //If checkList not empty
        //if (pending_list.length() > 0 && pending_updates_array.length() > 0) {
        if (pending_updates_array.length() > 0) {

            //Loops to be checked
            for (i in 0..(pending_updates_array.length() - 1)) {

                //Item to be checked
                val pending_item = JSONObject(pending_updates_array.get(i).toString())
                val to_be_checked_time = pending_item.get("time").toString()
                val user_id = pending_item.get("user_id").toString()

                //Retrieves user's time in,time out and date
                val user_edtr = helper.getUserEDTR(user_id)

                //If user already has same time in or out with to be checked time
                if (pending_item.get("date") == user_edtr.date_in) {
                    if (to_be_checked_time == user_edtr.time_in
                        || to_be_checked_time == user_edtr.time_out
                        && to_be_checked_time != "null") {

                        //finds the Pending Update and removes it
                        helper.findPendingUpdate(pending_item)
                    }
                }
            }

            isCheckingPendingUpdates = false
            sendIfthereArePendingUpdates()
        }


        //Sent Pending Updates
//        if (pending_list.length() > 0 && pending_updates_array.length() == 0) {
//            session.clearToBeChecked()
//        }
    }

    //Call this inside sendIftherearependings()

    //Filters pending updates by its session's location
    private fun getLocationPendingUpdates(i_queue_updates_array: JSONArray): String {

        var o_queue_updates_array = JSONArray()
        var new_updates_array = JSONArray()

        val company_location_id = session.getLocationID()

        for (i in 0..(i_queue_updates_array.length() - 1)) {
            var pending_item_object: JSONObject = i_queue_updates_array.get(i) as JSONObject

            var loc_id = pending_item_object.get("location_id") as String

            if (loc_id == company_location_id) o_queue_updates_array.put(pending_item_object)
            else new_updates_array.put(pending_item_object)
        }

        //Sets remaining back to Pending updates session
        session.setPendingUpdates(new_updates_array.toString())

        return o_queue_updates_array.toString()
    }


    //Sends all pending updates
    private fun sendPendingUpdates() {
        val IP = session.getIP()
        val queue_updates_string = session.getPendingUpdates()

        //val queue_updates_array = JSONArray(getLocationPendingUpdates(JSONArray(queue_updates_string)))
        val queue_updates_array = JSONArray(queue_updates_string)

        val url = "http://$IP/clock/api/kiosk"

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->

                var strResp = response.toString()
                val jsonObj = JSONObject(strResp)
                val status = jsonObj.get("status").toString()
                val message = jsonObj.get("msg").toString()

                if (status.trim() == "success") {

                    MDToast.makeText(this, "Updates sent.", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()

                    //session.clearToBeChecked()
                    session.clearPendingUpdates()
                    getUsersFromAPI()
                }
                else if (status.trim() != "success") {

                    if (message == "Access Denied: invalid token!") {
                        MDToast.makeText(
                            this,
                            "Invalid Token!\nSomeone else is using this account.\nTimekeeper will be logged-out",
                            MDToast.LENGTH_LONG,
                            MDToast.TYPE_ERROR
                        ).show()
                        timekeeperLocalLogout()
                    }

                    MDToast.makeText(this, message, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                }

                isSendingUpdates = false

            }, Response.ErrorListener { error ->

                //if (error != null && error.networkResponse != null && error.networkResponse.statusCode != null) {
                //    val error_code = error.networkResponse.statusCode
                //}

                //MDToast.makeText(
                //    this,
                //    "Server error! Unable to send updates at the moment.",
                //    MDToast.LENGTH_LONG,
                //    MDToast.TYPE_ERROR
                //).show()
                isSendingUpdates = false
            }) {

            override fun getBody(): ByteArray {

                var body_object = JSONObject()
                body_object.put("queue", queue_updates_array)
                body_object.put("api_token", session.getAPIToken())
                body_object.put("link", session.getLink())

                return body_object.toString().toByteArray()
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getHeaders(): Map<String, String> {
                var headers = HashMap<String, String>()

                headers["d"] = session.getHeaders("d")
                headers["t"] = session.getHeaders("t")
                headers["token"] = session.getToken()!!

                return headers
            }
        }

        //Timeouts call if there is no response within 60 seconds
        //Does not retry
        stringRequest.retryPolicy = DefaultRetryPolicy(
            60 * 1000, 0,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
    }

    //Calls GetUsersFromAPI function everyday to update time
    private fun dailyRefresh() {

        if (session.todayIs() == "null") {
            session.setTodayIs()

            clearTodayIsAndRefresh()
        } else {
            clearTodayIsAndRefresh()
        }
    }

    //
    private fun clearTodayIsAndRefresh() {

        if (session.todayIs() != session.getDate()) {
            getUsersFromAPI()
            session.setTodayIs()
        }
    }

    //Set time last opened
    private fun setLastOpenedTime() {

        if (session.TimeLastOpened() == "null") {
            session.setTimeLastChecked()
            checkIfSixHoursSinceLastOpenedApp()
        } else {
            checkIfSixHoursSinceLastOpenedApp()
        }
    }

    //Check if last time opened was 6 hours ago
    private fun checkIfSixHoursSinceLastOpenedApp() {
        if (helper.checkTimeDifference(session.TimeLastOpened(), session.getFormattedTime(24, "HH:mm:ss"), 60000)) {
            session.setLastLocation("")
            session.setTimeLastChecked()
        }
    }

    //Send Pending Updates when Updates > 0 and Not sending
    private fun sendIfthereArePendingUpdates() {

        val internet = helper.isNetworkConnected()
        val pendingsIsEmpty = helper.isPendingsEmpty()
        val isSending = isSendingUpdates

        if (internet && !pendingsIsEmpty && !isSending && !isCheckingPendingUpdates) {

            isSendingUpdates = true
            sendPendingUpdates()
        }
//        if (isSending && !pendingsIsEmpty) {
//            MDToast.makeText(this,
//                "Updates are still sending.",
//                MDToast.LENGTH_LONG,
//                MDToast.TYPE_INFO).show()
//        }
//        if (isCheckingPendingUpdates && !pendingsIsEmpty) {
//            MDToast.makeText(
//                this,
//                "Pending updates will be sent after users are successfully refreshed.",
//                MDToast.LENGTH_LONG,
//                MDToast.TYPE_INFO
//            ).show()
//        }
    }

    //If true, calls GetUsersFromAPI function
    private fun mustRefresh() {

        //if (session.mustRefresh() == "true") {
            isCheckingPendingUpdates = true
            getUsersFromAPI()
        //}
    }

    //Initialize button groups
    private fun groupSortButtons() {

        sort_group.setOnClickedButtonListener { button, position ->

            when (position) {
                0 -> {
                    search_bar.setText("")
                    recyclerViewReset()
                }
                1 -> {
                    search_bar.setText("")
                    sortOnSite()
                }
                2 -> {
                    search_bar.setText("")
                    //sortASC()
                    sortNotOnSite()
                }
            }
        }

        sort_group.setOnPositionChangedListener { button, currentPosition, lastPosition ->

            if (lastPosition == 1 && currentPosition == 2) {
                recyclerViewReset()
                //sortASC()
                sortNotOnSite()
            }

        }
    }

    //Sorts users alphabetically
    private fun sortASC() {

        Collections.sort(userList, kotlin.Comparator { o1, o2 -> return@Comparator o1.f_name.compareTo(o2.f_name) })

        addToAdapter(userList)
        //myRVAdapter.notifyDataSetChanged()
    }

    //Sort users who have not yet timed in
    private fun sortNotOnSite() {
        var on_site_users = ArrayList<user>()
        var i = 0
        repeat(userList.size) {

            var on_site_user = userList.get(i)

            if (on_site_user.time_in == "null") {
                on_site_users.add(on_site_user)
            }

            i++
        }

        if (on_site_users.size > 0) {
            addToAdapter(on_site_users)
            userList = on_site_users
        } else {
            whenNoResult()
        }

    }

    //Hides absent employees
    private fun sortOnSite() {

        var on_site_users = ArrayList<user>()
        var i = 0
        repeat(userList.size) {

            var on_site_user = userList.get(i)

            if (on_site_user.time_in != "null" && on_site_user.time_out == "null") {
                on_site_users.add(on_site_user)
            }

            i++
        }

        if (on_site_users.size > 0) {
            addToAdapter(on_site_users)
            userList = on_site_users
        } else {
            whenNoResult()
        }
    }

    //Calls all logout functions
    private fun timekeeperLocalLogout() {

        helper.deleteCompany(session.getLocationID())
        clearSession()
        session.setMustLogout(false)
        finish()
        MDToast.makeText(this, "Logout successful!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
    }

    //Checks if must logout is flagged
    private fun checkIfMustLogout() {
        if (session.getMustLogout() == "true") {
            MDToast.makeText(
                this,
                "Invalid Token!\nSomeone else is using this account.\nTimekeeper will be logged-out",
                MDToast.LENGTH_LONG,
                MDToast.TYPE_ERROR
            ).show()
            timekeeperLocalLogout()
        }
    }

    //Removes sessions
    private fun clearSession() {
        recyclerViewClear()
        session.clearUsers()

        session.clearPendingUpdates()
        //session.clearToBeChecked()

        session.setLink("")
        session.setLoggedIn("false")
        session.setAPIToken("")
        session.setDatabase("")
        session.setTable("")
        session.setLocationID("")
        session.setTimekeeperName("")
        session.setCompanyName("")
        session.setOfflineFlag(false)
        session.setTimekeeperLocations(JSONArray())
    }

    //Shows logout Dialog
    private fun createLogoutDialog() {
        var timekeeper_logout_dialog_builder = AlertDialog.Builder(this, R.style.ChangePinAlertDialogCustom)
        timekeeper_logout_dialog_builder.setTitle("Enter password")

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

        var i_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        i_lp.setMargins(10, 0, 10, 0)
        i_lp.gravity = Gravity.CENTER

        input.hint = "Password"
        input2.hint = "Confirm password"

        input.maxLines = 1
        input2.maxLines = 1

        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input2.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        input.setHintTextColor(resources.getColor(R.color.colorSilver))
        input2.setHintTextColor(resources.getColor(R.color.colorSilver))

        input.setTextColor(resources.getColor(R.color.colorSilver))
        input2.setTextColor(resources.getColor(R.color.colorSilver))

        var logout = Button(this)
        var s_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        logout.text = "Logout"
        logout.setTextColor(
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

        input.typeface = helper.getFont("normal")
        input2.typeface = helper.getFont("normal")

        logout.layoutParams = s_lp


        layout.addView(input)
        layout.addView(input2)
        layout.addView(logout)

        timekeeper_logout_dialog_builder.setView(layout)

        logout.setOnClickListener {

            val password = input.text.toString()
            val password_confirm = input2.text.toString()

            if (password != "" && password_confirm != "") {
                if (password == password_confirm) {

                    val timekeeper_password = session.getTimekeeperPassword()

                    if (password == timekeeper_password && password_confirm == timekeeper_password) {
                        timekeeperLocalLogout()
                    } else {
                        MDToast.makeText(
                            this,
                            "Passwords don't match our records.",
                            MDToast.LENGTH_LONG,
                            MDToast.TYPE_ERROR
                        ).show()
                    }
                    //sendChangePIN(old_pin,new_pin,new_pin2)
                } else {
                    MDToast.makeText(this, "Passwords don't match.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                }
            } else {
                MDToast.makeText(this, "Please fill all fields.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            }
        }

        timekeeper_logout_dialog = timekeeper_logout_dialog_builder.create()
        timekeeper_logout_dialog.show()

    }

    //Logout using API
//    private fun timekeeperLogout() {
//
//        val intent_splash_screen = Intent(this, IdleScreen::class.java)
//        startActivityForResult(intent_splash_screen, 69)
//
//        val IP = session.getIP()
//        val timekeeperID = session.getTimekeeperID()
//        val timekeeperPassword = session.getTimekeeperPassword()
//
//        val url = "http://$IP/clock/api/kiosk-logout"
//
//        val stringRequest = object : StringRequest(
//            Request.Method.POST, url,
//            Response.Listener { response ->
//
//                var strResp = response.toString()
//                val jsonObj = JSONObject(strResp)
//                val status = jsonObj.get("status").toString()
//                val message = jsonObj.get("msg").toString()
//
//                if (status.trim() == "success") {
//                    session.setMustLogout(false)
//                    finishActivity(69)
//                    MDToast.makeText(this, "Logout successful!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
//                    clearSession()
//                    finish()
//                }
//                if (status.trim() != "success") {
//                    finishActivity(69)
//                    MDToast.makeText(this, message, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
//                }
//
//            }, Response.ErrorListener { error ->
//                finishActivity(69)
//                MDToast.makeText(this, "Server Error! Please report to IT.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR)
//                    .show()
//            }) {
//
//            override fun getParams(): Map<String, String> {
//                val params = HashMap<String, String>()
//
//                params.put("password", timekeeperPassword)
//                params.put("api_token", session.getAPIToken())
//                params.put("user_id", timekeeperID)
//
//                return params
//            }
//
//            override fun getHeaders(): Map<String, String> {
//                var headers = HashMap<String, String>()
//
//                headers["d"] = session.getHeaders("d")
//                headers["t"] = session.getHeaders("t")
//
//                return headers
//            }
//        }
//        stringRequest.retryPolicy = DefaultRetryPolicy(
//            10 * 1000, 0,
//            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//        )
//
//        if (helper.isNetworkConnected()) {
//            volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
//        } else {
//            MDToast.makeText(
//                this,
//                "Please check your internet connection before logging out.",
//                MDToast.LENGTH_LONG,
//                MDToast.TYPE_ERROR
//            ).show()
//            finishActivity(69)
//        }
//    }

    //Enter logout PIN
    private fun showEnterLogoutDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
        alertDialogBuilder.setMessage("Enter timekeeper password.")
            .setCancelable(false)
            .setPositiveButton(
                "Goto Settings Page To Enable GPS"
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

    //Shows Change Image Capture Percentage
    private fun showSetPercentagedialog() {
        var set_percentage_dialog_builder = AlertDialog.Builder(this, R.style.ChangePinAlertDialogCustom)

        var layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        var l_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        l_lp.setMargins(0, 0, 0, 10)

        var input = EditText(this)

        var i_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        i_lp.setMargins(10, 0, 10, 0)
        i_lp.gravity = Gravity.CENTER

        var p_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        p_lp.setMargins(10, 10, 10, 0)
        p_lp.gravity = Gravity.CENTER

        //Title
        val current_percentage = TextView(this)
        current_percentage.text = "Current percentage: " + session.getCapturePercentage() + "%"
        current_percentage.typeface = helper.getFont("normal")
        current_percentage.gravity = Gravity.CENTER
        current_percentage.layoutParams = p_lp
        //set_percentage_dialog_builder.setCustomTitle(title)
        set_percentage_dialog_builder.setTitle("Set Percentage:")

        input.hint = "%"
        input.typeface = helper.getFont("normal")

        input.maxLines = 1
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setHintTextColor(resources.getColor(R.color.colorSilver))
        input.setTextColor(resources.getColor(R.color.colorSilver))

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
        save.layoutParams = s_lp

        input.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(3))

        layout.addView(current_percentage)
        layout.addView(input)
        layout.addView(save)

        set_percentage_dialog_builder.setView(layout)

        save.setOnClickListener {

            val percentage = input.text.toString()
            val int_percentage = percentage.toInt()

            if (int_percentage < 0 || int_percentage > 100) {
                MDToast.makeText(
                    this,
                    "Percentage should be greater than 0 and less than 100.",
                    MDToast.LENGTH_LONG,
                    MDToast.TYPE_ERROR
                ).show()
            } else {
                session.setCapturePercentage(int_percentage)
                MDToast.makeText(this, "Percentage set!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
                set_percentage_dialog.dismiss()
            }
        }

        set_percentage_dialog = set_percentage_dialog_builder.create()

        set_percentage_dialog.show()
    }

    //Shows App Information
    private fun showAboutUsdialog() {
        var about_us_dialog_builder = AlertDialog.Builder(this, R.style.ChangePinAlertDialogCustom)
        about_us_dialog_builder.setTitle("About Us")

        var p_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        p_lp.setMargins(10, 10, 10, 0)
        p_lp.gravity = Gravity.CENTER

        var layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        var l_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        l_lp.setMargins(0, 0, 0, 30)
        layout.layoutParams = l_lp


        //Title
        val version = session.getVersion()

        val content = TextView(this)
        content.text =
            "Caimito Apps\n" +
                    "Version $version\n\n" +
                    "Fixed cannot access Enter PIN Page when time is adjusted\n"
        "Fixing app is crashing when indled for a long time.\n"
        content.typeface = helper.getFont("normal")
        content.gravity = Gravity.CENTER
        content.layoutParams = p_lp

        var clicked = 0;

        content.setOnClickListener {
            clicked++

            if (clicked == 5) {

                val intnt = Intent(this, AdminPageActivity::class.java)
                startActivity(intnt)
            }
        }




        layout.addView(content)
        about_us_dialog_builder.setView(layout)

        about_us_dialog = about_us_dialog_builder.create()

        about_us_dialog.show()
    }

    //When configurations changed(ie: orientation)
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        recyclerViewReset()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.main_appbar, menu)
        action_offline_mode = menu.findItem(R.id.action_offline_mode)
        action_refresh_button = menu.findItem(R.id.action_refresh_button)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_change_company -> {

            val intent = Intent(this, CompanyList::class.java)
            startActivity(intent)

            true
        }
        R.id.action_more -> {
            if (session.getOfflineFlag() != "0") {
                action_offline_mode.title = "Online Mode"
                true
            } else {
                action_offline_mode.title = "Offline Mode"
                true
            }
        }
        R.id.action_refresh_button -> {

            button_all.performClick()
            getUsersFromAPI()

            true
        }
        R.id.action_offline_mode -> {

            if (session.getOfflineFlag() == "0") {
                session.setOfflineFlag(true)
            } else {
                session.setOfflineFlag(false)
            }
            //MDToast.makeText(this,"This function is currently disabled.", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()

            true
        }
        R.id.action_pending_updates -> {

            //Test
            val intent = Intent(this, PendingUpdatesList::class.java)
            startActivity(intent)
            //MDToast.makeText(this,"This function is currently disabled.", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()

            true
        }
        R.id.action_setings -> {

            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)

            true
        }
        R.id.action_send_pending_updates -> {

            sendIfthereArePendingUpdates()
            //MDToast.makeText(this,"This function is currently disabled.", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()

            true
        }
        R.id.action_logout -> {

            createLogoutDialog()

            true
        }
        R.id.about_us -> {

            showAboutUsdialog()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

}