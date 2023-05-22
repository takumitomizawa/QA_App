package jp.techacademy.takumi.tomizawa.qa_app

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.takumi.tomizawa.qa_app.databinding.ActivityQuestionDetailBinding

data class Favorite(
    val genre: Int = 0,
    val favorite: Boolean = false
)

class QuestionDetailActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityQuestionDetailBinding

    private lateinit var question: Question
    private lateinit var adapter: QuestionDetailListAdapter
    private lateinit var answerRef: DatabaseReference

    private var isFavorite: Boolean = false

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in question.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            question.answers.add(answer)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
        question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra("question", Question::class.java)!!
        else
            intent.getSerializableExtra("question") as? Question!!

        title = question.title

        // ListViewの準備
        adapter = QuestionDetailListAdapter(this, question)
        binding.listView.adapter = adapter
        adapter.notifyDataSetChanged()

        binding.fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", question)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        binding.fabFavorite.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                val favoriteRef = FirebaseDatabase.getInstance().reference
                    .child("favorites")
                    .child(user.uid)
                    .child(question.questionUid)
                    .child("genre")

                favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.value == null) {
                            // お気に入り登録されていない場合に登録処理をする
                            favoriteRef.setValue(question.genre)
                            binding.fabFavorite.setImageResource(R.drawable.ic_star)
                        } else {
                            // お気に入りに登録済みの場合はお気に入りから削除する
                            favoriteRef.removeValue()
                            binding.fabFavorite.setImageResource(R.drawable.ic_star_border)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // エラー処理を記述
                    }
                })
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
            .child(question.questionUid).child(AnswersPATH)
        answerRef.addChildEventListener(eventListener)
    }

    override fun onResume() {
        super.onResume()
        Log.d("test", "onResume通過")

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            Log.d("test", "if文通過")
            val favoriteRef = FirebaseDatabase.getInstance().reference
                .child("favorites")
                .child(user.uid)
                .child(question.questionUid)
                .child("genre")

            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //val isFavorite = snapshot.getValue(Boolean::class.java)

                    val favoriteData = snapshot.getValue(Favorite::class.java)
                    val isFavorite = favoriteData?.favorite ?: false
                    Log.d("test", "addListener通過")

                    if (isFavorite) {
                        Log.d("test", "お気に入り登録済み通過")
                        // お気に入り登録済みの場合
                        binding.fabFavorite.setImageResource(R.drawable.ic_star)
                    } else {
                        Log.d("test", "お気に入り未登録通過")
                        // お気に入り登録されていない場合
                        binding.fabFavorite.setImageResource(R.drawable.ic_star_border)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        } else {
            // ログインしていない場合
            binding.fabFavorite.setImageResource(R.drawable.ic_star_border)
        }
    }

    private fun updateFavoriteImage() {
        if (isFavorite) {
            binding.fabFavorite.setImageResource(R.drawable.ic_star)
        } else {
            binding.fabFavorite.setImageResource(R.drawable.ic_star_border)
        }
    }

    override fun onClick(v: View) {
        // キーボードが出てたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        val answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
            .child(question.questionUid).child(AnswersPATH)

        val data = HashMap<String, String>()

        // UID
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

        // 表示名
        // Preferenceから名前を取る
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        data["name"] = name!!

        /*// 回答を取得する
        val answer = binding.answerEditText.text.toString()

        if (answer.isEmpty()) {
            // 回答が入力されていない時はエラーを表示するだけ
            Snackbar.make(v, getString(R.string.answer_error_message), Snackbar.LENGTH_LONG).show()
            return
        }
        data["body"] = answer

        binding.progressBar.visibility = View.VISIBLE*/

        if (::answerRef.isInitialized) {
            answerRef.setValue(true)
        }
        //answerRef.push().setValue(data, this)
    }
}