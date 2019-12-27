package com.karl.kiosk.Services


import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.IBinder
import android.os.Looper
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.*
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.shared.preferences.session
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class LocationService: Service() {

    private lateinit var session: session
    private lateinit var helper: helper

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var geocoder: Geocoder

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10 * 1000
        mLocationRequest.fastestInterval = 60 * 1000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Geocoder converts g
        geocoder = Geocoder(this, Locale.getDefault())

        session = session(this)
        helper = helper(this)

        getUserLocation()
    }

    private fun getUserLocation() {

        val permission_granted = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (permission_granted) {

                val mLocationCallback: LocationCallback = object : LocationCallback() {

                    override fun onLocationResult(p0: LocationResult?) {
                        super.onLocationResult(p0)

                        if (p0 is LocationResult) {
                            val location_list = p0.getLocations()

                            if (location_list.size > 0) {

                                val location = location_list.get(location_list.size - 1)

                                val lat = location.latitude
                                val long = location.longitude

                                var location_array = JSONArray()
                                var location_object = JSONObject()
                                location_object.put("latitude",lat)
                                location_object.put("longitude",long)
                                location_array.put(location_object)

                                session.setLastLocation(location_array.toString())

                                //Crashes App
                                //Use when debugging location only
                                //logLocationHistory(lat,long)
                            }
                        }
                    }

                }

                fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
        }
    }

    private fun logLocationHistory(lat:Double,long: Double){

        var address = helper.toStringAddress(lat,long)

        val date = session.getReadableDate()
        val time = session.getFormattedTime(24,"HH:mm:ss")

        val location_history =   "Date: $date\n" +
                                        "Time: $time\n" +
                                        "Location: $address\n" +
                                        "--------------------"

        helper.logLocation(location_history)
    }

}