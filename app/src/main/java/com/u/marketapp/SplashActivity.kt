package com.u.marketapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.u.marketapp.chat.FCM
import com.u.marketapp.signup.SmsActivity
import kotlinx.android.synthetic.main.activity_chat.*

class SplashActivity : AppCompatActivity() {

    private val TAG = "SplashActivity"
    private var number = "NoNumber"

    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("User", Context.MODE_PRIVATE)

        val log = prefs.getString("log", "")
        Log.e("log $TAG", log)

        val hd = Handler()

        hd.postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {

                number = FirebaseAuth.getInstance().currentUser!!.phoneNumber!!
                Log.d("유저 확인", number)
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }else{
                Log.d("로그 없음", number)
                val intent = Intent(this@SplashActivity, SmsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.putExtra("number",number)
                startActivity(intent)

            }
        }, 1000) // 1초 후 이미지를 닫습니다




    }


}
