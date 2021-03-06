package com.u.marketapp.utils

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.u.marketapp.R
import com.u.marketapp.entity.CommentEntity
import com.u.marketapp.entity.ProductEntity
import com.u.marketapp.entity.UserEntity
import com.u.marketapp.vo.ChatRoomVO
import com.u.marketapp.vo.ChattingVO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await

@SuppressLint("Registered")
class FirebaseUtils : AppCompatActivity() {

    companion object {
        private val TAG = FirebaseUtils::class.java.simpleName
        val instance = FirebaseUtils()
    }

    private lateinit var activity: AppCompatActivity

    suspend fun getIsAllRemove(activity: AppCompatActivity) : Boolean {
        this.activity = activity
        CoroutineScope(Dispatchers.IO).async {
            val document = getOwner()
            if (document.exists()) {
                val user = document.toObject(UserEntity::class.java)!!
                if (user.salesArray.size > 0) {
                    for (sale in user.salesArray) {
                        val item = getProduct(sale)
                        if (item.exists()) {
                            val product = item.toObject(ProductEntity::class.java)!!
                            Log.i(TAG, "전체 데이터 삭제 준비 완료")
                            removeImage(product.imageArray) // 이미지 제거
                            Log.i(TAG, "이미지 제거 완료")
                            removeCommentList(item) // 댓글 제거
                            Log.i(TAG, "댓글 제거 완료")
                            removeAttentionHistory(item) // 관심자 관심목록 제거
                            Log.i(TAG, "관심대상의 관심목록 제거 완료")
                            removeSellList(item) // 사용자 판매목록 제거
                            Log.i(TAG, "사용자의 판매목록 제거 완료")
                            removeBuyerHistory(item) // 구매자 구매목록 제거
                            Log.i(TAG, "구매자의 구매목록 제거 완료")
                            removeChatRoomList(item)  // 채팅방 제거
                            Log.i(TAG, "채팅방 제거 완료")
                            removeProduct(sale)
                            Log.i(TAG, "상품 제거 완료")
                        }
                    }
                } else {
                    Log.i(TAG, "판매 내역 정보 없음!")
                }
            } else {
                Log.i(TAG, "유저 문서 정보 없음!")
            }
        }.await()

        return true
    }

    // 유저 정보 획득
    suspend fun getOwner() = FirebaseFirestore.getInstance()
        .collection(activity.resources.getString(R.string.db_user))
        .document(FirebaseAuth.getInstance().currentUser!!.uid)
        .get()
        .await()

    // 상품 정보 획득
    suspend fun getProduct(pid: String) = FirebaseFirestore.getInstance()
        .collection(activity.resources.getString(R.string.db_product))
        .document(pid)
        .get()
        .await()

    // 상품 제거
    suspend fun removeProduct(pid: String) = FirebaseFirestore.getInstance()
        .collection(activity.resources.getString(R.string.db_product))
        .document(pid)
        .get()
        .addOnSuccessListener {
          if (it.exists()) {
              it.reference.delete()
                  .addOnSuccessListener {
                      Log.i(TAG, "상품 제거 완료!!")
                  }.addOnFailureListener { e ->
                      Log.i(TAG, e.toString())
                  }
          }
        }.addOnFailureListener { e ->
            Log.i(TAG, e.toString())
        }.await()

