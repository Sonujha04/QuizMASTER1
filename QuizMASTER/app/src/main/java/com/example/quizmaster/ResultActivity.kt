package com.example.quizmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ProgressBar
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)

        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val tvFeedback = findViewById<TextView>(R.id.tvFeedback)
        val tvPercentage = findViewById<TextView>(R.id.tvPercentage)
        val tvFirstTry = findViewById<TextView>(R.id.tvFirstTry) // Repurposed for Correct Count
        val tvAfterSkipped = findViewById<TextView>(R.id.tvAfterSkipped) // Repurposed for Wrong Count
        val tvUnattempted = findViewById<TextView>(R.id.tvUnattempted) // New ID
        
        val resultProgressBar = findViewById<ProgressBar>(R.id.resultProgressBar)
        val btnReview = findViewById<Button>(R.id.btnReview)
        val btnTryAgain = findViewById<Button>(R.id.btnTryAgain)
        val btnHome = findViewById<Button>(R.id.btnHome)

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 10)
        val category = intent.getStringExtra("CATEGORY") ?: "General"
        val questions = intent.getSerializableExtra("QUESTIONS") as? ArrayList<Question> ?: arrayListOf()
        val userAnswers = intent.getIntArrayExtra("USER_ANSWERS") ?: IntArray(0)

        val isGroup = intent.getBooleanExtra("IS_GROUP", false)
        val roomId = intent.getStringExtra("ROOM_ID") ?: ""
        if (isGroup && roomId.isNotEmpty()) {
            RoomManager.updatePlayerProgress(roomId, total, score)
            RoomManager.updatePlayerStatus(roomId, "FINISHED")
        }

        val unattempted = userAnswers.count { it == 0 }
        val wrong = total - score - unattempted

        saveResult(score, total, category)

        val percentage = (score * 100) / total

        tvScore.text = "$score/$total"
        tvPercentage.text = "$percentage%"
        tvMessage.text = when {
            percentage >= 80 -> "Great Job!"
            percentage >= 60 -> "Well Done!"
            else -> "Keep Learning!"
        }
        tvFeedback.text = when {
            percentage >= 80 -> "Excellent! You are doing amazing. Keep it up!"
            percentage >= 60 -> "Good effort! A little more practice and you'll ace it."
            else -> "Don't give up! Review your answers and try again."
        }

        tvFirstTry.text = score.toString()
        tvAfterSkipped.text = wrong.toString()
        tvUnattempted.text = unattempted.toString()

        resultProgressBar.progress = percentage

        playCelebrationSound(percentage)

        btnReview.setOnClickListener {
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("QUESTIONS", questions)
            intent.putExtra("USER_ANSWERS", userAnswers)
            startActivity(intent)
        }

        btnTryAgain.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("CATEGORY", category)
            // AI-generated quizzes can't be rebuilt from the category, so replay the same questions
            if (category == "Document Quiz") {
                intent.putExtra("CUSTOM_QUESTIONS", questions)
            }
            startActivity(intent)
            finish()
        }

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun playCelebrationSound(percentage: Int) {
        if (percentage >= 60) {
            val resId = resources.getIdentifier("celebration", "raw", packageName)
            if (resId != 0) {
                try {
                    val mediaPlayer = MediaPlayer.create(this, resId)
                    mediaPlayer.setOnCompletionListener { it.release() }
                    mediaPlayer.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun saveResult(score: Int, total: Int, category: String) {
        val historyManager = HistoryManager(this)
        val historyItem = HistoryItem(
            score = score,
            total = total,
            category = category,
            date = System.currentTimeMillis()
        )
        historyManager.saveHistoryItem(historyItem)
    }
}