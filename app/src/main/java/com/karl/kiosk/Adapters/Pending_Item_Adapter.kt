package com.karl.kiosk.Adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.karl.kiosk.R
import com.karl.kiosk.Helpers.helper
import com.karl.kiosk.Models.pendingItem
import com.valdesekamdem.library.mdtoast.MDToast

class Pending_Item_Adapter(private val ctx: Context, private val myDataset: ArrayList<pendingItem>) :

    RecyclerView.Adapter<Pending_Item_Adapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(view: View): RecyclerView.ViewHolder(view){

        internal var pending_update_user_id: TextView = itemView.findViewById(R.id.pending_update_user_id)
        internal var pending_update_user_name: TextView = itemView.findViewById(R.id.pending_update_user_name)
        internal var pending_update_user_date: TextView = itemView.findViewById(R.id.pending_update_user_date)
        internal var pending_update_user_time: TextView = itemView.findViewById(R.id.pending_update_user_time)
        internal var button_delete_pending_item: ImageView = itemView.findViewById(R.id.button_delete_pending_item)

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {

        val view: View
        val mInflater = LayoutInflater.from(parent.context)
        view = mInflater.inflate(R.layout.pending_update_details_activity, parent, false)

        return MyViewHolder(view)

    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        val helper = helper(ctx)

        val pending_item_id = position + 1
        val user_id = myDataset[position].user_id
        val time =  myDataset[position].time
        val date =  myDataset[position].date

        holder.pending_update_user_id.text = pending_item_id.toString()
        holder.pending_update_user_name.text = helper.findNameByID(user_id)
        holder.pending_update_user_date.text = date
        holder.pending_update_user_time.text = time

        //holder.button_delete_pending_item.setOnClickListener {
            //val helper = helper(ctx)
            //helper.delete_pending_update(position)

            //session.setMustRefresh(true)

            //helper.clearUserTime(myDataset[position].user_id)

            //myDataset.removeAt(position)
            //notifyItemRemoved(position)
            //notifyItemRangeChanged(position,myDataset.size)
            //MDToast.makeText(ctx,"This feature is temporarily disabled.", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()
        //}

        holder.button_delete_pending_item.visibility = View.GONE
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}