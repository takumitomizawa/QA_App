package jp.techacademy.takumi.tomizawa.qa_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.ListView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.protobuf.Value
import jp.techacademy.takumi.tomizawa.qa_app.databinding.ActivityFavoriteBinding

class FavoriteActivity : AppCompatActivity() {
    private lateinit var favoriteListView: ListView
    private lateinit var adapter: QuestionDetailListAdapter
    private lateinit var binding: ActivityFavoriteBinding

    private var genre = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = QuestionDetailListAdapter(this, getFavoriteQuestion())
        binding.favoriteListView.adapter = adapter
    }

    private fun getFavoriteQuestion(): Question {
        //お気に入りの質問を取得する処理を実装する
        val user = FirebaseAuth.getInstance().currentUser
        val databaseRef: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("favorites").child(user!!.uid)

        val question: Question? = null

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val questionIdList: ArrayList<Question>
                    for (snapshot in dataSnapshot.children) {
                        val questionId = snapshot.key
                        questionId?.let { questionIdList.add(it) }
                    }

                    for (questionId in questionIdList) {
                        val questionRef: DatabaseReference =
                            FirebaseDatabase.getInstance().reference.child("questions")
                                .child(questionId)
                        questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val map = dataSnapshot.value as Map<*, *>
                                val title = map["title"] as? String ?: ""
                                val body = map["body"] as? String ?: ""
                                val name = map["name"] as? String ?: ""
                                val uid = map["uid"] as? String ?: ""
                                //val genre = map["genre"] as? Int ?: 0
                                val imageString = map["image"] as? String ?: ""
                                val bytes =
                                    if (imageString.isNotEmpty()) {
                                        Base64.decode(imageString, Base64.DEFAULT)
                                    } else {
                                        byteArrayOf()
                                    }

                                val answerArrayList = ArrayList<Answer>()
                                val answerMap = map["answers"] as Map<*, *>?
                                if (answerMap != null) {
                                    for (key in answerMap.keys) {
                                        val map1 = answerMap[key] as Map<*, *>
                                        val map1Body = map1["body"] as? String ?: ""
                                        val map1Name = map1["name"] as? String ?: ""
                                        val map1Uid = map1["uid"] as? String ?: ""
                                        val map1AnswerUid = key as? String ?: ""
                                        val answer =
                                            Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                                        answerArrayList.add(answer)
                                    }
                                }

                                val question = Question(
                                    title, body, name, uid,dataSnapshot.key ?: "",
                                    genre, bytes, answerArrayList, true
                                )
                                questionIdList.add(question)
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                TODO("Not yet implemented")
                            }
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
        return question!!
    }
}