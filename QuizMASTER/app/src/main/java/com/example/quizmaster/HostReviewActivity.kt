package com.example.quizmaster

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.io.Serializable

class HostReviewActivity : AppCompatActivity() {

    private lateinit var rvGeneratedQuestions: RecyclerView
    private lateinit var btnCancelReview: Button
    private lateinit var btnApproveQuestions: Button
    private lateinit var reviewLoadingOverlay: FrameLayout
    private lateinit var tvReviewProcessingText: TextView

    private lateinit var roomId: String
    private lateinit var documentText: String
    private lateinit var category: String
    private lateinit var difficulty: String
    private lateinit var instructions: String
    private lateinit var questionList: MutableList<Question>
    private lateinit var adapter: ReviewQuestionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_review)

        roomId = intent.getStringExtra("ROOM_ID") ?: ""
        documentText = intent.getStringExtra("DOCUMENT_TEXT") ?: ""
        category = intent.getStringExtra("CATEGORY") ?: "Document Quiz"
        difficulty = intent.getStringExtra("DIFFICULTY") ?: "Medium"
        instructions = intent.getStringExtra("INSTRUCTIONS") ?: ""
        
        @Suppress("UNCHECKED_CAST")
        val initialQuestions = intent.getSerializableExtra("QUESTIONS") as? ArrayList<Question>
        questionList = initialQuestions?.toMutableList() ?: mutableListOf()

        rvGeneratedQuestions = findViewById(R.id.rvGeneratedQuestions)
        btnCancelReview = findViewById(R.id.btnCancelReview)
        btnApproveQuestions = findViewById(R.id.btnApproveQuestions)
        reviewLoadingOverlay = findViewById(R.id.reviewLoadingOverlay)
        tvReviewProcessingText = findViewById(R.id.tvReviewProcessingText)

        rvGeneratedQuestions.layoutManager = LinearLayoutManager(this)
        adapter = ReviewQuestionsAdapter()
        rvGeneratedQuestions.adapter = adapter

        btnCancelReview.setOnClickListener {
            finish()
        }

        btnApproveQuestions.setOnClickListener {
            approveAndSaveQuestions()
        }
    }

    private fun approveAndSaveQuestions() {
        if (questionList.isEmpty()) {
            Toast.makeText(this, "No questions to approve. Please generate some first.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate all questions have valid inputs before uploading
        for (i in 0 until questionList.size) {
            val q = questionList[i]
            if (q.question.isBlank() || q.option1.isBlank() || q.option2.isBlank() || q.option3.isBlank() || q.option4.isBlank()) {
                Toast.makeText(this, "Question ${i + 1} has empty fields. Please fill them out.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        reviewLoadingOverlay.visibility = View.VISIBLE
        tvReviewProcessingText.text = "Uploading quiz to room..."

        RoomManager.updateRoomQuestions(roomId, category, questionList) { success, errorMsg ->
            reviewLoadingOverlay.visibility = View.GONE
            if (success) {
                Toast.makeText(this, "Questions published to room!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save questions: $errorMsg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun regenerateIndividualQuestion(position: Int) {
        reviewLoadingOverlay.visibility = View.VISIBLE
        tvReviewProcessingText.text = "AI is generating a replacement..."

        lifecycleScope.launch {
            try {
                val enhancedPrompt = if (instructions.isNotBlank()) {
                    "$instructions. Make sure the question is unique and doesn't match these existing ones: " + 
                            questionList.joinToString { it.question }
                } else {
                    "Generate a unique question that does not match these existing ones: " + 
                            questionList.joinToString { it.question }
                }

                val (generatedList, errorMsg) = AIQuestionGenerator.generateQuestionsDetailed(
                    documentText, category, difficulty, 1, enhancedPrompt
                )

                if (generatedList.isNotEmpty()) {
                    questionList[position] = generatedList[0]
                    adapter.notifyItemChanged(position)
                    Toast.makeText(this@HostReviewActivity, "Question replaced!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@HostReviewActivity, "AI failed to replace: ${errorMsg ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@HostReviewActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                reviewLoadingOverlay.visibility = View.GONE
            }
        }
    }

    inner class ReviewQuestionsAdapter : RecyclerView.Adapter<ReviewQuestionsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_host_question_review, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = questionList.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvLabel: TextView = itemView.findViewById(R.id.tvQuestionIndexLabel)
            private val btnRegenerate: ImageButton = itemView.findViewById(R.id.btnRegenerateItem)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteItem)
            private val etQuestionText: EditText = itemView.findViewById(R.id.etReviewQuestionText)
            private val etOption1: EditText = itemView.findViewById(R.id.etReviewOption1)
            private val etOption2: EditText = itemView.findViewById(R.id.etReviewOption2)
            private val etOption3: EditText = itemView.findViewById(R.id.etReviewOption3)
            private val etOption4: EditText = itemView.findViewById(R.id.etReviewOption4)
            private val spinnerCorrectAnswer: Spinner = itemView.findViewById(R.id.spinnerReviewCorrectAnswer)

            init {
                val correctChoices = arrayOf("Option 1 is Correct", "Option 2 is Correct", "Option 3 is Correct", "Option 4 is Correct")
                spinnerCorrectAnswer.adapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_dropdown_item, correctChoices)

                // Save field edits back to the mutable backing array list as focus changes
                val focusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        saveCurrentState(bindingAdapterPosition)
                    }
                }

                etQuestionText.onFocusChangeListener = focusChangeListener
                etOption1.onFocusChangeListener = focusChangeListener
                etOption2.onFocusChangeListener = focusChangeListener
                etOption3.onFocusChangeListener = focusChangeListener
                etOption4.onFocusChangeListener = focusChangeListener

                spinnerCorrectAnswer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        val currentPos = bindingAdapterPosition
                        if (currentPos in questionList.indices) {
                            val oldQ = questionList[currentPos]
                            questionList[currentPos] = oldQ.copy(correctAnswer = pos + 1)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }

            fun bind(position: Int) {
                val q = questionList[position]
                tvLabel.text = "Question ${position + 1} of ${questionList.size}"
                
                etQuestionText.setText(q.question)
                etOption1.setText(q.option1)
                etOption2.setText(q.option2)
                etOption3.setText(q.option3)
                etOption4.setText(q.option4)
                
                spinnerCorrectAnswer.setSelection(q.correctAnswer - 1)

                btnDelete.setOnClickListener {
                    val currentPos = bindingAdapterPosition
                    if (currentPos in questionList.indices) {
                        questionList.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        notifyItemRangeChanged(currentPos, questionList.size)
                    }
                }

                btnRegenerate.setOnClickListener {
                    val currentPos = bindingAdapterPosition
                    if (currentPos in questionList.indices) {
                        saveCurrentState(currentPos)
                        regenerateIndividualQuestion(currentPos)
                    }
                }
            }

            private fun saveCurrentState(pos: Int) {
                if (pos in questionList.indices) {
                    val oldQ = questionList[pos]
                    questionList[pos] = Question(
                        id = oldQ.id,
                        question = etQuestionText.text.toString(),
                        option1 = etOption1.text.toString(),
                        option2 = etOption2.text.toString(),
                        option3 = etOption3.text.toString(),
                        option4 = etOption4.text.toString(),
                        correctAnswer = spinnerCorrectAnswer.selectedItemPosition + 1
                    )
                }
            }
        }
    }
}