package com.karl.kiosk.Adapters

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.karl.kiosk.Models.CompanyLN
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.R
import com.karl.kiosk.shared.preferences.session
import com.valdesekamdem.library.mdtoast.MDToast
import android.support.v7.widget.PopupMenu

class Company_Adapter(private val ctx: Activity, private val myDataset: ArrayList<CompanyLN>) :

    RecyclerView.Adapter<Company_Adapter.MyViewHolder>() {
        class MyViewHolder(view: View) : RecyclerView.ViewHolder(view),View.OnCreateContextMenuListener {

            override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
                menu?.setHeaderTitle("Select an Option.")
                menu?.add(this.adapterPosition,1,0,"Remove Company")
            }

            internal var company_details: FrameLayout
            internal var company_status: FrameLayout
            internal var company_name: TextView
            internal var company_options: ImageView

            init {
                company_details = itemView.findViewById(R.id.company_details)
                company_name = itemView.findViewById(R.id.company_name)
                company_status = itemView.findViewById(R.id.company_status)
                company_options = itemView.findViewById(R.id.company_options)

                company_options.setOnCreateContextMenuListener(this)
            }
        }

    override fun onBindViewHolder(holder: Company_Adapter.MyViewHolder, position: Int) {

        val session = session(ctx)
        val helper = helper(ctx)
        val location_id = myDataset[position].Location_id

        val company_name = myDataset[position].Company_name

        holder.company_name.text = company_name

        holder.company_name.setOnClickListener {

            session.setMustRefresh(true)
            helper.switchCompany(location_id)
            MDToast.makeText(ctx, "Company $company_name selected.", MDToast.LENGTH_LONG, MDToast.TYPE_SUCCESS).show()
            ctx.finish()
        }

        holder.company_options.setOnClickListener {
            val popupMenu = PopupMenu(ctx, holder.company_options, Gravity.START)
            popupMenu.menuInflater.inflate(R.menu.company_options_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                        R.id.remove_company -> {

                        helper.deleteCompany(myDataset[position].Location_id)
                        myDataset.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, myDataset.size);
                        true }
                        else -> { false }
                }
            }
            popupMenu.show()
        }
    }


    override fun getItemCount() = myDataset.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Company_Adapter.MyViewHolder {

        val view: View
        val mInflater = LayoutInflater.from(parent.context)

        view = mInflater.inflate(R.layout.company_details_activity, parent, false)

        return Company_Adapter.MyViewHolder(view)
    }
}