    // 댓글 목록 제거
    suspend fun removeCommentList(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.commentSize > 0) { // 댓글 목록 제거
            val items = documentSnapshot.reference.collection(activity.resources.getString(R.string.db_comment)).get().await()
            removeCommentList(items.documents)
        }
    }

    // 채팅방 제거
    suspend fun removeChatRoomList(documentSnapshot: DocumentSnapshot) {
        val db = FirebaseFirestore.getInstance()
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.chattingRoom.size > 0) {
            for (room in item.chattingRoom) {
                val roomDoc = db.collection(activity.resources.getString(R.string.db_chatting)).document(room).get().await()
                val chatRoom = roomDoc.toObject(ChatRoomVO::class.java)
                chatRoom?.apply {
                    buyer?.let { removeBuyerChatRoom(it, room) } // 구매 희망자의 채팅방 제거
                    seller?.let { removeSellerChatRoom(it, room) } // 판매자의 채팅방 제거
                }

                val chatDoc = roomDoc.reference.collection(activity.resources.getString(R.string.db_chatting_comment)).get().await()
                // 채팅 내역 제거
                for (document in chatDoc.documents) {
                    val url = document.toObject(ChattingVO::class.java)!!.imageMsg
                    url?.let { if (it.isNotEmpty()) deleteChatImage(it) } // 채팅에 포함된 이미지 (저장소) 삭제
                    document.reference.get()
                        .addOnSuccessListener {
                            if (it.exists()) {
                                it.reference.delete()
                                    .addOnSuccessListener {
                                        Log.i(TAG, "채팅 내역 제거 완료!!")
                                    }.addOnFailureListener { e ->
                                        Log.i(TAG, e.toString())
                                    }
                            }
                        }.addOnFailureListener { e ->
                            Log.i(TAG, e.toString())
                        }.await()
                }
                // 실제 채팅방 제거
                roomDoc.reference.get()
                    .addOnSuccessListener {
                        if (it.exists()) {
                            it.reference.delete()
                                .addOnSuccessListener {
                                    Log.i(TAG, "채팅방 제거 완료!!")
                                }.addOnFailureListener { e ->
                                    Log.i(TAG, e.toString())
                                }
                        }
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }.await()
            }
        }
    }

    // 관심목록 제거
    suspend fun removeAttentionHistory(documentSnapshot: DocumentSnapshot) {
        val db = FirebaseFirestore.getInstance()
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.attention.size > 0) {
            for (uid in item.attention) {
                db.collection(activity.resources.getString(R.string.db_user))
                    .document(uid)
                    .get()
                    .addOnSuccessListener {
                        if (it.exists()) {
                            it.reference.update("attentionArray", FieldValue.arrayRemove(documentSnapshot.id))
                                .addOnSuccessListener {
                                    Log.i(TAG, "관심자 관심목록 제거 완료!!")
                                }.addOnFailureListener { e ->
                                    Log.i(TAG, e.toString())
                                }
                        } else {
                            Log.i(TAG, "유저 문서 정보 없음!")
                        }
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }.await()
            }
        }
    }

    // 판매자의 판매내역 제거
    suspend fun removeSellList(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(item.seller)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    it.reference.update("salesArray", FieldValue.arrayRemove(documentSnapshot.id))
                        .addOnSuccessListener {
                            Log.i(TAG, "판매자 판매내역 제거 완료!!")
                        }.addOnFailureListener { e ->
                            Log.i(TAG, e.toString())
                        }
                } else {
                    Log.i(TAG, "유저 문서 정보 없음!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }.await()
    }

    // 구매자의 구매목록 제거
    suspend fun removeBuyerHistory(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.buyer.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection(activity.resources.getString(R.string.db_user))
                .document(item.buyer)
                .get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        it.reference.update("purchaseArray", FieldValue.arrayRemove(documentSnapshot.id))
                            .addOnSuccessListener {
                                Log.i(TAG, "구매자 구매목록 제거 완료!!")
                            }.addOnFailureListener { e ->
                                Log.i(TAG, e.toString())
                            }
                    } else {
                        Log.i(TAG, "유저 문서 정보 없음!")
                    }
                }.addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }.await()
        }
    }

    // 구매자들 채팅방 제거
    suspend fun removeBuyerChatRoom(buyer: String, room: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(buyer)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    it.reference.update("chatting", FieldValue.arrayRemove(room))
                        .addOnSuccessListener {
                            Log.i(TAG, "구매자 채팅방 제거 완료!!")
                        }.addOnFailureListener { e ->
                            Log.i(TAG, e.toString())
                        }
                } else {
                    Log.i(TAG, "구매자 문서 정보 없음!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }.await()
    }

    // 판매자 채팅방 제거
    suspend fun removeSellerChatRoom(seller: String, room: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(seller)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    it.reference.update("chatting", FieldValue.arrayRemove(room))
                        .addOnSuccessListener {
                            Log.i(TAG, "판매자 채팅방 제거 완료!!")
                        }.addOnFailureListener { e ->
                            Log.i(TAG, e.toString())
                        }
                } else {
                    Log.i(TAG, "판매자 문서 정보 없음!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }.await()
    }

    // 이미지 삭제
    suspend fun removeImage(imageArray: List<String>) {
        val storage = FirebaseStorage.getInstance()
        if (imageArray.isNotEmpty()) {
            for (url in imageArray) {
                try {
                    storage.getReferenceFromUrl(url)
                        .delete()
                        .addOnSuccessListener {
                            Log.i(TAG, "이미지 제거 완료!!")
                        }.addOnFailureListener { e ->
                            Log.i(TAG, "이미지 파일이 존재하지 않음!")
                            Log.i(TAG, e.toString())
                        }.await()
                } catch (e: Exception) {
                    Log.i(TAG, e.toString())
                }
            }
        }
    }

    // 전체 상품 제거
    fun allDeleteProduct(activity: AppCompatActivity) {
        this.activity = activity

        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(UserEntity::class.java)!!
                    if (user.salesArray.size > 0) {
                        for (sale in user.salesArray) {
                            deleteProduct(sale)
                        }
                    }
                } else {
                    Log.i(TAG, "유저 정보가 없음!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 제거
    fun deleteProduct(activity: AppCompatActivity, pid: String) {
        this.activity = activity

        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_product)).document(pid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val item = documentSnapshot.toObject(ProductEntity::class.java)!!
                    deleteImage(item.imageArray) // 이미지 제거
                    deleteCommentList(documentSnapshot) // 댓글 제거
                    deleteAttentionHistory(documentSnapshot) // 관심자 관심목록 제거
                    deleteSellList(documentSnapshot) // 사용자 판매목록 제거
                    deleteBuyerHistory(documentSnapshot) // 구매자 구매목록 제거
                    deleteChatRoomList(documentSnapshot) // 채팅방 제거

                    // 실제 상품 제거
                    documentSnapshot.reference
                        .delete()
                        .addOnSuccessListener {
                            Log.i(TAG, "상품 삭제 성공!")
                        }.addOnFailureListener { e ->
                            Log.i(TAG, e.toString())
                        }
                } else {
                    Log.i(TAG, "상품 문저 정보 없음!")
                }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 상품 제거
    fun deleteProduct(pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_product)).document(pid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val item = documentSnapshot.toObject(ProductEntity::class.java)!!
                deleteImage(item.imageArray) // 이미지 제거
                deleteCommentList(documentSnapshot) // 댓글 제거
                deleteAttentionHistory(documentSnapshot) // 관심자 관심목록 제거
                deleteSellList(documentSnapshot) // 사용자 판매목록 제거
                deleteBuyerHistory(documentSnapshot) // 구매자 구매목록 제거
                deleteChatRoomList(documentSnapshot) // 채팅방 제거

                // 실제 상품 제거
                documentSnapshot.reference
                    .delete()
                    .addOnSuccessListener {
                        Log.i(TAG, "상품 삭제 성공!")
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 댓글 목록 제거
    private fun deleteCommentList(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.commentSize > 0) { // 댓글 목록 제거
            documentSnapshot.reference.collection(activity.resources.getString(R.string.db_comment))
                .get()
                .addOnSuccessListener { task ->
                    deleteCommentList(task.documents)
                }.addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        }
    }

    // 댓글목록 제거
    suspend fun removeCommentList(list: List<DocumentSnapshot>) {
        for (document in list) {
            if (document.exists()) {
                val item = document.toObject(CommentEntity::class.java)!!
                if (item.replySize > 0) {
                    val replyQuery = getReplyQuery(document.reference)
                    // 답글 삭제
                    removeReplyList(replyQuery.documents)
                }
                // 댓글 삭제
                document.reference
                    .delete()
                    .addOnSuccessListener {
                        Log.i(TAG, "댓글 목록 삭제 성공!")
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                        Log.i(TAG, "댓글 목록 삭제 실패!")
                    }.await()
            } else {
                Log.i(TAG, "댓글 문서 정보 없음")
            }
        }
    }

    // 답글목록 제거
    suspend fun removeReplyList(list: List<DocumentSnapshot>) {
        for (document in list) {
            // 답글 삭제
            if (document.exists()) {
                document.reference.delete()
                    .addOnSuccessListener {
                        Log.i(TAG, "답글 목록 삭제 성공!!")
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }.await()
            } else {
                Log.i(TAG, "답글 문서 정보 없음")
            }
        }
    }

    // 판매자 채팅방 제거
    suspend fun getReplyQuery(reference: DocumentReference) = reference
        .collection(activity.resources.getString(R.string.db_reply))
        .get()
        .await()

    // 구매자의 구매목록 제거
    private fun deleteBuyerHistory(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.buyer.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection(activity.resources.getString(R.string.db_user))
                .document(item.buyer)
                .update("purchaseArray", FieldValue.arrayRemove(documentSnapshot.id))
                .addOnSuccessListener {
                    Log.i(TAG, "구매자 구매목록 제거")
                }.addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                }
        }
    }

    // 관심목록 제거
    private fun deleteAttentionHistory(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.attention.size > 0) {
            for (uid in item.attention) {
                deleteAttention(uid, documentSnapshot.id)
            }
        }
    }

    // 관심자의 관심목록 제거
    private fun deleteAttention(uid: String, pid: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(uid)
            .update("attentionArray", FieldValue.arrayRemove(pid))
            .addOnSuccessListener {
                Log.i(TAG, "관심자의 관심목록 제거")
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 채팅방 제거
    private fun deleteChatRoomList(documentSnapshot: DocumentSnapshot) {
        val db = FirebaseFirestore.getInstance()
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        if (item.chattingRoom.size > 0) {
            for (room in item.chattingRoom) {
                db.collection(activity.resources.getString(R.string.db_chatting))
                    .document(room)
                    .get()
                    .addOnSuccessListener { documentSnapshot2 ->
                        val chatRoom = documentSnapshot2.toObject(ChatRoomVO::class.java)
                        chatRoom?.apply {
                            buyer?.let { deleteBuyerChatRoom(it, room) } // 구매 희망자의 채팅방 제거
                            seller?.let { deleteSellerChatRoom(it, room) } // 판매자의 채팅방 제거
                        }

                        documentSnapshot2.reference
                            .collection(activity.resources.getString(R.string.db_chatting_comment))
                            .get()
                            .addOnSuccessListener { documentSnapshot3 ->
                                // 채팅 내역 제거
                                for (document in documentSnapshot3.documents) {
                                    val url = document.toObject(ChattingVO::class.java)!!.imageMsg
                                    url?.let { if (it.isNotEmpty()) deleteChatImage(it) } // 채팅에 포함된 이미지 (저장소) 삭제
                                    document.reference
                                        .delete()
                                        .addOnSuccessListener {
                                            Log.i(TAG, "채팅 내역 삭제!")
                                        }.addOnFailureListener { e ->
                                            Log.i(TAG, e.toString())
                                        }
                                }

                                // 실제 채팅방 제거
                                documentSnapshot2.reference
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.i(TAG, "채팅방 삭제 성공!")
                                    }.addOnFailureListener { e ->
                                        Log.i(TAG, e.toString())
                                    }
                            }.addOnFailureListener { e ->
                                Log.i(TAG, e.toString())
                            }
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }
            }
        }
    }

    // 판매자 채팅방 제거
    private fun deleteSellerChatRoom(seller: String, room: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(seller)
            .update("chatting", FieldValue.arrayRemove(room))
            .addOnSuccessListener {
                Log.i(TAG, "판매자 채팅방 제거")
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 구매자들 채팅방 제거
    private fun deleteBuyerChatRoom(buyer: String, room: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(buyer)
            .update("chatting", FieldValue.arrayRemove(room))
            .addOnSuccessListener {
                Log.i(TAG, "구매자 채팅방 제거")
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 댓글목록 제거
    private fun deleteCommentList(list: List<DocumentSnapshot>) {
        for (document in list) {
            val item = document.toObject(CommentEntity::class.java)!!
            if (item.replySize > 0) {
                document.reference.collection(activity.resources.getString(R.string.db_reply))
                    .get()
                    .addOnSuccessListener {
                        // 답글 삭제
                        deleteReplyList(it.documents)
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }
            }
            // 댓글 삭제
            document.reference
                .delete()
                .addOnSuccessListener {
                    Log.i(TAG, "댓글 목록 삭제 성공!")
                }.addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                    Log.i(TAG, "댓글 목록 삭제 실패!")
                }
        }
    }

    // 답글목록 제거
    private fun deleteReplyList(list: List<DocumentSnapshot>) {
        for (document in list) {
            // 답글 삭제
            document.reference.delete()
                .addOnSuccessListener {
                    Log.i(TAG, "답글 목록 삭제 성공!")
                }.addOnFailureListener { e ->
                    Log.i(TAG, e.toString())
                    Log.i(TAG, "답글 목록 삭제 실패!")
                }
        }
    }

    // 판매자의 판매내역 제거
    private fun deleteSellList(documentSnapshot: DocumentSnapshot) {
        val item = documentSnapshot.toObject(ProductEntity::class.java)!!
        val db = FirebaseFirestore.getInstance()
        db.collection(activity.resources.getString(R.string.db_user))
            .document(item.seller)
            .update("salesArray", FieldValue.arrayRemove(documentSnapshot.id))
            .addOnSuccessListener {
                Log.i(TAG, "판매내역 제거 성공! : ${documentSnapshot.id}")
            }.addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

    // 이미지 삭제
    private fun deleteImage(imageArray: List<String>) {
        val storage = FirebaseStorage.getInstance()
        if (imageArray.isNotEmpty()) {
            for (url in imageArray) {
                storage.getReferenceFromUrl(url)
                    .delete()
                    .addOnSuccessListener {
                        Log.i(TAG, "이미지 삭제 성공 (저장소) : $url")
                    }.addOnFailureListener { e ->
                        Log.i(TAG, e.toString())
                    }
            }
        }
    }

    // 이미지 삭제
    private fun deleteChatImage(url: String) {
        val storage = FirebaseStorage.getInstance()
        storage.getReferenceFromUrl(url)
            .delete()
            .addOnSuccessListener {
                Log.i(TAG, "이미지 삭제 성공 (저장소) : $url")
            }
            .addOnFailureListener { e ->
                Log.i(TAG, e.toString())
            }
    }

}