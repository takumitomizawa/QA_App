package jp.techacademy.takumi.tomizawa.qa_app


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import com.google.firebase.database.*
import jp.techacademy.takumi.tomizawa.qa_app.databinding.ActivityFavoriteBinding

class FavoriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteBinding

    private var genre = 0

    // ----- 追加:ここから -----
    private lateinit var databaseReference: DatabaseReference
    private lateinit var questionArrayList: ArrayList<Question>
    private lateinit var adapter: QuestionsListAdapter

    private var genreRef: DatabaseReference? = null

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
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
                    val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(
                title, body, name, uid,dataSnapshot.key ?: "",
                genre, bytes, answerArrayList, true
            )
            questionArrayList.add(question)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            // 変更があったQuestionを探す
            for (question in questionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<*, *>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val map1 = answerMap[key] as Map<*, *>
                            val map1Body = map1["body"] as? String ?: ""
                            val map1Name = map1["name"] as? String ?: ""
                            val map1Uid = map1["uid"] as? String ?: ""
                            val map1AnswerUid = key as? String ?: ""
                            val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                            question.answers.add(answer)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {}
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
        override fun onCancelled(p0: DatabaseError) {}
    }

    /*private fun getQuestionsFromFirebase(){
        val user = FirebaseAuth.getInstance().currentUser
        val userRef = databaseReference.child(UsersPATH).child(user!!.uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val data = dataSnapshot.value as Map<*, *>? ?: return

                val favoriteQuestionIds = data["favorites"] as Map<*, *>?
                if (favoriteQuestionIds == null){
                    questionArrayList.clear()
                    adapter.notifyDataSetChanged()
                    return
                }

                val questionRef = databaseReference.child(ContentsPATH)
                questionRef.addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val questions = snapshot.children.mapNotNull { it.getValue(Question::class.java) }

                        val favoriteQuestions = questions.filter { favoriteQuestionIds.containsKey(it.questionUid) }
                        questionArrayList.clear()
                        questionArrayList.addAll(favoriteQuestions)
                        adapter.setQuestionArrayList(questionArrayList)
                        adapter.notifyDataSetChanged()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = QuestionsListAdapter(this)
        questionArrayList = ArrayList()
        adapter.notifyDataSetChanged()

        // ListViewの設定とデータ取得処理の呼び出し
        questionArrayList.clear()
        adapter.setQuestionArrayList(questionArrayList)
        binding.favoriteListView.adapter = adapter

        if (genreRef != null) {
            genreRef!!.removeEventListener(eventListener)
        }
        genreRef = databaseReference.child(ContentsPATH).child(genre.toString())
        genreRef!!.addChildEventListener(eventListener)
    }
}