package com.example.quizmaster

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ReviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val questions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("QUESTIONS", ArrayList::class.java) as? ArrayList<Question> ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("QUESTIONS") as? ArrayList<Question> ?: arrayListOf()
        }
        val userAnswers = intent.getIntArrayExtra("USER_ANSWERS") ?: IntArray(0)

        val rvReview = findViewById<RecyclerView>(R.id.rvReview)
        val btnClose = findViewById<Button>(R.id.btnClose)

        rvReview.layoutManager = LinearLayoutManager(this)
        rvReview.adapter = ReviewAdapter(questions, userAnswers)

        btnClose.setOnClickListener {
            finish()
        }
    }

    class ReviewAdapter(
        private val questions: List<Question>,
        private val userAnswers: IntArray
    ) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

        class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
            val tvOption1: TextView = view.findViewById(R.id.tvOption1)
            val tvOption2: TextView = view.findViewById(R.id.tvOption2)
            val tvOption3: TextView = view.findViewById(R.id.tvOption3)
            val tvOption4: TextView = view.findViewById(R.id.tvOption4)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val options = listOf(tvOption1, tvOption2, tvOption3, tvOption4)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
            return ReviewViewHolder(view)
        }

        override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
            val question = questions[position]
            val userAnswer = userAnswers[position]

            holder.tvQuestion.text = "${position + 1}. ${question.question}"
            holder.tvOption1.text = question.option1
            holder.tvOption2.text = question.option2
            holder.tvOption3.text = question.option3
            holder.tvOption4.text = question.option4

            // Reset backgrounds
            holder.options.forEach { it.setBackgroundResource(R.drawable.bg_option_review) }

            // Highlight correct answer
            if (question.correctAnswer in 1..4) {
                holder.options[question.correctAnswer - 1].setBackgroundResource(R.drawable.bg_option_correct)
            }

            // Highlight user answer if wrong
            if (userAnswer != question.correctAnswer && userAnswer in 1..4) {
                holder.options[userAnswer - 1].setBackgroundResource(R.drawable.bg_option_incorrect)
            }

            if (userAnswer == question.correctAnswer) {
                holder.tvStatus.text = "Correct"
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.correct))
            } else if (userAnswer == 0) {
                holder.tvStatus.text = "Skipped"
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.GRAY)
            } else {
                holder.tvStatus.text = "Incorrect"
                holder.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(holder.itemView.context.getColor(R.color.incorrect))
            }
            holder.tvStatus.setTextColor(Color.WHITE)
        }

        override fun getItemCount(): Int = questions.size
    }
}
