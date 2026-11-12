package com.example.quizmaster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class UploadActivity : AppCompatActivity() {

    private lateinit var btnSelectFile: MaterialCardView
    private lateinit var tvFileName: TextView
    private lateinit var tvSelectLabel: TextView
    private lateinit var spinnerQuestionCount: Spinner
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var btnGenerateQuiz: Button
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var btnBack: ImageView
    private lateinit var tilCustomCount: com.google.android.material.textfield.TextInputLayout
    private lateinit var etCustomCount: com.google.android.material.textfield.TextInputEditText

    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val fileName = getFileName(uri)
            tvFileName.text = "Selected: $fileName"
            tvFileName.visibility = View.VISIBLE
            tvSelectLabel.text = "Change Document"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        btnSelectFile = findViewById(R.id.btnSelectFile)
        tvFileName = findViewById(R.id.tvFileName)
        tvSelectLabel = findViewById(R.id.tvSelectLabel)
        spinnerQuestionCount = findViewById(R.id.spinnerQuestionCount)
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty)
        btnGenerateQuiz = findViewById(R.id.btnGenerateQuiz)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        btnBack = findViewById(R.id.btnBack)
        tilCustomCount = findViewById(R.id.tilCustomCount)
        etCustomCount = findViewById(R.id.etCustomCount)

        setupSpinners()

        btnSelectFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        btnGenerateQuiz.setOnClickListener {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a document first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateQuiz()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSpinners() {
        val counts = arrayOf("5 Questions", "10 Questions", "15 Questions", "20 Questions", "25 Questions", "30 Questions", "Custom")
        spinnerQuestionCount.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, counts)

        spinnerQuestionCount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tilCustomCount.visibility = if (counts[position] == "Custom") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val difficulties = arrayOf("Easy", "Medium", "Hard")
        spinnerDifficulty.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, difficulties)
    }

    private fun generateQuiz() {
        val uri = selectedFileUri ?: return
        
        loadingOverlay.visibility = View.VISIBLE
        btnGenerateQuiz.isEnabled = false
        updateStatus("Reading document...")

        lifecycleScope.launch {
            try {
                // 1. Extract Text
                val extractedText = DocumentExtractor.extractText(this@UploadActivity, uri)
                android.util.Log.d("UPLOAD_DEBUG", "Extracted Text Length: ${extractedText.length}")
                
                if (extractedText.isBlank()) {
                    showError("Could not extract readable text from this document. Please upload a text-based file.")
                    return@launch
                }

                updateStatus("Generating questions...")

                // 2. Prepare Settings
                val category = "Document Quiz" // Fixed or inferred
                val difficulty = spinnerDifficulty.selectedItem.toString()
                val selectedCountText = spinnerQuestionCount.selectedItem.toString()
                val questionCount = if (selectedCountText == "Custom") {
                    etCustomCount.text.toString().toIntOrNull() ?: 10
                } else {
                    selectedCountText.split(" ")[0].toInt()
                }

                // 3. AI Generation
                val (generatedQuestions, errorMsg) = AIQuestionGenerator.generateQuestionsDetailed(
                    extractedText, category, difficulty, questionCount
                )

                if (generatedQuestions.isEmpty()) {
                    showError("Failed: ${errorMsg ?: "No questions generated."}")
                } else {
                    updateStatus("Preparing quiz...")
                    
                    // 4. Success -> Start Quiz
                    val intent = Intent(this@UploadActivity, QuizActivity::class.java)
                    intent.putExtra("CATEGORY", "Document Quiz")
                    intent.putExtra("CUSTOM_QUESTIONS", ArrayList(generatedQuestions))
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                showError("An error occurred: ${e.message}")
            } finally {
                loadingOverlay.visibility = View.GONE
                btnGenerateQuiz.isEnabled = true
            }
        }
    }

    private fun updateStatus(status: String) {
        // Find textview in overlay
        findViewById<TextView>(R.id.tvProcessingTitle)?.text = status
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        loadingOverlay.visibility = View.GONE
        btnGenerateQuiz.isEnabled = true
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path?.let { p ->
                val cut = p.lastIndexOf('/')
                if (cut != -1) p.substring(cut + 1) else p
            }
        }
        return result ?: "Unknown file"
    }
}
