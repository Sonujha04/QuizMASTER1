package com.example.quizmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val startQuizButton = findViewById<android.view.View>(R.id.btnStartQuiz)
        val categoriesButton = findViewById<android.view.View>(R.id.btnCategories)
        val historyButton = findViewById<android.view.View>(R.id.btnHistory)
        val groupQuizButton = findViewById<android.view.View>(R.id.btnGroupQuiz)
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)

        startQuizButton.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            startActivity(intent)
        }

        categoriesButton.setOnClickListener {
            val intent = Intent(this, CategoryActivity::class.java)
            startActivity(intent)
        }

        historyButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        groupQuizButton.setOnClickListener {
            startActivity(Intent(this, LobbyActivity::class.java))
        }

        // Category Icon Clicks
        findViewById<android.view.View>(R.id.layoutProg).setOnClickListener {
            startQuizWithCategory("Python") // Assuming Prog refers to Python for now
        }
        findViewById<android.view.View>(R.id.layoutDbms).setOnClickListener {
            startQuizWithCategory("DBMS")
        }
        findViewById<android.view.View>(R.id.layoutAndroid).setOnClickListener {
            startQuizWithCategory("Android")
        }
        findViewById<android.view.View>(R.id.layoutGk).setOnClickListener {
            startQuizWithCategory("General")
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_leaderboard -> {
                    startActivity(Intent(this, LeaderboardActivity::class.java))
                    true
                }
                R.id.nav_upload -> {
                    startActivity(Intent(this, UploadActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        findViewById<android.view.View>(R.id.btnAiShortcut).setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }

        updateUI(auth)
    }

    private fun updateUI(auth: FirebaseAuth) {
        val user = auth.currentUser
        val tvTitle = findViewById<android.widget.TextView>(R.id.tvTitle)
        
        if (user != null && tvTitle != null) {
            // Function to parse name
            fun parseFirstName(rawName: String?): String {
                if (rawName.isNullOrBlank()) return "User"
                
                // 1. Initial split by symbols, numbers, or CamelCase (e.g. "Sonu.Jha" or "Sonu123")
                val parts = rawName.split(Regex("(?<=[a-z])(?=[A-Z])|[\\s._0-9]+"))
                var nameCandidate = parts[0]

                // 2. Aggressive split by common merged surnames (e.g. "Sonujhadbj" -> "Sonu")
                val commonSurnames = listOf("jha", "kumar", "singh", "sharma", "gupta", "verma", "yadav", "doe", "smith")
                for (surname in commonSurnames) {
                    val index = nameCandidate.lowercase().indexOf(surname)
                    if (index > 0) { // If surname is found and not at the start
                        nameCandidate = nameCandidate.substring(0, index)
                        break
                    }
                }
                
                return nameCandidate.replaceFirstChar { it.uppercase() }
            }

            // Show name immediately
            val initialName = parseFirstName(user.displayName ?: user.email?.split("@")?.get(0))
            tvTitle.text = "Hello, $initialName!"

            // Then refresh from server to fetch the "Full Name" registered during sign-up
            user.reload().addOnCompleteListener {
                val freshName = parseFirstName(user.displayName ?: user.email?.split("@")?.get(0))
                tvTitle.text = "Hello, $freshName!"
            }
        }

        val historyManager = HistoryManager(this)
        val history = historyManager.getHistory()

        // Update Stats Cards
        val tvTotalQuizzes = findViewById<android.widget.TextView>(R.id.tvTotalQuizzes)
        val tvAvgScore = findViewById<android.widget.TextView>(R.id.tvAvgScore)
        
        tvTotalQuizzes?.text = history.size.toString()
        val avg = if (history.isNotEmpty()) {
            history.map { (it.score.toDouble() / it.total) * 100 }.average().toInt()
        } else 0
        tvAvgScore?.text = "$avg%"

        // Update Recent List
        val container = findViewById<android.widget.LinearLayout>(R.id.layoutRecentQuizzes)
        val tvEmpty = findViewById<android.view.View>(R.id.tvNoRecent)
        
        if (container == null) return

        container.removeAllViews()

        if (history.isEmpty()) {
            tvEmpty?.visibility = android.view.View.VISIBLE
        } else {
            tvEmpty?.visibility = android.view.View.GONE
            val recentItems = history.take(2) // Show last 2
            for (item in recentItems) {
                val card = layoutInflater.inflate(R.layout.item_history, container, false)
                
                card.findViewById<android.widget.TextView>(R.id.tvCategory)?.text = item.category
                card.findViewById<android.widget.TextView>(R.id.tvScore)?.text = "Score: ${item.score}/${item.total}"
                card.findViewById<android.widget.TextView>(R.id.tvDate)?.text = 
                    android.text.format.DateFormat.format("dd MMM yyyy", item.date)
                
                val progress = (item.score.toDouble() / item.total * 100).toInt()
                card.findViewById<android.widget.ProgressBar>(R.id.scoreProgress)?.progress = progress
                card.findViewById<android.widget.TextView>(R.id.tvPercent)?.text = "$progress%"
                
                container.addView(card)
            }
        }
    }

    private fun startQuizWithCategory(category: String) {
        val intent = Intent(this, QuizActivity::class.java)
        intent.putExtra("CATEGORY", category)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateUI(FirebaseAuth.getInstance())
    }
}
