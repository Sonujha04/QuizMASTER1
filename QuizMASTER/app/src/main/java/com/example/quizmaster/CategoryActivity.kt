package com.example.quizmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CategoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_category)

        val btnPython = findViewById<Button>(R.id.btnPython)
        val btnJava = findViewById<Button>(R.id.btnJava)
        val btnAndroid = findViewById<Button>(R.id.btnAndroid)
        val btnSQL = findViewById<Button>(R.id.btnSQL)
        val btnGeneral = findViewById<Button>(R.id.btnGeneral)

        btnPython.setOnClickListener {
            startQuiz("Python")
        }

        btnJava.setOnClickListener {
            startQuiz("Java")
        }

        btnAndroid.setOnClickListener {
            startQuiz("Android")
        }

        btnSQL.setOnClickListener {
            startQuiz("SQL")
        }

        btnGeneral.setOnClickListener {
            startQuiz("General")
        }
    }

    private fun startQuiz(category: String) {

        val intent = Intent(this, QuizActivity::class.java)

        intent.putExtra("CATEGORY", category)

        startActivity(intent)
    }
}