package com.u.marketapp.signup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.common.io.Files.getFileExtension
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.u.marketapp.activity.MainActivity
import com.u.marketapp.R
import com.u.marketapp.utils.BaseApplication
import gun0912.tedimagepicker.builder.TedImagePicker
import kotlinx.android.synthetic.main.activity_profile.*
import java.util.*

class ProfileActivity : AppCompatActivity() {

    //private val tag = "ProfileActivity"
    private var profileImage : Uri? =null
    private lateinit var phoneNumber:String
    lateinit var address:String
    var uid: String? = null

    private val db = FirebaseFirestore.getInstance()
    private var mStorageRef: StorageReference? = FirebaseStorage.getInstance().getReference("Profile")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(true)

        Glide.with(this).load(R.drawable.ic_profile)
            .apply(RequestOptions.bitmapTransform(CircleCrop())).into(proflie_imageView)

        proflie_imageView.setOnClickListener {
            Toast.makeText(this," 이미지 선택",Toast.LENGTH_SHORT).show()
            permission()
        }

        text_inpur_edit.isCounterEnabled = true
        text_inpur_edit.counterMaxLength = 10


        profile_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                when {
                    s.isEmpty() -> {
                        text_inpur_edit.error = null
                    }
                    s.length > 10 -> {
                        text_inpur_edit.error = "10자 이하로 적어주세요."
                    }
                    else -> {
                        text_inpur_edit.error = null
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })


    }

    // toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_profile, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.join -> {
                BaseApplication.instance.progressON(this@ProfileActivity, resources.getString(R.string.loading))
                val name = profile_name.text.toString()
                when {
                    name.replace(" ", "").isEmpty() -> {
                        Toast.makeText(this@ProfileActivity, "닉네임을 입력해주세요", Toast.LENGTH_LONG).show()
                    }
                    name.length >= 11 -> {
                        Toast.makeText(this@ProfileActivity, "10자 이하로 적어주세요.", Toast.LENGTH_LONG).show()
                    }
                    name.length in 1..10 -> {
                        if (profileImage == null || profileImage.toString().isEmpty()) {
                            profileImage = Uri.parse("android.resource://com.u.marketapp/drawable/ic_default")
                        }
                        try {
                            join(name)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }
                }
                true
            }
            else -> {
                Toast.makeText(applicationContext, "나머지 버튼 클릭됨", Toast.LENGTH_LONG).show()
                super.onOptionsItemSelected(item)
            }
        }
    }

    // 퍼미션 권한 설정
    private fun permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("TAG", "권한 설정 완료")
                TedImagePicker.with(this)
                    .start { uriList -> getImageList(uriList) }
            } else {
                Log.d("TAG", "권한 설정 요청")
                ActivityCompat.requestPermissions( this, arrayOf(
                    Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET), 1 )
            }
        }else{
            TedImagePicker.with(this)
                .start { uriList -> getImageList(uriList) }
        }

    }

    private fun getImageList(image: Uri){
        profileImage = image
        Glide.with(this).load(image)
            .apply(RequestOptions.bitmapTransform(CircleCrop())).into(proflie_imageView)

    }


    private fun join(name:String) {
        val intentItems = intent
        phoneNumber = intentItems.getStringExtra("phoneNumber")
        address = intentItems.getStringExtra("address")
        Log.d("@adapter@ activity ", " $phoneNumber  $address")

        val prefs = getSharedPreferences("User", Context.MODE_PRIVATE)
        val edit = prefs.edit()

        uid = FirebaseAuth.getInstance().currentUser?.uid
        val fileReference: StorageReference = mStorageRef!!.child(uid!!)
            .child(System.currentTimeMillis().toString() + "." + getFileExtension(profileImage.toString()))
        fileReference.putFile(profileImage!!).continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            fileReference.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val pref = getSharedPreferences("user", Context.MODE_PRIVATE)
                val token = pref.getString("token", "")
                val user = hashMapOf(
                   // "uid" to uid,
                    "name" to name,
                    "address" to address,
                    "regDate" to Date(System.currentTimeMillis()),
                    "imgPath" to downloadUri.toString(),
                    "token" to token,
                    "status" to 1
                )

                db.collection(resources.getString(R.string.db_user)).document(uid!!)
                    .set(user)

                edit.putString("address", address)

                //edit.apply(); //비동기 처리
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                edit.putString("log", "IN")
                edit.apply()

                BaseApplication.instance.progressOFF()
                startActivity(intent)

            } else {
                Toast.makeText(
                    this@ProfileActivity,
                    "upload failed: " + task.exception!!.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}
