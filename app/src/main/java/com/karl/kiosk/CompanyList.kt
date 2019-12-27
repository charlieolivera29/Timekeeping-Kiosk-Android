package com.karl.kiosk

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.BaseColumns
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.karl.kiosk.Adapters.Company_Adapter
import com.karl.kiosk.Models.Company
import com.karl.kiosk.Models.CompanyLN
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.Sql.Helpers.CompanyContract
import com.karl.kiosk.Sql.Helpers.CompanyDBHelper
import com.karl.kiosk.shared.preferences.session
import com.karl.kiosk.volley.singleton.volley_singleton
import com.valdesekamdem.library.mdtoast.MDToast
import kotlinx.android.synthetic.main.activity_company_list.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE
import java.util.HashMap

class CompanyList : AppCompatActivity() {

    private lateinit var session: session
    private lateinit var helper: helper
    private lateinit var login_button: Button
    private lateinit var add_company_dialog: AlertDialog
    private lateinit var company_adapter: Company_Adapter
    private var company_array = ArrayList<CompanyLN>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_list)

        helper = helper(this)
        session = session(this)

        //Adds logged in company to top
        addLoggedInCompany()

        //Adds all companies to List
        addCompaniesToRV()

        //Adds Company when clicked
        fabAddCompany.setOnClickListener {

            createLoginDialog()
        }

    }

    private fun addLoggedInCompany(){
        using_company_name.text = session.getCompanyName()
    }

    private fun addCompaniesToRV(){

        val CompanySqlHelper = CompanyDBHelper(this)
        val db_read = CompanySqlHelper.readableDatabase

        val projection = arrayOf(
            CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID,
            CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME
        )

        val sortOrder = "${BaseColumns._ID} DESC"

        // Define 'where' part of query.
        val selection = "${CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID} NOT LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(session.getLocationID())

        val cursor = db_read.query(
            CompanyContract.CompanyEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )

        with(cursor) {
            while (moveToNext()) {
                val location_Id = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_LOCATION_ID))
                val company_name = getString(cursor.getColumnIndex(CompanyContract.CompanyEntry.COLUMN_NAME_COMPANY_NAME))

                val company = CompanyLN(
                    location_Id,
                    company_name
                )
                company_array.add(company)
            }
        }
        db_read.close()

        company_adapter = Company_Adapter(this,company_array)
        companyRV.layoutManager = GridLayoutManager(this, 1)
        companyRV.adapter = company_adapter
    }

    private fun createLoginDialog(){

        var add_company_dialog_builder = AlertDialog.Builder(this,R.style.ChangePinAlertDialogCustom)
        add_company_dialog_builder.setTitle("Add Company")

        var layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        var l_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        l_lp.setMargins(0,0,0,10)

        var email_field = EditText(this)
        var password_field = EditText(this)
        var link_field = EditText(this)

        var i_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        i_lp.setMargins(10,0,10,0)
        i_lp.gravity = Gravity.CENTER

        email_field.hint = "Email"
        password_field.hint = "Password"
        link_field.hint = "Link"

        email_field.maxLines = 1
        password_field.maxLines = 1
        link_field.maxLines = 1

        email_field.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        password_field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        link_field.inputType = InputType.TYPE_CLASS_TEXT

        email_field.setHintTextColor(getResources().getColor(R.color.colorSilver))
        password_field.setHintTextColor(getResources().getColor(R.color.colorSilver))
        link_field.setHintTextColor(getResources().getColor(R.color.colorSilver))

        email_field.setTextColor(getResources().getColor(R.color.colorSilver))
        password_field.setTextColor(getResources().getColor(R.color.colorSilver))
        link_field.setTextColor(getResources().getColor(R.color.colorSilver))

        email_field.typeface = helper.getFont("normal")
        password_field.typeface = helper.getFont("normal")
        link_field.typeface = helper.getFont("normal")

        login_button = Button(this)
        var s_lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        login_button.text = "Login"
        login_button.setTextColor(
            ContextCompat.getColor(this,
                R.color.colorDarkGray))
        s_lp.setMargins(0,0,10,10)
        s_lp.gravity = Gravity.END

        layout.layoutParams = l_lp

        email_field.layoutParams = i_lp
        password_field.layoutParams = i_lp
        link_field.layoutParams = i_lp

        login_button.layoutParams = s_lp

        layout.addView(email_field)
        layout.addView(password_field)
        layout.addView(link_field)
        layout.addView(login_button)

        add_company_dialog_builder.setView(layout)

        login_button.setOnClickListener {

            login_button.isEnabled = FALSE
            val email = email_field.text.toString()
            val password = password_field.text.toString()
            val link = link_field.text.toString()

            checkCredentials(email,password,link)
        }

        add_company_dialog = add_company_dialog_builder.create()
        add_company_dialog.show()
    }

    private fun checkCredentials(email:String,password:String,link:String){

        val internet:Boolean = helper.isNetworkConnected()

        if(internet) {
            if (!email.isEmpty() && !password.isEmpty() && !link.isEmpty()) {

                if (helper.isEmailValid(email)) {
                    sendLoginCredentials(email, password, link)
                }
                if (!helper.isEmailValid(email)) {
                    MDToast.makeText(this,"Invalid Email.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    login_button.isEnabled = TRUE
                }
            } else {
                MDToast.makeText(this,"Please fill all fields.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                login_button.isEnabled = TRUE
            }
        }
        if(!internet){
            MDToast.makeText(this,"Check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            login_button.isEnabled = TRUE
        }
    }

    private fun sendLoginCredentials(email:String,password:String,link:String){

        val internet:Boolean = helper.isNetworkConnected()

        if(internet){

            val intent_splash_screen = Intent(this, IdleScreen::class.java)
            startActivityForResult(intent_splash_screen, 69)

            val IP:String = session.getIP()
            val url = "http://$IP/clock/api/kiosk-login"

            val stringRequest = object : StringRequest(
                Request.Method.POST, url,
                Response.Listener { response ->

                    var strResp = response.toString()
                    val jsonObj = JSONObject(strResp)
                    val status = jsonObj.get("status").toString()

                    if(status.trim() == "success"){

                        val msg = jsonObj.get("msg") as JSONObject

                        getResultsAsCompany(msg,email,password,link)
                    }
                    if(status.trim() != "success"){

                        val message_string = jsonObj.get("msg").toString()
                        MDToast.makeText(this,message_string, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                        finishActivity(69)
                        login_button.isEnabled = TRUE
                    }

                }, Response.ErrorListener { error ->

                    error.printStackTrace()

                    MDToast.makeText(this,"Server Error :(", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    finishActivity(69)
                    login_button.isEnabled = TRUE
                })
            {
                override fun getParams(): Map<String, String> {
                    val params = HashMap<String, String>()

                    params["username"] = email
                    params["password"] = password
                    params["link"] = link

                    return params
                }
            }

            stringRequest.retryPolicy = DefaultRetryPolicy(
                10 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
        }
        if(!internet){
            MDToast.makeText(this,"Check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            finishActivity(69)
            login_button.isEnabled = TRUE
        }
    }

    private fun getResultsAsCompany(message: JSONObject, email:String, password:String, link:String){

        //CAST JSONObject values AS JSONArray
        val tenant_info_array = message.get("tenant_info") as JSONArray
        val user_array = message.get("user") as JSONArray
        val user_branch_array = message.get("user_branch") as JSONArray

        //CAST all first value of JSONARRAYS AS JSONObject
        val tenant_info_object = tenant_info_array.get(0) as JSONObject
        val user_object = user_array.get(0) as JSONObject
        val user_branch_object = user_branch_array.get(0) as JSONObject

        //GET VALUES database,table,api_token,location_id
        val d = tenant_info_object.get("database").toString()
        val t = tenant_info_object.get("tbl").toString()
        val company_name = tenant_info_object.get("company_name").toString()

        val api_token = user_object.get("api_token").toString()
        val location_id = user_branch_object.get("location_id").toString()
        val timekeeper_id = user_object.get("user_id").toString()

        val user_name_array = user_object.get("name") as JSONArray
        val user_name_object = user_name_array.get(0) as JSONObject
        val fname = user_name_object.get("fname")
        val lname = user_name_object.get("lname")

        val full_name = "$fname $lname"

        val company = Company(
            location_id,
            link,
            company_name,
            timekeeper_id,
            full_name,
            email,
            password,
            api_token,
            d,
            t,
            0.0,
            0.0,
            "[]"
        )

        getUsersFromAPI(company)
    }

    private fun getUsersFromAPI(company: Company){

        val IP = session.getIP()
        val location_id = company.Location_id
        val api_token = company.API_token
        val link = company.Company_link

        val url = "http://$IP/adminbackend/api/location/employee/$location_id?api_token=$api_token&link=$link"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener { response ->
                val response_object = JSONObject(response)

                if(response_object.get("status").toString() == "success"){

                    val stringUsers = response_object.get("msg").toString()
                    //If company does not exists add Company
                    company.Employees = stringUsers

                    if (helper.addCompany(company)){
                        MDToast.makeText(this,"Success!", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()

                        company_array.clear()
                        addCompaniesToRV()

                        company_adapter.notifyDataSetChanged()
                        finishActivity(69)
                        login_button.isEnabled = TRUE
                        add_company_dialog.cancel()
                    }else{
                        MDToast.makeText(this,"DB error!\nCannot add to Companies.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                        finishActivity(69)
                        login_button.isEnabled = TRUE
                    }
                }
                if(response_object.get("status").toString() != "success"){
                    MDToast.makeText(this,response_object.get("msg").toString(), MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    finishActivity(69)
                    login_button.isEnabled = TRUE
                }
            },
            Response.ErrorListener {
                MDToast.makeText(this,"Server Error :(", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                finishActivity(69)
                login_button.isEnabled = TRUE
            }) {
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()

                params["d"] = company.d
                params["t"] = company.t

                return params
            }
        }

        if(!helper.isNetworkConnected()){
            MDToast.makeText(this,"Please check your internet connection.", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
            finishActivity(69)
            login_button.isEnabled = TRUE
        }
        if(helper.isNetworkConnected()){
            stringRequest.retryPolicy = DefaultRetryPolicy(
                20 * 1000, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            volley_singleton.getInstance(this).addToRequestQueue(stringRequest)
        }
    }

}
