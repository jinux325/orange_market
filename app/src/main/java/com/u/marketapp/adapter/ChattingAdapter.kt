package com.u.marketapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.u.marketapp.R
import com.u.marketapp.setting.FullImageActivity
import com.u.marketapp.vo.ChattingVO
import kotlinx.android.synthetic.main.item_chatting.view.*
import java.text.SimpleDateFormat

class ChattingAdapter(val context: Context?, private val chattingList:MutableList<ChattingVO>):
    RecyclerView.Adapter<ChattingAdapter.ViewHolder>() {

    private val myUid = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun getItemCount(): Int = chattingList.size

    @SuppressLint("RtlHardcoded", "SimpleDateFormat")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val date = SimpleDateFormat("a hh:mm")
        Log.e("chattingAdapter  ", chattingList[position].message +"  "+chattingList[position].image)
        Log.e("chattingAdapter if ", chattingList[position].uid +"   $myUid")

        val yearTime = SimpleDateFormat("yyyy년 MM월 dd일")

        /*if(chattingList.size != 0){
            holder.dateLinear.visibility = VISIBLE
            holder.dateTxt.text = yearTime.format(Date(System.currentTimeMillis()))
        }else{
            holder.dateLinear.visibility = GONE
        }*/


        if(position != 0 ){
            if (yearTime.format(chattingList[position].registDate) != yearTime.format(chattingList[position - 1].registDate)) {
                holder.dateLinear.visibility = VISIBLE
                holder.dateTxt.text = yearTime.format(chattingList[position].registDate)
            }else{
                holder.dateLinear.visibility = GONE
            }
        }else{
            holder.dateLinear.visibility = VISIBLE
            holder.dateTxt.text = yearTime.format(chattingList[position].registDate)
        }




       /* holder.dateLinear.visibility = VISIBLE
        holder.dateTxt.text = yearTime.format(chattingList[position].registDate)
*/
       /* if(chattingList.size == 0){
            holder.dateLinear.visibility = VISIBLE
            holder.dateTxt.text = yearTime.format(Date(System.currentTimeMillis()))
        }else {
            if(position > 1) {
                if (yearTime.format(chattingList[position].registDate) != yearTime.format(
                        chattingList[position - 1].registDate
                    )
                ) {
                    holder.dateLinear.visibility = VISIBLE
                    holder.dateTxt.text = yearTime.format(Date(System.currentTimeMillis()))
                }
            }
        }*/

        if(chattingList[position].imageMsg != ""){
            Log.e(" 이미지 if ", chattingList[position].message +"  "+chattingList[position].image)
            if(chattingList[position].uid.equals(myUid)){
                holder.image.visibility = GONE
                holder.linearLayout.gravity = Gravity.RIGHT
                holder.leftTime.visibility = VISIBLE
                holder.leftTime.text = date.format(chattingList[position].registDate).toString()
                holder.rightTime.visibility = GONE

            }else{
                holder.image.visibility = VISIBLE
                holder.linearLayout.gravity = Gravity.LEFT
                holder.leftTime.visibility = GONE
                holder.rightTime.visibility = VISIBLE
                holder.rightTime.text = date.format(chattingList[position].registDate)
                Glide.with(context!!).load(chattingList[position].image)
                    .apply(RequestOptions.bitmapTransform(CircleCrop())).into(holder.image)
            }
            holder.msg.visibility = GONE
            holder.imageMsg.visibility = VISIBLE
            Glide.with(context!!).load(chattingList[position].imageMsg).into(holder.imageMsg)
        }else{
            holder.msg.visibility= VISIBLE
            holder.imageMsg.visibility=GONE
            Log.e(" 이미지 else ", chattingList[position].message +"  "+chattingList[position].image)
            Log.e(" 이미지 else > if ", chattingList[position].uid +" ::::::::: "+myUid)
            if(chattingList[position].uid.equals(myUid)){
                Log.e(" 이미지 else > if ", " >>>> if ")
                holder.image.visibility = GONE
                holder.linearLayout.gravity = Gravity.RIGHT
                holder.leftTime.visibility = VISIBLE
                holder.leftTime.text = date.format(chattingList[position].registDate).toString()
                holder.rightTime.visibility = GONE
            }else{
                Log.e(" 이미지 else > if ", " >>>> else ")
                holder.image.visibility = VISIBLE
                holder.linearLayout.gravity = Gravity.LEFT
                holder.rightTime.visibility = VISIBLE
                holder.rightTime.text = date.format(chattingList[position].registDate)
                holder.leftTime.visibility = GONE
            }
            Log.d("chattingAdapter 22 ", context.toString()+" "+chattingList[position].message +"  "+chattingList[position].image)
            Glide.with(context!!).load(chattingList[position].image)
                .apply(RequestOptions.bitmapTransform(CircleCrop())).into(holder.image)
            holder.message.text = chattingList[position].message
        }

        holder.imageMsg.setOnClickListener {
            val intent = Intent(context, FullImageActivity::class.java)
            intent.putExtra("image", chattingList[position].imageMsg)
            context.startActivity(intent)
        }



    }

    inner class ViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_chatting,parent,false)) {
        val message = itemView.chatting_text!!
        val image = itemView.chatting_image!!
        val linearLayout = itemView.layout!!

        val rightTime = itemView.chatting_time_right!!
        val leftTime = itemView.chatting_time_left!!
        val msg = itemView.chatting_text!!
        val imageMsg = itemView.chatting_image_msg!!

        val dateLinear = itemView.linear_date_text!!
        val dateTxt = itemView.date_text!!
    }
}