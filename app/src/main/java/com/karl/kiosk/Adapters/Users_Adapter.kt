package com.karl.kiosk.Adapters

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.karl.kiosk.R
import com.karl.kiosk.Models.user
import com.karl.kiosk.RecyclerViewClickListener

class Users_Adapter(
    private val ctx: Context,
    private val myDataset: ArrayList<user>,
    private val g_onClickListener: RecyclerViewClickListener) : RecyclerView.Adapter<Users_Adapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(
        view: View,
        i_onClickListener: RecyclerViewClickListener
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {

        internal var onItemClickListener: RecyclerViewClickListener;

        internal var userImage: de.hdodenhof.circleimageview.CircleImageView
        internal var userName: TextView
        internal var cardView: CardView

        init {
            userName = itemView.findViewById(R.id.userIDTF)
            userImage = itemView.findViewById(R.id.userAvatar)
            cardView = itemView.findViewById(R.id.userCardViewID)
            onItemClickListener = i_onClickListener
            view.setOnClickListener(this)
        }

        override fun onClick(p0: View) {
            onItemClickListener.onItemClicked(
                adapterPosition
            )
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {

        val view: View
        val mInflater = LayoutInflater.from(parent.context)

        view = mInflater.inflate(R.layout.user_details_activity, parent, false)

        return MyViewHolder(view, g_onClickListener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        val space_pos = myDataset[position].f_name.indexOf(" ")
        val size = myDataset[position].f_name.length
        var fname = myDataset[position].f_name
        val initial = myDataset[position].l_name.get(0).toUpperCase().toString()
        var name = "$fname $initial."

        if (space_pos > 0) {
            fname = myDataset[position].f_name.removeRange(space_pos, size)
            name = "$fname $initial."
        }

        holder.userName.text = name

        val user_image_url = myDataset[position].userImagePath

        Glide.with(ctx)
            .load(user_image_url)
            //For placeholder
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.blank_image)
                    .centerCrop()
                    .fitCenter()
                    .override(500, 200)
            )
            .into(holder.userImage)

        val time_in = myDataset[position].time_in
        val time_out = myDataset[position].time_out

        if (time_in != "null" && time_out == "null") {
            holder.userImage.borderColor = ContextCompat.getColor(
                ctx,
                R.color.colorLimeGreen
            )
        }

        if (time_in != "null" && time_out != "null") {
            holder.userImage.borderColor = ContextCompat.getColor(
                ctx,
                R.color.colorWasActive
            )
        }

        if (time_in == "null" && time_out == "null") {
            holder.userImage.borderColor = ContextCompat.getColor(
                ctx,
                R.color.colorDarkGray
            )
        }

//        holder.cardView.setOnClickListener {
//            //            val intent = Intent(ctx, EnterPIN::class.java)
////            intent.putExtra("userId", myDataset[position].id)
////            intent.putExtra("firstName", myDataset[position].f_name)
////            intent.putExtra("lastName", myDataset[position].l_name)
////            intent.putExtra("userImagePath", myDataset[position].userImagePath)
////            intent.putExtra("user_time_in", myDataset[position].time_in)
////            intent.putExtra("user_time_out", myDataset[position].time_out)
////            intent.putExtra("user_pin", myDataset[position].emp_pin)
////            ctx.startActivity(intent)
//        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}