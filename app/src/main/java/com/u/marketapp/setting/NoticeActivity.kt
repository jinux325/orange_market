package com.u.marketapp.setting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.u.marketapp.R
import com.u.marketapp.adapter.NoticeAdapter
import com.u.marketapp.vo.NoticeVO
import kotlinx.android.synthetic.main.activity_notice.*

class NoticeActivity : AppCompatActivity() {

    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice)

        setSupportActionBar(notice_toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        db.collection("Notice").orderBy("date",
            Query.Direction.DESCENDING).addSnapshotListener{ snapshot, _ ->
            val list= mutableListOf<NoticeVO>()
            for (doc in snapshot!!) {
                val noticeVO: NoticeVO = doc.toObject(NoticeVO::class.java)
                Log.e("notice  "," ${noticeVO.title}  ${noticeVO.date}")
                list.add(noticeVO)
            }

            rv_notice_setting.layoutManager = LinearLayoutManager(this)
            rv_notice_setting.adapter = NoticeAdapter(this, list)
        }


    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
