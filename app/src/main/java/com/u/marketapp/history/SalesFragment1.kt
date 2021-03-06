package com.u.marketapp.history

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.u.marketapp.R
import com.u.marketapp.activity.EditActivity
import com.u.marketapp.activity.ProductActivity
import com.u.marketapp.adapter.CustomChoiceAdapter
import com.u.marketapp.adapter.SalesHistoryRVAdapter
import com.u.marketapp.entity.ListViewItem
import com.u.marketapp.entity.ProductEntity
import com.u.marketapp.entity.UserEntity
import com.u.marketapp.utils.FirebaseUtils
import com.u.marketapp.vo.ChatRoomVO
import kotlinx.android.synthetic.main.fragment_history.view.*
import java.util.*
import kotlin.collections.ArrayList

class SalesFragment1 : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    companion object {
        private val TAG = SalesFragment1::class.java.simpleName
        private const val REQUEST_PRODUCT = 100
        private const val REQUEST_UPDATE = 200
    }

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var textView: TextView
    private lateinit var adapterSales: SalesHistoryRVAdapter
    private var selectPosition: Int = -1

    // 새로고침
    override fun onRefresh() {
        adapterSales.clear()
        requestItems()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        swipeRefreshLayout = view.swip_refresh_layout
        swipeRefreshLayout.setOnRefreshListener(this)
        recyclerView = view.recycler_view
        textView = view.text_view_empty
        registerForContextMenu(view)
        initView()
        return view
    }

    private fun initView() {
        setRVAdapter()
        setRVLayoutManager()
        checkItemsData(true)
        requestItems()
    }

    // 리사이클뷰 레이아웃 매니저 설정
    private fun setRVLayoutManager() {
        recyclerView.apply {
            setHasFixedSize(true)
            val linearlayout = LinearLayoutManager(context)
            layoutManager = linearlayout
        }
    }

    // 리사이클뷰 어댑터 설정
    private fun setRVAdapter() {
        if (recyclerView.adapter == null) {
            adapterSales = SalesHistoryRVAdapter()
            recyclerView.adapter = adapterSales
            adapterSales.setItemClickListener(object : SalesHistoryRVAdapter.ItemClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "Item Click : $selectPosition")
                    moveProductActivity(adapterSales.getItem(position).id)
                }
            })
            adapterSales.setMoreClickListener(object : SalesHistoryRVAdapter.MoreClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "More Click : $selectPosition")
                    activity?.apply {
                        openContextMenu(view)
                    }
                }
            })
            adapterSales.setTradeClickListener(object : SalesHistoryRVAdapter.TradeClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "Trade Click : $selectPosition")
                    tradeChangeItem1()
                }
            })
            adapterSales.setStateClickListener(object : SalesHistoryRVAdapter.StateClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "State Click : $selectPosition")
                    searchChatRoomList()
                }
            })
        }
    }

    // 데이터 로드
    private fun requestItems() {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_user))
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .get()
            .addOnSuccessListener { documentSnapshots ->
                val user = documentSnapshots.toObject(UserEntity::class.java)!!
                val array = user.salesArray
                if (array.size > 0) {
                    for (item in user.salesArray) {
                        getProductItem(item)
                    }
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    checkItemsData(true)
                    swipeRefreshLayout.isRefreshing = false
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 조회
    private fun getProductItem(pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_product))
            .document(pid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                Log.i(TAG, "문서 정보 상태 : ${documentSnapshot.exists()}")
                if (documentSnapshot.exists()) {
                    if (getActiveProductItem(documentSnapshot) && getStateProductItem(documentSnapshot) != 2) {
                        adapterSales.addItem(documentSnapshot)
                        checkItemsData(false)
                    }
                } else {
                    Log.i(TAG, "No such DocumentSnapshot!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 거래 상태 확인
    private fun getStateProductItem(document: DocumentSnapshot) : Int {
        val item = document.toObject(ProductEntity::class.java)!!
        return item.transactionStatus
    }

    // 상품 활성화 확인
    private fun getActiveProductItem(document: DocumentSnapshot) : Boolean {
        val item = document.toObject(ProductEntity::class.java)!!
        return item.status
    }

    // 상품 존재 여부
    private fun checkItemsData(isCheck: Boolean) {
        if (!isCheck) {
            recyclerView.visibility = View.VISIBLE
            textView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            textView.visibility = View.VISIBLE
        }
    }

    // 상품 상세 정보 페이지 이동
    private fun moveProductActivity(id: String) {
        Log.i(TAG, "Document ID : $id")
        val intent = Intent(context, ProductActivity::class.java)
        intent.putExtra("id", id)
        startActivityForResult(intent, REQUEST_PRODUCT)
    }

    private fun moveEditActivity(id: String) {
        Log.i(TAG, "Document ID : $id")
        val intent = Intent(context, EditActivity::class.java)
        intent.putExtra("pid", id)
        startActivityForResult(intent, REQUEST_UPDATE)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        activity?.apply {
            menuInflater.apply {
                inflate(R.menu.context_sales, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_pull -> { // 끌어올리기
                Log.i(TAG, "action_pull : $selectPosition")
                showPopupForPull()
                true
            }
            R.id.action_update -> { // 수정
                Log.i(TAG, "action_update : $selectPosition")
                moveEditActivity(adapterSales.getItem(selectPosition).id)
                true
            }
            R.id.action_hide -> { // 숨기기
                Log.i(TAG, "action_hide : $selectPosition")
                showPopupForHide()
                true
            }
            R.id.action_delete -> { // 삭제
                Log.i(TAG, "action_delete : $selectPosition")
                showPopupForDelete()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // 거래 상태 변경
    private fun tradeChangeItem1() {
        if (selectPosition != -1) {
            val pid = adapterSales.getItem(selectPosition).id
            val item = adapterSales.getItem(selectPosition).toObject(ProductEntity::class.java)!!
            val status = when (item.transactionStatus) {
                0 -> 1
                1 -> 0
                else -> 0
            }
            val db = FirebaseFirestore.getInstance()
            db.collection(resources.getString(R.string.db_product))
                .document(pid)
                .update("transactionStatus", status)
                .addOnSuccessListener {
                    adapterSales.removeItem(selectPosition)
                    getItem(selectPosition, pid)
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        } else {
            Log.i(TAG, "선택이 잘못되었습니다.....")
        }
    }

    // 채팅 대상 목록 검색
    private fun searchChatRoomList() {
        if (selectPosition != -1) {
            val pid = adapterSales.getItem(selectPosition).id
            val db = FirebaseFirestore.getInstance()
            db.collection(resources.getString(R.string.db_product))
                .document(pid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    val item = documentSnapshot.toObject(ProductEntity::class.java)!!
                    val array = item.chattingRoom
                    if (array.size > 0) {
                        val itemList = ArrayList<ListViewItem>()
                        Log.i(TAG, "Chatting Room Size : ${array.size}")
                        for (room in array) {
                            db.collection(resources.getString(R.string.db_chatting))
                                .document(room)
                                .get()
                                .addOnSuccessListener { documentSnapshot2 ->
                                    Log.i(TAG, "Chatting Room : ${documentSnapshot2.id}")
                                    val roomVO = documentSnapshot2.toObject(ChatRoomVO::class.java)!!
                                    val key = roomVO.buyer
                                    key?.let {
                                        db.collection(resources.getString(R.string.db_user))
                                            .document(key)
                                            .get()
                                            .addOnSuccessListener { documentSnapshot3 ->
                                                Log.i(TAG, "User : ${documentSnapshot3.id}")
                                                val user = documentSnapshot3.toObject(UserEntity::class.java)!!
                                                val viewItem = ListViewItem()
                                                viewItem.uid = documentSnapshot3.id
                                                viewItem.icon = user.imgPath
                                                viewItem.name = user.name
                                                itemList.add(viewItem)
                                                if (itemList.size >= array.size) {
                                                    showPopUpCompleteUser(itemList)
                                                }
                                            }.addOnFailureListener { e ->
                                                Log.i(TAG, e.toString())
                                            }
                                    }
                                }.addOnFailureListener { e ->
                                    Log.i(TAG, e.toString())
                                }
                        }
                    } else {
                        Toast.makeText(context, "구매 희망자가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        }
    }

    // 거래완료 대상자 검색
    private fun showPopUpCompleteUser(list: List<ListViewItem>) {
        Log.i(TAG, list.toString())
        var choiceItem = -1
        val view = ListView(context)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        view.choiceMode = ListView.CHOICE_MODE_SINGLE
        view.divider = ColorDrawable(Color.TRANSPARENT).current
        view.adapter = CustomChoiceAdapter(requireActivity(), list)
        view.setOnItemClickListener { parent, view, position, id ->
            choiceItem = position
            Log.i(TAG, "Selected Item : $choiceItem")
        }

        val builder = MaterialAlertDialogBuilder(context)
        builder.setTitle("구매자 선택")
        builder.setView(view)
        builder.setPositiveButton("확인", null)
        builder.setNegativeButton("취소", null)

        val diaogView = builder.create()
        diaogView.show()
        diaogView.setButton(AlertDialog.BUTTON_POSITIVE, "선택") { dialog, which ->
            var wantToCloseDialog = true

            if (choiceItem != -1) {
                val uid = list[choiceItem].uid
                tradeChangeItem2(uid)
                dialog.dismiss()
            } else {
                wantToCloseDialog = false
            }

            if (wantToCloseDialog) {
                dialog.dismiss()
            } else {
                Toast.makeText(context, "구매자를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        diaogView.setButton(AlertDialog.BUTTON_NEGATIVE, "취소") { dialog, which ->
            dialog.dismiss()
        }
    }

    // 거래완료 상태로 변경
    private fun tradeChangeItem2(uid: String) {
        if (selectPosition != -1) {
            val pid = adapterSales.getItem(selectPosition).id
            val db = FirebaseFirestore.getInstance()
            db.collection(resources.getString(R.string.db_product))
                .document(pid)
                .update("transactionStatus", 2)
                .addOnSuccessListener {
                    adapterSales.removeItem(selectPosition)
                    addBuyer(pid, uid)
                    addPurchase(uid, pid)
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        } else {
            Log.i(TAG, "선택이 잘못되었습니다.....")
        }
    }

    // 상품의 구매자 등록
    private fun addBuyer(pid: String, uid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_product))
            .document(pid)
            .update("buyer", uid)
            .addOnSuccessListener {
                Log.i(TAG, "구매자 등록 성공!!")
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 구매자의 구매목록 추가
    private fun addPurchase(uid: String, pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_user))
            .document(uid)
            .update("purchaseArray", FieldValue.arrayUnion(pid))
            .addOnSuccessListener {
                Log.i(TAG, "구매자의 구매목록 등록 성공!!")
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 끌어올리기
    private fun pullUpItem() {
        if (selectPosition != -1) {
            val pid = adapterSales.getItem(selectPosition).id
            val db = FirebaseFirestore.getInstance()
            db.collection(resources.getString(R.string.db_product))
                .document(pid)
                .update("modDate", Date())
                .addOnSuccessListener {
                    adapterSales.removeItem(selectPosition)
                    getItem(selectPosition, pid)
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        } else {
            Log.i(TAG, "선택이 잘못되었습니다.....")
        }
    }

    // 상품 숨기기
    private fun hideItem() {
        if (selectPosition != -1) {
            val pid = adapterSales.getItem(selectPosition).id
            val db = FirebaseFirestore.getInstance()
            db.collection(resources.getString(R.string.db_product))
                .document(pid)
                .update("status", false)
                .addOnSuccessListener {
                    adapterSales.removeItem(selectPosition)
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        } else {
            Log.i(TAG, "선택이 잘못되었습니다.....")
        }
    }

    // 상품 재정의
    private fun refreshItem() {
        if (selectPosition != -1) {
            Log.i(TAG, "일시정지에서 돌아옴 : 상품 재정의")
            if (adapterSales.itemCount > selectPosition) {
                val pid = adapterSales.getItem(selectPosition).id
                adapterSales.removeItem(selectPosition)
                checkGetItem(selectPosition, pid)
                selectPosition = -1 // 초기화
            } else {
                Log.i(TAG, "해당되는 상품이 없음!")
            }
        } else {
            Log.i(TAG, "일시정지에서 돌아옴 : 재정의 실패!!")
        }
    }

    // 상품 존재 확인
    private fun checkGetItem(position: Int, pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_user))
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(UserEntity::class.java)!!
                if (user.salesArray.contains(pid)) {
                    Log.i(TAG, "판매내역에 상품 있음!!")
                    getItem(position, pid)
                } else {
                    Log.i(TAG, "판매내역에 상품 없음!!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 로드
    private fun getItem(position: Int, pid: String) {
        Log.i(TAG, "PID : $pid")
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_product))
            .document(pid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val item = documentSnapshot.toObject(ProductEntity::class.java)!!
                    if (item.status && item.transactionStatus != 2) {
                        Log.i(TAG, "상품 추가!!")
                        adapterSales.addItem(position, documentSnapshot)
                    } else {
                        Log.i(TAG, "상품 상태 또는 거래상태가 달라서 추가 안함!!")
                    }
                } else {
                    Log.i(TAG, "No such DocumentSnapshot!")
                }
            }
            .addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 삭제 확인 팝업창
    private fun showPopupForDelete() {
        MaterialAlertDialogBuilder(context)
            .setTitle("거래중인 게시글이 삭제되면 거래 상대방이 당황할 수 있어요. 게시글을 정말 삭제하시겠어요?")
            .setPositiveButton("확인") { _, _ -> FirebaseUtils.instance.deleteProduct((context as AppCompatActivity), adapterSales.getItem(selectPosition).id) }
            .setNegativeButton("취소", null)
            .show()
    }

    // 숨김 확인 팝업창
    private fun showPopupForHide() {
        MaterialAlertDialogBuilder(context)
            .setTitle("게시물이 목록에서 제거됩니다.")
            .setPositiveButton("숨기기") { _, _ -> hideItem() }
            .setNegativeButton("취소", null)
            .show()
    }

    // 끌어올리기 확인 팝업창
    private fun showPopupForPull() {
        val item = adapterSales.getItem(selectPosition).toObject(ProductEntity::class.java)
        val day = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        val mod = item?.modDate?.time ?: 0
        val diff = now - mod

        Log.i(TAG, "mod : $diff")

        if (diff > day) {
            val str = "상품 끌어올리기를 하시겠습니까?"
            MaterialAlertDialogBuilder(context)
                .setTitle(str)
                .setPositiveButton("끌어올리기") { _, _ -> pullUpItem() }
                .setNegativeButton("취소", null)
                .show()
        } else {
            val str = "${remainingTime(day - diff)} 후에 사용가능합니다."
            MaterialAlertDialogBuilder(context)
                .setTitle(str)
                .setPositiveButton("확인", null)
                .show()
        }
    }

    private fun remainingTime(date: Long) : String {
        Log.i(TAG, "남은 시간 : $date")
        val second = 1000L
        val minute = second * 60
        val hour = minute * 60
        val day = hour * 24

        return when {
            date < day -> {
                "${date/hour}시간"
            }
            date < hour -> {
                "${date/minute}분"
            }
            date < minute -> {
                "${date/second}초"
            }
            else -> {
                "??"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Selected Item : $selectPosition")
        refreshItem()

    }

}
