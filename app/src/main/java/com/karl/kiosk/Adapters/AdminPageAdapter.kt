package com.karl.kiosk.Adapters

import android.app.Activity
import android.app.Dialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.karl.kiosk.R

data class emp(
    var Name: String,
    var UserID: String,
    var Details: String
)

class AdminPageAdapter(private val ctx: Activity, private val myDataset: ArrayList<emp>) :

    RecyclerView.Adapter<AdminPageAdapter.MyViewHolder>() {
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        internal var employee_json: TextView

        init {
            employee_json = itemView.findViewById(R.id.employee_json)
        }
    }

    override fun onBindViewHolder(holder: AdminPageAdapter.MyViewHolder, p1: Int) {

        holder.employee_json.text = myDataset[p1].Name;
        holder.employee_json.setOnClickListener{

            val dialog = Dialog(ctx)
            dialog.setContentView(R.layout.dialog_employee_json)
            val tvDetails = dialog.findViewById<TextView>(R.id.employee_json)
            tvDetails.text = myDataset[p1].Details
            dialog.show()
        }
    }

    override fun getItemCount() = myDataset.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminPageAdapter.MyViewHolder {

        val view: View
        val mInflater = LayoutInflater.from(parent.context)

        view = mInflater.inflate(R.layout.recyclerview_json_data, parent, false)

        return AdminPageAdapter.MyViewHolder(view)
    }


}