package com.karl.kiosk.Helpers

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.Settings
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.util.TypedValue
import com.karl.kiosk.Models.Company
import com.karl.kiosk.Models.userEdtr
import com.karl.kiosk.R
import com.karl.kiosk.Sql.Helpers.CompanyContract
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID
import com.karl.kiosk.Sql.Helpers.CompanyContract.CompanyEntry.TABLE_NAME
import com.karl.kiosk.Sql.Helpers.CompanyDBHelper
import com.karl.kiosk.shared.preferences.session
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.regex.Pattern
import kotlin.random.Random
import kotlin.random.nextInt

class helper(private var mContext: Context) {

    private val session = session(mContext)
    private lateinit var CompanyDBHelper: CompanyDBHelper
    private lateinit var geocoder: Geocoder

    fun isNetworkConnected(): Boolean {

        if (session.getOfflineFlag() == "0") {
            val cm = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = cm.activeNetworkInfo

            return ni != null
        } else {
            return false
        }
    }

    fun findPendingUpdate(pending_item_LN: JSONObject) {
        val pending_updates_array = JSONArray(session.getPendingUpdates())

        val user_id = pending_item_LN.get("user_id").toString()
        val time = pending_item_LN.get("time").toString()
        val date = pending_item_LN.get("date").toString()

        if (pending_updates_array.length() > 0) {
            //Loops to be checked
            findPendingUpdate@ for (i in 0..(pending_updates_array.length() - 1)) {

                //To be checked
                val pending_update = pending_updates_array.get(i) as JSONObject
                val pending_update_user_ID = pending_update.get("user_id").toString()
                val pending_update_time = pending_update.get("time").toString()
                val pending_update_date = pending_update.get("date").toString()

                //Checks to be checked's
                //User ID,
                //time,
                //date
                if (user_id == pending_update_user_ID &&
                    time == pending_update_time &&
                    date == pending_update_date
                ) {
                    delete_pending_update(i)
                }
            }
        }
    }

    private fun delete_pending_update(position: Int) {

        var edtr_array = JSONArray(session.getPendingUpdates())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            edtr_array.remove(position)
        }

