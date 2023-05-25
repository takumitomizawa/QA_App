package jp.techacademy.takumi.tomizawa.qa_app


import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.takumi.tomizawa.qa_app.databinding.ActivityFavoriteBinding

class FavoriteActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
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
            val questionId = dataSnapshot.key ?: ""

            val genre = map["genre"].toString()

            val questionRef = databaseReference.child(ContentsPATH).child(genre).child(questionId)

            questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = snapshot.value as Map<*, *>?
                    val title = map?.get("title") as? String ?: ""
                    val body = map?.get("body") as? String ?: ""
                    val name = map?.get("name") as? String ?: ""
                    val uid = map?.get("uid") as? String ?: ""
                    val imageString = map?.get("image") as? String ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }

                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map?.get("answers") as Map<*, *>?
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
                        title, body, name, uid, dataSnapshot.key ?: "",
                        genre.toInt(), bytes, answerArrayList
                    )
                    questionArrayList.add(question)
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(firebaseError: DatabaseError) {}
            })
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
    // ----- 追加:ここまで -----

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        // ログインしていなければログイン画面に遷移させる
        if (user == null) {
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
        }

        // ----- 追加:ここから -----
        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = QuestionsListAdapter(this)
        questionArrayList = ArrayList()
        binding.favoriteListView.adapter = adapter
        adapter.setQuestionArrayList(questionArrayList)
        adapter.notifyDataSetChanged()
        // ----- 追加:ここまで -----

        binding.favoriteListView.setOnItemClickListener { _, _, position, _ ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", questionArrayList[position])
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        genreRef = databaseReference.child("favorites").child(user!!.uid)
        questionArrayList.clear()
        genreRef!!.addChildEventListener(eventListener)



    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        /*when (item.itemId) {
            R.id.nav_hobby -> {
                binding.content.toolbar.title = getString(R.string.menu_hobby_label)
                genre = 1
            }
            R.id.nav_life -> {
                binding.content.toolbar.title = getString(R.string.menu_life_label)
                genre = 2
            }
            R.id.nav_health -> {
                binding.content.toolbar.title = getString(R.string.menu_health_label)
                genre = 3
            }
            R.id.nav_computer -> {
                binding.content.toolbar.title = getString(R.string.menu_computer_label)
                genre = 4
            }
            R.id.nav_favorite -> {
                val intent = Intent(this, FavoriteActivity::class.java)
                startActivity(intent)
            }
        }*/

        //binding.drawerLayout.closeDrawer(GravityCompat.START)

        // ----- 追加:ここから -----
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        questionArrayList.clear()
        adapter.setQuestionArrayList(questionArrayList)
        binding.favoriteListView.adapter = adapter

        // 選択したジャンルにリスナーを登録する
        if (genreRef != null) {
            genreRef!!.removeEventListener(eventListener)
        }
        genreRef = databaseReference.child("favorites").child(genre.toString())
        genreRef!!.addChildEventListener(eventListener)
        // ----- 追加:ここまで -----

        return true
    }
}