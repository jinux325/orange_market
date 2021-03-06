package com.u.marketapp.history

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.u.marketapp.activity.ProductActivity
import com.u.marketapp.adapter.CompleteHistoryRVAdapter
import com.u.marketapp.entity.ProductEntity
import com.u.marketapp.entity.UserEntity
import com.u.marketapp.utils.FirebaseUtils
import kotlinx.android.synthetic.main.fragment_history.view.*

class SalesFragment2 : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    companion object {
        private val TAG = SalesFragment2::class.java.simpleName
        private const val REQUEST_PRODUCT = 100
    }

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var textView: TextView
    private lateinit var adapterSales: CompleteHistoryRVAdapter
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
            adapterSales = CompleteHistoryRVAdapter()
            recyclerView.adapter = adapterSales
            adapterSales.setItemClickListener(object : CompleteHistoryRVAdapter.ItemClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "Item Click : $selectPosition")
                    moveProductActivity(adapterSales.getItem(position).id)
                }
            })
            adapterSales.setMoreClickListener(object : CompleteHistoryRVAdapter.MoreClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "More Click : $selectPosition")
                    activity?.apply {
                        openContextMenu(view)
                    }
                }
            })
            adapterSales.setReviewClickListener(object : CompleteHistoryRVAdapter.ReviewClickListener {
                override fun onClick(view: View, position: Int) {
                    selectPosition = position
                    Log.i(TAG, "Review Click : $selectPosition")
                    Toast.makeText(context, resources.getString(R.string.feature_to_be_added), Toast.LENGTH_SHORT).show()
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
                if (documentSnapshot.exists()) {
                    if (getActiveProductItem(documentSnapshot) && getStateProductItem(documentSnapshot) == 2) {
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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        activity?.apply {
            menuInflater.apply {
                inflate(R.menu.context_sales_complete, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_sales -> { // '판매중'으로 변경
                Log.i(TAG, "action_change_sales : $selectPosition")
                tradeChangeItem1()
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
            val db = FirebaseFirestore.getInstance()
            db.collection(resources.getString(R.string.db_product))
                .document(pid)
                .update("transactionStatus", 0)
                .addOnSuccessListener {
                    removePurchaseProductItem(pid)
                    adapterSales.removeItem(selectPosition)
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        } else {
            Log.i(TAG, "선택이 잘못되었습니다.....")
        }
    }

    // 구매자의 구매목록에서 상품 제거
    private fun removePurchaseProductItem(pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_product))
            .document(pid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val item = documentSnapshot.toObject(ProductEntity::class.java)!!
                    if (item.buyer.isNotEmpty()) {
                        db.collection(resources.getString(R.string.db_user))
                            .document(item.buyer)
                            .update("purchaseArray", FieldValue.arrayRemove(pid))
                            .addOnSuccessListener {
                                Log.i(TAG, "유저의 구매목록에서 상품 제거")
                                deleteBuyerProductItem(pid)
                            }.addOnFailureListener { e ->
                                Log.i(TAG, e.toString())
                            }
                    }
                } else {
                    Log.i(TAG, "No such DocumentSnapshot!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품의 구매자 제거
    private fun deleteBuyerProductItem(pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_product))
            .document(pid)
            .update("buyer", "")
            .addOnSuccessListener {
                Log.i(TAG, "상품에서 구매자 제거!")
            }
            .addOnFailureListener { e ->
                Log.i(TAG, e.toString())
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
                    getItem(position, pid)
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 로드
    private fun getItem(position: Int, pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(resources.getString(R.string.db_product))
            .document(pid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val item = documentSnapshot.toObject(ProductEntity::class.java)!!
                    if (item.status && item.transactionStatus == 2) {
                        adapterSales.addItem(position, documentSnapshot)
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

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "Selected Item : $selectPosition")
        refreshItem()
    }

}
