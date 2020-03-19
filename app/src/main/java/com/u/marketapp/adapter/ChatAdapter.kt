package com.u.marketapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.u.marketapp.R
import com.u.marketapp.chat.ChatActivity
import com.u.marketapp.entity.ProductEntity
import com.u.marketapp.vo.ChatRoomVO
import com.u.marketapp.entity.UserEntity
import kotlinx.android.synthetic.main.item_chatroom.view.*
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(val context: Context?, private val chatList: MutableList<ChatRoomVO>):
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val myUid = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun getItemCount(): Int = chatList.size

    @SuppressLint("LongLogTag", "SimpleDateFormat", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {



        Log.e("chatting data: ", "  ${chatList[position].cId}  ${chatList[position].buyer}  ${chatList[position].comment}  ${chatList[position].seller}  ${chatList[position].pid}  ${chatList[position].registDate}")
        //val chatFragment = ChatFragment()
        if(myUid == chatList[position].buyer){
            //chatList[position].buyer?.let { chatFragment.getName(it, holder, context!!) }
            chatList[position].seller?.let { getName(it,holder) }
        }else{
           // chatList[position].seller?.let { chatFragment.getName(it, holder, context!!) }
            chatList[position].buyer?.let { getName(it,holder) }
        }
        //chatList[position].pid?.let { chatFragment.getProductImage(it, holder, context!!) }
        chatList[position].pid?.let { getProductImage(it, holder) }

        holder.comment.text = chatList[position].comment
        holder.cardView.setOnClickListener {
            Log.e("chatIntent: "," ${chatList[position].comment}  ${chatList[position].buyer}  ${chatList[position].seller}   ${chatList[position].cId}")
            if(myUid == chatList[position].buyer){
                chattingIntent(chatList[position].seller!! , chatList[position].cId!!)
            }else{
                chattingIntent(chatList[position].buyer!! , chatList[position].cId!!)
            }
        }

        /*holder.time.text = chatList[position].registDate.toString()
        holder.comment.text = chatList[position].comment*/
        /*chatList[position].let { item ->
            with(holder) {
                time.text = item.registDate.toString()
                comment.text = item.comment
                cardView.setOnClickListener {
                    Log.e("chatIntent: "," ${item.comment}  ${item.buyer}  ${item.seller}   ${chatUidList[position]}")
                    if(myUid == item.buyer){
                        //chatList[position].buyer?.let { it1 -> chatFragment.chattingIntent(it1, chatUidList[position], context!!) }
                        item.seller?.let { it1 -> chattingIntent(it1, chatUidList[position]) }
                    }else{
                        //chatList[position].seller?.let { it1 -> chatFragment.chattingIntent(it1, chatUidList[position], context!!) }
                        item.buyer?.let { it1 -> chattingIntent(it1, chatUidList[position]) }
                    }
                }
            }
        }*/


        /*chatList[position].let { item ->
                with(holder) {
                    nickname.text = item.seller
                }
            }*/
        val day = SimpleDateFormat("a hh:mm")
        val year = SimpleDateFormat("yyyy년")
        if (chatList.size >= 1 && year.format(chatList[position].registDate) == year.format(
                Date(
                    System.currentTimeMillis()
                )
            )
        ) {
            val month = SimpleDateFormat("MM월 dd일")
            if (month.format(chatList[position].registDate) == month.format(
                    Date(
                        System.currentTimeMillis()
                    )
                )
            ) {
                val dayFilter = SimpleDateFormat("hh")
                if (dayFilter.format(chatList[position].registDate) == dayFilter.format(
                        Date(
                            System.currentTimeMillis()
                        )
                    )
                ) {
                    val minute = SimpleDateFormat("mm")
                    if (minute.format(chatList[position].registDate) == minute.format(
                            Date(
                                System.currentTimeMillis()
                            )
                        )
                    ) {
                        val second = SimpleDateFormat("ss")
                        val s =
                            Integer.valueOf(second.format(Date(System.currentTimeMillis()))) - Integer.valueOf(
                                second.format(chatList[position].registDate)
                            )
                        holder.time.text = s.toString() + "초 전"
                    } else {
                        val minutes =
                            Integer.valueOf(minute.format(Date(System.currentTimeMillis()))) - Integer.valueOf(
                                minute.format(chatList[position].registDate)
                            )
                        holder.time.text = minutes.toString() + "분 전"
                    }
                } else {
                    holder.time.text = day.format(chatList[position].registDate)
                }
            } else {
                holder.time.text = month.format(chatList[position].registDate)
            }
        } else {
            if (chatList.size >= 1) holder.time.text = year.format(chatList[position].registDate)
        }


    }

    inner class ViewHolder(parent: ViewGroup): RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chatroom,parent,false)) {
        val time = itemView.chat_time!!
        val pImage = itemView.product_iamge!!
        val cardView = itemView.chat_cardview!!
        val image = itemView.chat_image!!
        val comment = itemView.chat_comment!!
        val nickname = itemView.chat_nickname!!
    }

    private fun getName(uid:String, holder:ViewHolder){
        db.collection(context!!.resources.getString(R.string.db_user)).document(uid).get()
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val userEntity: UserEntity? = task.result!!.toObject<UserEntity>(UserEntity::class.java)
                    //name = userEntity?.name.toString()
                    Log.d("getName 1231 231 2  ", userEntity?.name+"    image:   "+ userEntity?.imgPath)
                    holder.nickname.text = userEntity?.name
                    Glide.with(context).load(userEntity?.imgPath)
                        .apply(RequestOptions.bitmapTransform(CircleCrop())).into(holder.image)
                }
            }
    }

    private fun getProductImage(uid:String, holder:ViewHolder){
        db.collection("Product").document(uid).get()
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val productEntity: ProductEntity? = task.result!!.toObject<ProductEntity>(
                        ProductEntity::class.java)
                    if (context != null) Glide.with(context).load(productEntity?.imageArray!![0]).into(holder.pImage)
                }
            }
    }

    private fun chattingIntent(uid:String, chatRoomUid:String){
        db.collection(context!!.resources.getString(R.string.db_user)).document(uid).get()
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val userEntity: UserEntity? = task.result!!.toObject<UserEntity>(UserEntity::class.java)
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("chatRoomUid", chatRoomUid)
                    intent.putExtra("name", userEntity?.name.toString())
                    context.startActivity(intent)
                }
            }
    }
    /*private fun chattingRoomList(list:MutableList<String>, holder: ViewHolder){
        for(i in list){
            FirebaseFirestore.getInstance().collection("Chatting").document(i).get()
                .addOnCompleteListener{ task ->
                    if (task.isSuccessful) {
                        val chatRoomVO: ChatRoomVO? = task.result!!.toObject<ChatRoomVO>(ChatRoomVO::class.java)
                        Log.d(chatRoomVO?.buyer, chatRoomVO?.buyer)
                        Log.d(chatRoomVO?.seller, chatRoomVO?.seller)
                        Log.d(chatRoomVO?.pid, if (chatRoomVO != null) chatRoomVO.pid else null)
                        Log.d(chatRoomVO?.registDate.toString(), chatRoomVO?.registDate.toString())
                        Log.d(chatRoomVO?.comment, chatRoomVO?.comment)

                        holder.comment.text = chatRoomVO?.comment
                        if(myUid == chatRoomVO?.buyer){
                            getName(chatRoomVO?.seller!!, holder)
                        }else{
                            getName(chatRoomVO?.buyer!!, holder)
                        }
                        getProductImage(chatRoomVO?.pid!!, holder)


                        Log.d("chattingRoomList ", chatRoomVO!!.buyer)

                        chatList.add(chatRoomVO)
                        Log.d("chattingRoomList 111 ", chatList[0].buyer)

                    }
                    Log.d("chattingRoomList 444 ", chatList[0].buyer)

                    if(list.size == chatList.size){
                       *//* chat_recyclerView.layoutManager = LinearLayoutManager(context)
                        chat_recyclerView.adapter = ChatAdapter(context, chattingRoomList, chattingRoomUidList)*//*
                    }
                }
        }
    }*/
}