        session.setPendingUpdates(edtr_array.toString())
    }

    fun addToBeChecked(item_to_be_checked: JSONObject) {

        var pending_item_checklist = JSONArray(session.toBeChecked())

        pending_item_checklist.put(item_to_be_checked)

        session.setToBeChecked(pending_item_checklist.toString())
    }

    fun log(message: String) {

        //IF NOT WORKING
        //TRY Settings -> Application Manager -> Kiosk -> Permissions -> Enable Storage

        val company = session.getCompanyName()

        val e_dir = Environment.getExternalStorageDirectory()
        val dir = "$e_dir/Caimito Apps/Kiosk/$company/logs/"
        val logFile = File(dir, "logs.txt")

        val logFileDir = File(dir)

        if (!logFileDir.exists()) {
            logFileDir.mkdirs()

            createLogFile(logFile, message)
        } else {
            createLogFile(logFile, message)
        }
    }


    fun logRequest(message: String) {

        //IF NOT WORKING
        //TRY Settings -> Application Manager -> Kiosk -> Permissions -> Enable Storage

        val company = session.getCompanyName()

        val e_dir = Environment.getExternalStorageDirectory()
        val dir = "$e_dir/Caimito Apps/Kiosk/$company/Network/"
        val logFile = File(dir, "Network Logs.txt")

        val logFileDir = File(dir)

        if (!logFileDir.exists()) {
            logFileDir.mkdirs()

            createLogFile(logFile, message)
        } else {
            createLogFile(logFile, message)
        }
    }

    fun logLocation(message: String) {

        //IF NOT WORKING
        //TRY Settings -> Application Manager -> Kiosk -> Permissions -> Enable Storage

        val company = session.getCompanyName()

        val e_dir = Environment.getExternalStorageDirectory()
        val dir = "$e_dir/Caimito Apps/Kiosk/$company/Location History/"
        val logFile = File(dir, "Location logs.txt")

        val logFileDir = File(dir)

        if (!logFileDir.exists()) {
            logFileDir.mkdirs()

            createLogFile(logFile, message)
        } else {
            createLogFile(logFile, message)
        }
    }


    private fun createLogFile(f: File, message: String) {
        if (!f.exists()) {
            try {
                f.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(f, true))

            buf.append(message)
            buf.newLine()

            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


//    fun clearUserTime(user_id:String){
//
//        var users = JSONArray(session.getUsers())
//        val empty_json_array = JSONArray()
//
//        deleteLocalUser@for (i in 0..(users.length() - 1)) {
//
//            var user = users.get(i) as JSONObject
//
//            if (user_id == user.get("id")){
//
//                var user_edtr_array = user.get("edtr") as JSONArray
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                    user_edtr_array.remove(0)
//                    break@deleteLocalUser
//                }
//
//            }
//
//        }
//        session.setUsers(users.toString())
//    }

    fun findNameByID(ID: String): String {

        var users = JSONArray(session.getUsers())

        findNameByID@ for (i in 0..(users.length() - 1)) {

            val user = users.get(i) as JSONObject

            if (user.get("user_id").toString() == ID) {

                val userInfo = user.get("employee") as JSONObject

                return userInfo.get("fname").toString() + " " + userInfo.get("lname").toString()

                break@findNameByID
            }

        }

        return "null"
    }

    fun getUserEDTR(ID: String): userEdtr {

        var users = JSONArray(session.getUsers())
        var user_edtr = userEdtr("", "", "")

        checkIfUserHasTime@ for (i in 0..(users.length() - 1)) {

            val user = users.get(i) as JSONObject

            if (user.get("user_id").toString() == ID) {

                val string_edtr = user.get("edtr").toString()

                val userTime_array = JSONArray(string_edtr)

                if (userTime_array.length() > 0) {

                    val userTime = userTime_array.get(0) as JSONObject

                    if (userTime.length() > 0) {
                        val date = userTime.get("date_in").toString()
                        val time_in = userTime.get("time_in").toString()
                        val time_out = userTime.get("time_out").toString()

                        user_edtr.date_in = date
                        user_edtr.time_in = time_in
                        user_edtr.time_out = time_out

                        break@checkIfUserHasTime
                    }
                }
            }
        }

        return user_edtr
    }

    fun isPendingsEmpty(): Boolean {

        val edtr_array = JSONArray(session.getPendingUpdates())
        val company_location_id = session.getLocationID()
        var size = 0

        for (i in 0..(edtr_array.length() - 1)) {
            var pending_item_object: JSONObject = edtr_array.get(i) as JSONObject

            var loc_id = pending_item_object.get("location_id") as String

            if (loc_id == company_location_id) size++
        }

        return size == 0
    }

    fun checkTimeDifference(time_in: String, time_out: String, difference: Int): Boolean {

        val time_in_date = time_in.replace(":", "").toInt()
        val time_out_date = time_out.replace(":", "").toInt()

        val elapsed = time_out_date - time_in_date

        return elapsed >= difference
    }

    fun isLocation20metersAway(loc: String): Boolean {

        return true

//        if (session.getLastLocation() != "") {
//            val json_array_location = JSONArray(session.getLastLocation())
//            val json_object_location = json_array_location.get(0) as JSONObject
//
//            val lat = json_object_location.get("latitude").toString()
//            val long = json_object_location.get("longitude").toString()
//
//
//            val company_location = Location(LocationManager.NETWORK_PROVIDER)
//
//            //Pasig
//            company_location.latitude = session.getKioskLatitude().toDouble()
//            company_location.longitude = session.getKioskLongitude().toDouble()
//
//            //Zolvere
//            //company_location.latitude = 14.5708555
//            //company_location.longitude = 121.0160013
//
//            val current_location = Location(LocationManager.NETWORK_PROVIDER)
//            current_location.latitude = lat.toDouble()
//            current_location.longitude = long.toDouble()
//
//            val difference = company_location.distanceTo(current_location)
//
//            if (session.CheckLocation() == "true")
//                return difference < 50
//            else
//                return true
//
//        } else {
//            return false
//        }
    }

    fun toStringAddress(lat: Double, long: Double): String {
        geocoder = Geocoder(mContext, Locale.getDefault())

        var addresses: List<Address>
        var address = "Lat: $lat\n" +
                "Long: $long"
        var errorMessage = ""

        try {
            addresses = geocoder.getFromLocation(
                lat,
                long,
                1
            )

            if (!addresses.isEmpty()) {
                address = addresses[0].getAddressLine(0)
            }
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            errorMessage = "Service_not_available"
            Log.e(TAG, errorMessage, ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = "Invalid latitude or longitude used"
            Log.e(
                TAG, "$errorMessage. Latitude = $lat , " +
                        "Longitude =  $long", illegalArgumentException
            )
        }


        return address
    }

    fun isEmailValid(email: String): Boolean {

        val expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(email)
        return matcher.matches()
    }

    //DB Functions
    fun checkIfCompanyExist(location_id: String): Boolean {

        val location_id_column = CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID

        CompanyDBHelper = CompanyDBHelper(mContext)

        val db_read = CompanyDBHelper.readableDatabase
        val selection = "$location_id_column = ?"
        val selectionArgs = arrayOf(location_id)

        val projection = arrayOf(BaseColumns._ID)

        val cursor = db_read.query(
            CompanyContract.CompanyEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            null               // The sort order
        )

        val count = cursor.getCount()
        return count > 0
    }

    fun switchCompany(location_Id: String) {

        CompanyDBHelper = CompanyDBHelper(mContext)
        val db_read = CompanyDBHelper.readableDatabase

        val projection = arrayOf(
            CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID,
            CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LINK,
            CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME,
            CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_ID,
            CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_NAME,
            CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_EMAIL,
            CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_PASSWORD,
            CompanyContract.CompanyEntry.COLUMN_NAME_API_TOKEN,
            CompanyContract.CompanyEntry.COLUMN_NAME_D,
            CompanyContract.CompanyEntry.COLUMN_NAME_T,
            CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LAT,
            CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LONG,
            CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES
        )

        val sortOrder = "${BaseColumns._ID} DESC"

        val selection = "${COLUMN_NAME_LOCATION_ID} = ?"
        val selectionArgs = arrayOf(location_Id)

        val cursor = db_read.query(
            CompanyContract.CompanyEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        var companyArray = ArrayList<Company>()
        with(cursor) {
            while (moveToNext()) {
                val location_Id = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID))
                val link = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LINK))
                val company_name =
                    getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME))
                val timekeeper_Id =
                    getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_ID))
                val timekeeper_name =
                    getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_NAME))
                val timekeeper_email =
                    getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_EMAIL))
                val timekeeperPassword =
                    getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_PASSWORD))
                val api_token = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_API_TOKEN))
                val d = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_D))
                val t = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_T))
                val lat = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LAT))
                val long = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LONG))
                val employees = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES))

                val company = Company(
                    location_Id,
                    link,
                    company_name,
                    timekeeper_Id,
                    timekeeper_name,
                    timekeeper_email,
                    timekeeperPassword,
                    api_token,
                    d,
                    t,
                    lat.toDouble(),
                    long.toDouble(),
                    employees
                )
                companyArray.add(company)
            }
        }

        val company = companyArray.get(0)

        //Replace sessions
        session.setLink(company.Company_link)
        session.setAPIToken(company.API_token)
        session.setDatabase(company.d)
        session.setTable(company.t)
        session.setLocationID(company.Location_id)
        session.setTimekeeperName(company.Timekeeper_name)
        session.setCompanyName(company.Company_name)

        session.setTimekeeperID(company.Timekeeper_id)
        session.setTimekeeperEmail(company.Timekeeper_email)
        session.setTimekeeperPassword(company.Timekeeper_password)
        session.setUsers(company.Employees)

        db_read.close()
    }

    fun addCompany(company: Company): Boolean {

        val CompanyDBHelper = CompanyDBHelper(mContext)
        val db_write = CompanyDBHelper.writableDatabase

        //Adds to Company Details to Companies Database
        val values = ContentValues().apply {
            put(CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID, company.Location_id)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LINK, company.Company_link)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME, company.Company_name)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_ID, company.Timekeeper_id)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_NAME, company.Company_name)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_EMAIL, company.Timekeeper_email)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_PASSWORD, company.Timekeeper_password)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_API_TOKEN, company.API_token)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_D, company.d)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_T, company.t)
            put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LAT, session.getCompanyLatitude())
            put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LONG, session.getCompanyLongitude())
            put(CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES, company.Employees)
        }

        db_write.insert(CompanyContract.CompanyEntry.TABLE_NAME, null, values)

        db_write.close()

        return !db_write.equals(-1)
    }

    fun addCompanyBySession(users: String): Boolean {

        if (!checkIfCompanyExist(session.getLocationID())) {

            val CompanyDBHelper = CompanyDBHelper(mContext)
            val db_write = CompanyDBHelper.writableDatabase

            val location_id = session.getLocationID()

            //Adds to Company Details to Companies Database
            val values = ContentValues().apply {
                put(CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID, location_id)
                put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LINK, session.getLink())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME, session.getCompanyName())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_ID, session.getTimekeeperID())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_NAME, session.getTimekeeperName())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_EMAIL, session.getTimekeeperEmail())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_TIMEKEEPER_PASSWORD, session.getTimekeeperPassword())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_API_TOKEN, session.getAPIToken())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_D, session.getHeaders("d"))
                put(CompanyContract.CompanyEntry.COLUMN_NAME_T, session.getHeaders("t"))
                put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LAT, session.getCompanyLatitude())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_LONG, session.getCompanyLongitude())
                put(CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES, users)
            }

            val result = db_write.insert(CompanyContract.CompanyEntry.TABLE_NAME, null, values)
            db_write.close()

            return !result.equals(-1)
        }
        return false
    }

    fun deleteCompany(location_id: String): Boolean {

        val CompanyDBHelper = CompanyDBHelper(mContext)
        val db_write = CompanyDBHelper.writableDatabase

        val selection = "${COLUMN_NAME_LOCATION_ID} LIKE ?"

        val selectionArgs = arrayOf(location_id)

        val result = db_write.delete(TABLE_NAME, selection, selectionArgs)
        db_write.close()

        return !result.equals(-1)
    }

    fun updateCompanyEmployees(employees: String): Boolean {

        if (getCompanyUsers(session.getLocationID()) != employees) {
            val db_write = CompanyDBHelper.writableDatabase

            val values = ContentValues().apply {
                put(COLUMN_NAME_EMPLOYEES, employees)
            }

            // Which row to update, based on the title
            val selection = "${COLUMN_NAME_LOCATION_ID} LIKE ?"
            val location_id = session.getLocationID()
            val selectionArgs = arrayOf("$location_id")

            val count = db_write.update(
                TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            val result = !count.equals(-1)

            db_write.close()

            return result
        } else {
            return true
        }
    }

    private fun getCompanyUsers(location_id: String): String {

        CompanyDBHelper = CompanyDBHelper(mContext)
        val db_read = CompanyDBHelper.readableDatabase

        val projection = arrayOf(
            CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID,
            CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES
        )

        val sortOrder = "${BaseColumns._ID} DESC"

        val selection = "${COLUMN_NAME_LOCATION_ID} = ?"
        val selectionArgs = arrayOf(location_id)

        val cursor = db_read.query(
            CompanyContract.CompanyEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        var employees_list = ArrayList<String>()
        with(cursor) {
            while (moveToNext()) {
                val employees = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_EMPLOYEES))
                employees_list.add(employees)
            }
        }
        db_read.close()
        if (employees_list.size > 0) {
            return employees_list[0]
        } else {
            return ""
        }
    }

    fun randomChanceByPercentage(percentage: Int): Boolean {

        if (percentage > 1 && percentage < 101) {
            val range = IntRange(1, 100)

            val random_int = Random.nextInt(range)

            return random_int <= percentage
        } else {
            return false
        }

    }

    fun isDateTimeAutomatic(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME, 0) == 1
        } else {
            return android.provider.Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.AUTO_TIME,
                0
            ) == 1
        }
    }

    fun isTimeZoneAutomatic(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0) == 1
        } else {
            return android.provider.Settings.System.getInt(
                mContext.getContentResolver(),
                Settings.System.AUTO_TIME_ZONE,
                0
            ) == 1
        }
    }


    //FONTS
    fun getFont(font_name: String): Typeface? {

        when (font_name) {

            "normal" -> return ResourcesCompat.getFont(mContext, R.font.helveticaneue)
            "bold" -> return ResourcesCompat.getFont(mContext, R.font.helveticaneubold)
            else -> return null
        }
    }

    fun isTablet(): Boolean {

        return (mContext.getResources().getConfiguration().screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    fun integertoDP(i: Int): Float {

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, i.toFloat(), mContext.resources.displayMetrics);
    }

}