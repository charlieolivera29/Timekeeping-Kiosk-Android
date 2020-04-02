package com.karl.kiosk.shared.preferences

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.karl.kiosk.Models.KioskLocation
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class session(
    cntx: Context,
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(cntx)
) {

    // COMPANY
    fun setCompanyName(name: String) {
        prefs.edit().putString("company_name", name).apply()
    }

    fun getCompanyName(): String {

        return prefs.getString("company_name", "")
    }

    fun setCompanyLatitude(latitude: String) {

        prefs.edit().putString("company_latitude", latitude).apply()
    }

    fun getCompanyLatitude(): String {
        return prefs.getString("company_latitude", "")
    }

    fun setCompanyLongitude(longitude: String) {

        prefs.edit().putString("company_longitude", longitude).apply()
    }

    fun getCompanyLongitude(): String {
        return prefs.getString("company_longitude", "")
    }

    // Company Name
    fun setLink(link: String) {

        prefs.edit().putString("link", link).apply()
    }

    fun getLink(): String {

        return prefs.getString("link", "")
    }

    fun setKioskLatitude(kiosk_lat: String) {

        prefs.edit().putString("Kiosk Latitude", "$kiosk_lat").apply()
    }

    fun getKioskLatitude(): String {
        return prefs.getString("Kiosk Latitude", "0.0")
    }

    fun setKioskLongitude(kiosk_long: String) {

        prefs.edit().putString("Kiosk Longitude", "$kiosk_long").apply()
    }

    fun getKioskLongitude(): String {
        return prefs.getString("Kiosk Longitude", "0.0")
    }

    fun setCheckLocation(check: Boolean) {

        if (!check)
            prefs.edit().putString("Location difference", "false").apply()
        else
            prefs.edit().putString("Location difference", "true").apply()
    }

    fun CheckLocation(): String {
        return prefs.getString("Location difference", "false")
    }


    // TIMEKEEPER

    //ID
    fun setTimekeeperID(id: String) {
        prefs.edit().putString("timekeeper_id", id).apply()

    }

    fun getTimekeeperID(): String {

        return prefs.getString("timekeeper_id", "")
    }

    //Name
    fun setTimekeeperName(full_name: String) {

        prefs.edit().putString("Full Name", full_name).apply()
    }

    fun getTimekeeperName(): String {

        return prefs.getString("Full Name", "")
    }

    //Email
    fun setTimekeeperEmail(email: String) {

        prefs.edit().putString("TimekeeperEmail", email).apply()

    }

    fun getTimekeeperEmail(): String {

        return prefs.getString("TimekeeperEmail", "")

    }

    //Password
    fun setTimekeeperPassword(password: String) {

        prefs.edit().putString("TimekeeperPassword", password).apply()

    }

    fun getTimekeeperPassword(): String {

        return prefs.getString("TimekeeperPassword", "")
    }


    // Users
    fun setUsers(Userlist: String) {
        prefs.edit().putString("Users", Userlist).apply()
    }

    fun getUsers(): String {
        return prefs.getString("Users", "[]")
    }

    fun clearUsers() {
        prefs.edit().putString("Users", "[]").apply()
    }


    // DATES AND TIME
    fun getReadableDate(): String {
        val timeInMillis = System.currentTimeMillis()
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = timeInMillis
        val dateFormat = SimpleDateFormat(
            "MMMM dd, yyyy"
        )
        return dateFormat.format(cal1.time)
    }

    fun getFormattedTime(format: Int, pattern: String): String {

        if (format == 24) {
            val timeInMillis = System.currentTimeMillis()
            val cal1 = Calendar.getInstance()
            cal1.timeInMillis = timeInMillis
            val dateFormat = SimpleDateFormat(
                pattern
            )

            return dateFormat.format(cal1.time)
        }
        if (format == 12) {

            val timeInMillis = System.currentTimeMillis()
            val cal1 = Calendar.getInstance()
            cal1.timeInMillis = timeInMillis
            val dateFormat = SimpleDateFormat(
                pattern
            )

            return dateFormat.format(cal1.time)
        } else {
            return "Error.Use 12 or 24 for parameters."
        }
    }

    fun getDate(): String {

        val fd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return fd.toString()
    }


    // TO SEND
    fun setPendingUpdates(TimeUpdates: String) {
        prefs.edit().putString("time_updates", TimeUpdates).apply()
    }

    fun getPendingUpdates(): String {
        return prefs.getString("time_updates", "[]")
    }

    fun clearPendingUpdates() {
        prefs.edit().putString("time_updates", "[]").apply()
    }


    //Deprecated
    //TO BE CHECKED
    fun setToBeChecked(pending_items: String) {

        prefs.edit().putString("To be checked", pending_items).apply()
    }

    fun toBeChecked(): String {
        return prefs.getString("To be checked", "[]")
    }

    fun clearToBeChecked() {
        prefs.edit().putString("To be checked", "[]").apply()
    }
    //Deprecated

    // SESSIONS
    // Will be deleted when logged out
    fun setAPIToken(api_token: String) {

        prefs.edit().putString("API Token", api_token).apply()
    }

    fun getAPIToken(): String {

        return prefs.getString("API Token", "")
    }

    fun setDatabase(d: String) {

        prefs.edit().putString("database", d).apply()
    }

    fun setTable(t: String) {

        prefs.edit().putString("table", t).apply()
    }

    fun getHeaders(d_or_t: String): String {

        if (d_or_t == "d") {

            return prefs.getString("database", "")
        }
        if (d_or_t == "t") {

            return prefs.getString("table", "")
        } else {

            return "null "
        }

    }

    fun setLocationID(location_id: String) {

        prefs.edit().putString("Location ID", location_id).apply()
    }

    fun getLocationID(): String {

        return prefs.getString("Location ID", "")
    }

    fun setCapturePercentage(percentage: Int) {

        if (percentage in 0..100) {
            prefs.edit().putString("Capture Percentage", percentage.toString()).apply()
        }
    }

    fun getCapturePercentage(): String {

        return prefs.getString("Capture Percentage", "0")
    }


    // FLAGS
    fun setOfflineFlag(bool: Boolean) {

        if (bool) {
            prefs.edit().putString("Offline", "1").apply()
        } else {
            prefs.edit().putString("Offline", "0").apply()
        }
    }

    fun getOfflineFlag(): String {

        return prefs.getString("Offline", "0")
    }

    fun setMustRefresh(bool: Boolean) {

        if (bool) {
            prefs.edit().putString("must_refresh", "true").apply()
        } else {
            prefs.edit().putString("must_refresh", "false").apply()
        }

    }

    fun mustRefresh(): String {

        return prefs.getString("must_refresh", "false")
    }

    fun setLoggedIn(true_or_false: String) {

        prefs.edit().putString("isLoggedIn", true_or_false).apply()

    }

    fun isLoggedIn(): String {

        return prefs.getString("isLoggedIn", "false")
    }

    fun setMustLogout(bool: Boolean) {
        if (bool) {
            prefs.edit().putString("must_logout", "true").apply()
        } else {
            prefs.edit().putString("must_logout", "false").apply()
        }
    }

    fun getMustLogout(): String {
        return prefs.getString("must_logout", "false")
    }


    // iP
    fun getIP(): String {

        //live
        //return "timekeeping-kiosk.iplusapps.com/prod4"

        //live2
//        return "timekeeping.caimitoapps.com:8081"

        //Local
        return "192.168.1.96/timekeeping/timekeeping-backend"
    }


    //UPDATED AUTOMATICALLY
    fun setTodayIs() {

        prefs.edit().putString("today", getDate()).apply()
    }

    fun todayIs(): String {

        return prefs.getString("today", "null")
    }

    fun setTimeLastChecked() {

        prefs.edit().putString("time_last_opened", getFormattedTime(24, "HH:mm:ss")).apply()
    }

    fun TimeLastOpened(): String {

        return prefs.getString("time_last_opened", "null")
    }

    fun setLastLocation(location: String) {
        prefs.edit().putString("location", location).apply()
    }

    fun getLastLocation(): String {
        return prefs.getString("location", "")
    }

    fun setTimekeeperLocations(locations: JSONArray) {
        prefs.edit().putString("locations", locations.toString()).apply()
    }

    fun getTimekeeperLocations(): JSONArray {

        val locations = prefs.getString("locations", "[]")

        return JSONArray(locations)
    }

    fun setToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? {

        return prefs.getString("token", "")
    }


    //STATIC
    fun getVersion(): String {

        //Do not replace when updating the app
        return prefs.getString("Kiosk Version", "1.0")
    }
}
