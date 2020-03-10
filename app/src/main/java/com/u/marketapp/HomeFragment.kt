package com.u.marketapp

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.u.marketapp.adapter.ProductRVAdapter
import com.u.marketapp.databinding.FragmentHomeBinding
import com.u.marketapp.entity.ProductEntity
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }

    private lateinit var adapter : ProductRVAdapter
    private lateinit var binding : FragmentHomeBinding

    override fun onRefresh() {
        adapter.clear()
        setItemsData()
        binding.swipRefreshLayout.isRefreshing = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // 데이터 바인딩
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        setRVLayoutManager()
        setRVAdapter()
        setItemsData()
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.swipRefreshLayout.setOnRefreshListener(this)
    }

    // 리사이클뷰 레이아웃 매니저 설정
    private fun setRVLayoutManager() {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
    }

    // 리사이클뷰 어댑터 설정
    private fun setRVAdapter() {
        adapter = ProductRVAdapter()
        binding.recyclerView.adapter = adapter
    }

    // 데이터 설정
    private fun setItemsData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Product").whereEqualTo("status", "active").orderBy("regDate", Query.Direction.DESCENDING).limit(30).get().addOnCompleteListener {
            if (it.isSuccessful) {
                for(document in it.result?.documents!!) {
                    val item = document.toObject(ProductEntity::class.java)
                    Log.i(TAG, item.toString())
                    item?.let { a -> adapter.addItem(a) }
                }
            }
        }
    }

}
