package com.example.quizmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val tvName = findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvAccountEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Statistics Views
        val tvTotalQuizzes = findViewById<TextView>(R.id.tvTotalQuizzes)
        val tvAvgScore = findViewById<TextView>(R.id.tvAvgScore)
        val tvBestScore = findViewById<TextView>(R.id.tvBestScore)
        
        // Progress Views
        val pbAverageScore = findViewById<ProgressBar>(R.id.pbAverageScore)
        val tvProgressPercent = findViewById<TextView>(R.id.tvProgressPercent)
        val tvProgressStatus = findViewById<TextView>(R.id.tvProgressStatus)

        if (user != null) {
            // Use display name if available, otherwise derive from email
            tvName.text = user.displayName ?: user.email?.split("@")?.get(0) ?: "User"
            tvEmail.text = user.email
            tvAccountEmail.text = user.email
        }

        // Load and Calculate Stats from History
        val historyManager = HistoryManager(this)
        val history = historyManager.getHistory()
        
        val totalQuizzes = history.size
        val avgScorePercent = if (totalQuizzes > 0) {
            history.map { (it.score.toDouble() / it.total) * 100 }.average().toInt()
        } else 0
        val bestScore = if (totalQuizzes > 0) {
            history.maxOf { it.score }
        } else 0

        tvTotalQuizzes.text = totalQuizzes.toString()
        tvAvgScore.text = "$avgScorePercent%"
        tvBestScore.text = bestScore.toString()

        pbAverageScore.progress = avgScorePercent
        tvProgressPercent.text = "$avgScorePercent%"
        
        tvProgressStatus.text = when {
            avgScorePercent >= 80 -> "Excellent! You are a Quiz Master!"
            avgScorePercent >= 60 -> "Well done! You're performing great!"
            totalQuizzes > 0 -> "Keep practicing to master your skills!"
            else -> "Start your first quiz to see your progress!"
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
