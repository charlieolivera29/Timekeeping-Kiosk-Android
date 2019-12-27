package com.karl.kiosk

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.karl.kiosk.Adapters.AdminPageAdapter
import com.karl.kiosk.Adapters.emp
import com.karl.kiosk.shared.preferences.session
import org.json.JSONArray
import org.json.JSONObject

class AdminPageActivity : AppCompatActivity() {

    private lateinit var rv_employee_data: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_page)

        val session = session(this);

        rv_employee_data = findViewById(R.id.rv_employee_data)
        rv_employee_data.setHasFixedSize(true)
        rv_employee_data.setItemViewCacheSize(20)
        rv_employee_data.setLayoutManager(LinearLayoutManager(this))


        val ja = JSONArray(session.getUsers())

        val dataSet = ArrayList<emp>()
        dataSet.clear()

        if (ja.length() != 0) {

            var i = 0;
            repeat(ja.length()) {

                val emp_jo = JSONObject(ja[i].toString())
                val emp_profile = JSONObject(emp_jo.getString("employee"))

                dataSet.add(
                    emp(
                        emp_profile.getString("fname").plus(" ").plus(emp_profile.getString("lname")),
                        emp_jo.getString("user_id"),
                        ja[i].toString()
                            .replace(",","\n")
                            .replace("{","{\n")
                            .replace("}","\n}")
                    )
                )

                i++
            }
        }


        val adminPageAdapter = AdminPageAdapter(this,dataSet)
        //adminPageAdapter.notifyDataSetChanged()
        rv_employee_data.adapter = adminPageAdapter

    }

}