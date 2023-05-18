package jp.techacademy.takumi.tomizawa.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import jp.techacademy.takumi.tomizawa.qa_app.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener  {
    private lateinit var binding: ActivityMainBinding

    private var genre = 0

    private lateinit var databaseReference: DatabaseReference
    private lateinit var questionArrayList: ArrayList<Question>
    private lateinit var adapter: QuestionsListAdapter

    private var snapshotListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.content.toolbar)

        binding.content.fab.setOnClickListener {
            // ジャンルを選択していない場合はメッセージを表示するだけ
            if (genre == 0) {
                Snackbar.make(
                    it,
                    getString(R.string.question_no_select_genre),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていなければログイン画面に遷移させる
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", genre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.content.toolbar,
            R.string.app_name,
            R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = QuestionsListAdapter(this)
        questionArrayList = ArrayList()
        adapter.notifyDataSetChanged()

        binding.content.inner.listView.setOnItemClickListener { _, _, position, _ ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", questionArrayList[position])
            startActivity(intent)
        }
    }


    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // 1:趣味を既定の選択とする
        if(genre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
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
        when (item.itemId) {
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
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        questionArrayList.clear()
        adapter.setQuestionArrayList(questionArrayList)
        binding.content.inner.listView.adapter = adapter

        // 一つ前のリスナーを消す
        snapshotListener?.remove()

        // 選択したジャンルにリスナーを登録する
        snapshotListener = FirebaseFirestore.getInstance()
            .collection(ContentsPATH)
            .whereEqualTo("genre", genre)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    // 取得エラー
                    return@addSnapshotListener
                }
                var questions = listOf<Question>()
                val results = querySnapshot?.toObjects(FireStoreQuestion::class.java)
                results?.also {
                    questions = it.map { firestoreQuestion ->
                        val bytes =
                            if (firestoreQuestion.image.isNotEmpty()) {
                                Base64.decode(firestoreQuestion.image, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }
                        Question(firestoreQuestion.title, firestoreQuestion.body, firestoreQuestion.name, firestoreQuestion.uid,
                            firestoreQuestion.id, firestoreQuestion.genre, bytes, firestoreQuestion.answers)
                    }
                }
                questionArrayList.clear()
                questionArrayList.addAll(questions)
                adapter.notifyDataSetChanged()
            }
        return true
    }
}
