package jp.techacademy.takumi.tomizawa.qa_app

import java.io.Serializable
import java.util.ArrayList

class Question(
    val title: String,
    val body: String,
    val name: String,
    val uid: String,
    val questionUid: String,
    val genre: Int,
    bytes: ByteArray,
    val answers: ArrayList<Answer>,
    var isFavorite: Boolean
) : Serializable {
    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}