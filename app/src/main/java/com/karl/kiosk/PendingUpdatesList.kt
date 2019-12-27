package com.karl.kiosk

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import com.karl.kiosk.Adapters.Pending_Item_Adapter
import com.karl.kiosk.Models.pendingItem
import com.karl.kiosk.shared.preferences.session
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PendingUpdatesList : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_updates_list)

        var pending_updates_rv = findViewById<RecyclerView>(R.id.pending_updates_recycler_view)
        var pending_item_array = ArrayList<pendingItem>()

        val session = session(this)

        supportActionBar!!.title = "Pending Updates"

        val pending_updates = session.getPendingUpdates()
        val pending_update_JSON_array = JSONArray(pending_updates)
        val pending_update_array = reverseOrder(pending_update_JSON_array)
        //val pending_update_array = pending_update_JSON_array

        val company_location_id = session.getLocationID()

        for (i in 0..(pending_update_array.length() - 1)) {
            var pending_item_object:JSONObject = pending_update_array.get(i) as JSONObject

            var loc_id = pending_item_object.get("location_id") as String

                if (loc_id == company_location_id) {

                    var pending_item = pendingItem(
                        pending_item_object.get("user_id") as String,
                        pending_item_object.get("time") as String,
                        pending_item_object.get("reference") as String,
                        pending_item_object.get("pin") as String,
                        pending_item_object.get("date") as String,
                        loc_id
                    )
                    pending_item_array.add(pending_item)
                }
        }

        val myRVAdapter = Pending_Item_Adapter(this,pending_item_array)
        pending_updates_rv.layoutManager = GridLayoutManager(this, 1)

        pending_updates_rv.adapter = myRVAdapter
    }

    private fun reverseOrder(pendings:JSONArray):JSONArray{

        var pu_array = ArrayList<pendingItem>()

        for (i in 0..(pendings.length() - 1)) {

            var pending_item_object: JSONObject = pendings.get(i) as JSONObject
            var u_id = pending_item_object.get("user_id") as String
            var time = pending_item_object.get("time") as String
            var reference = pending_item_object.get("reference") as String
            var pin = pending_item_object.get("pin") as String
            var date = pending_item_object.get("date") as String
            var location_id = pending_item_object.get("location_id") as String

            var pu_object = pendingItem(u_id,time,reference,pin,date,location_id)

            pu_array.add(pu_object)
        }

        Collections.reverse(pu_array)

        var pu_json_array = JSONArray()

        var i = pu_array.size - 1

        repeat(pu_array.size){

            var pending_item_object: JSONObject = pendings.get(i) as JSONObject

            var pu_json_object = JSONObject()
            pu_json_object.put("user_id",pending_item_object.get("user_id") as String)
            pu_json_object.put("time",pending_item_object.get("time") as String)
            pu_json_object.put("reference",pending_item_object.get("reference") as String)
            pu_json_object.put("pin",pending_item_object.get("pin") as String)
            pu_json_object.put("date",pending_item_object.get("date") as String)
            pu_json_object.put("location_id",pending_item_object.get("location_id") as String)

            pu_json_array.put(pu_json_object)

            i--
        }

        return pu_json_array
    }

    override fun onBackPressed() {
        super.onBackPressed()

        finish()
    }
}

