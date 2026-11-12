package com.example.quizmaster

import android.os.CountDownTimer
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.os.*
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.appcompat.app.AlertDialog
import android.widget.*
import com.google.firebase.database.*

class QuizActivity : AppCompatActivity() {

    private lateinit var tvCategoryName: TextView
    private lateinit var tvQuestionNumber: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTimer: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button
    private lateinit var btn5050: Button
    private lateinit var btnFreeze: Button

    private var soundPool: SoundPool? = null
    private var tickSoundId = 0
    private var tickStreamId = 0
    private var correctSoundId = 0
    private var wrongSoundId = 0

    private var timer: CountDownTimer? = null

    private var currentQuestion = 0
    private var score = 0
    private var firstTryScore = 0
    private var skippedScore = 0
    private val skippedQuestionIndices = mutableListOf<Int>()
    private var isSkippedPhase = false
    private var hasUsed5050 = false
    private var hasUsedFreeze = false
    private var isTimerFrozen = false
    private var isTimerRunning = false
    private var isTickLoaded = false
    private var isAnswerSubmitted = false

    private lateinit var questions: List<Question>
    private lateinit var userAnswers: IntArray
    private var optShuffle: Map<String, List<Int>>? = null

    // Firebase Listeners
    private var currentQuestionRef: DatabaseReference? = null
    private var currentQuestionListener: ValueEventListener? = null
    private var roomStatusRef: DatabaseReference? = null
    private var roomStatusListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        initSoundPool()

        tvQuestionNumber = findViewById(R.id.tvQuestionNumber)
        tvCategoryName = findViewById(R.id.tvCategoryName)
        tvTimer = findViewById(R.id.tvTimer)
        tvQuestion = findViewById(R.id.tvQuestion)
        progressBar = findViewById(R.id.progressBar)
        radioGroup = findViewById(R.id.radioGroup)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        btn5050 = findViewById(R.id.btn5050)
        btnFreeze = findViewById(R.id.btnFreeze)

        val category = intent.getStringExtra("CATEGORY") ?: "General"
        tvCategoryName.text = category

        val customQuestions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("CUSTOM_QUESTIONS", ArrayList::class.java) as? ArrayList<Question>
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("CUSTOM_QUESTIONS") as? ArrayList<Question>
        }

        val isGroup = intent.getBooleanExtra("IS_GROUP", false)
        val roomId = intent.getStringExtra("ROOM_ID") ?: ""

        if (isGroup && roomId.isNotEmpty()) {
            tvQuestion.text = "Loading room questions..."
            setControlsEnabled(false)

            if (customQuestions != null && customQuestions.isNotEmpty()) {
                setupGroupQuiz(roomId, customQuestions)
            } else {
                val questionsRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("questions")
                questionsRef.get().addOnSuccessListener { snapshot ->
                    val fetchedQuestions = mutableListOf<Question>()
                    for (qSnap in snapshot.children) {
                        val q = qSnap.getValue(Question::class.java)
                        if (q != null && q.question.isNotBlank()) {
                            fetchedQuestions.add(q)
                        }
                    }

                    if (fetchedQuestions.isNotEmpty()) {
                        setupGroupQuiz(roomId, fetchedQuestions)
                    } else {
                        Toast.makeText(this@QuizActivity, "No questions found in this room.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }.addOnFailureListener { err ->
                    Toast.makeText(this@QuizActivity, "Failed to load questions: ${err.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } else {
            questions = if (customQuestions != null && customQuestions.isNotEmpty()) {
                customQuestions
            } else {
                getQuestions(category)
            }
            userAnswers = IntArray(questions.size)
            showQuestion()
        }

        btnNext.setOnClickListener {
            checkAnswer()
        }

        btnSkip.setOnClickListener {
            skipQuestion()
        }

        btn5050.setOnClickListener {
            use5050()
        }

        btnFreeze.setOnClickListener {
            useFreeze()
        }
    }

    private fun setupGroupQuiz(roomId: String, questionList: List<Question>) {
        // Use a fixed room seed so ALL participants get identical question & option order
        val seed = roomId.hashCode().toLong()

        val qOrder = ShuffleUtility.generateQuestionOrder(questionList.size, seed)
        val shuffledQuestions = mutableListOf<Question>()
        val optShuffleMap = mutableMapOf<String, List<Int>>()

        for (idx in qOrder) {
            if (idx in questionList.indices) {
                val q = questionList[idx]
                shuffledQuestions.add(q)
                val qSeed = seed + q.id.hashCode()
                optShuffleMap[q.id] = ShuffleUtility.generateOptionOrder(qSeed)
            }
        }

        questions = if (shuffledQuestions.isNotEmpty()) shuffledQuestions else questionList
        optShuffle = optShuffleMap
        userAnswers = IntArray(questions.size)

        RoomManager.updatePlayerStateWithShuffle(roomId, qOrder, optShuffleMap)
        RoomManager.updatePlayerProgress(roomId, currentQuestion, score)
        RoomManager.updatePlayerStatus(roomId, "PLAYING")

        listenForGroupCurrentQuestion(roomId)
        listenForGroupRoomStatus(roomId)

        setControlsEnabled(true)
        showQuestion()
    }

    private fun listenForGroupCurrentQuestion(roomId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("currentQuestion")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomQuestionIdx = snapshot.getValue(Int::class.java) ?: 0
                if (roomQuestionIdx != currentQuestion && roomQuestionIdx < questions.size) {
                    currentQuestion = roomQuestionIdx
                    isAnswerSubmitted = false
                    runOnUiThread {
                        setControlsEnabled(true)
                        showQuestion()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        currentQuestionRef = ref
        currentQuestionListener = listener
    }

    private fun listenForGroupRoomStatus(roomId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("rooms").child(roomId).child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusStr = snapshot.getValue(String::class.java)
                if (statusStr == RoomStatus.FINISHED.name && currentQuestion < questions.size - 1) {
                    Toast.makeText(this@QuizActivity, "Host ended the quiz.", Toast.LENGTH_SHORT).show()
                    finishQuiz()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        roomStatusRef = ref
        roomStatusListener = listener
    }

    private fun setControlsEnabled(enabled: Boolean) {
        findViewById<RadioButton>(R.id.rbOption1).isEnabled = enabled
        findViewById<RadioButton>(R.id.rbOption2).isEnabled = enabled
        findViewById<RadioButton>(R.id.rbOption3).isEnabled = enabled
        findViewById<RadioButton>(R.id.rbOption4).isEnabled = enabled
        btnSkip.isEnabled = enabled && !isSkippedPhase
        btn5050.isEnabled = enabled && !hasUsed5050
        btnFreeze.isEnabled = enabled && !hasUsedFreeze

        if (enabled) {
            btnNext.text = if (isSkippedPhase) {
                if (skippedQuestionIndices.isEmpty()) "FINISH" else "NEXT"
            } else {
                if ((currentQuestion == questions.size - 1) && skippedQuestionIndices.isEmpty()) "FINISH" else "NEXT"
            }
        } else {
            btnNext.text = "Waiting for Host..."
        }
    }

    private fun use5050() {
        if (hasUsed5050) return
        hasUsed5050 = true
        btn5050.isEnabled = false
        btn5050.alpha = 0.5f

        val currentQ = questions[currentQuestion]
        val optionOrder = optShuffle?.get(currentQ.id) ?: listOf(1, 2, 3, 4)

        val correctUiIndex = optionOrder.indexOf(currentQ.correctAnswer) + 1

        val uiOptions = mutableListOf(1, 2, 3, 4)
        uiOptions.remove(correctUiIndex)
        uiOptions.shuffle()

        val toHide = uiOptions.take(2)
        toHide.forEach { uiIndex ->
            when (uiIndex) {
                1 -> findViewById<RadioButton>(R.id.rbOption1).visibility = android.view.View.INVISIBLE
                2 -> findViewById<RadioButton>(R.id.rbOption2).visibility = android.view.View.INVISIBLE
                3 -> findViewById<RadioButton>(R.id.rbOption3).visibility = android.view.View.INVISIBLE
                4 -> findViewById<RadioButton>(R.id.rbOption4).visibility = android.view.View.INVISIBLE
            }
        }
        Toast.makeText(this, "50:50 Used!", Toast.LENGTH_SHORT).show()
    }

    private fun useFreeze() {
        if (hasUsedFreeze) return
        hasUsedFreeze = true
        isTimerFrozen = true
        btnFreeze.isEnabled = false
        btnFreeze.alpha = 0.5f
        timer?.cancel()
        stopTickSound()
        tvTimer.text = "❄️"
        tvTimer.setTextColor(Color.parseColor("#03A9F4"))
        Toast.makeText(this, "Timer Frozen!", Toast.LENGTH_SHORT).show()
    }

    private fun skipQuestion() {
        timer?.cancel()
        stopTickSound()
        skippedQuestionIndices.add(currentQuestion)
        moveToNext()
    }

    private fun getQuestions(category: String): List<Question> {
        return QuestionProvider.getQuestions(category)
    }

    private fun showQuestion() {
        if (!::questions.isInitialized || questions.isEmpty() || currentQuestion !in questions.indices) return

        val question = questions[currentQuestion]

        tvQuestionNumber.text = "Question ${currentQuestion + 1} of ${questions.size}"
        tvQuestion.text = question.question

        val option1 = findViewById<RadioButton>(R.id.rbOption1)
        val option2 = findViewById<RadioButton>(R.id.rbOption2)
        val option3 = findViewById<RadioButton>(R.id.rbOption3)
        val option4 = findViewById<RadioButton>(R.id.rbOption4)

        val optionOrder = optShuffle?.get(question.id) ?: listOf(1, 2, 3, 4)

        option1.text = "A) ${getOptionText(question, optionOrder[0])}"
        option2.text = "B) ${getOptionText(question, optionOrder[1])}"
        option3.text = "C) ${getOptionText(question, optionOrder[2])}"
        option4.text = "D) ${getOptionText(question, optionOrder[3])}"

        option1.visibility = android.view.View.VISIBLE
        option2.visibility = android.view.View.VISIBLE
        option3.visibility = android.view.View.VISIBLE
        option4.visibility = android.view.View.VISIBLE

        radioGroup.clearCheck()

        progressBar.max = questions.size
        progressBar.progress = currentQuestion + 1

        val isGroup = intent.getBooleanExtra("IS_GROUP", false)
        if (isGroup) {
            setControlsEnabled(!isAnswerSubmitted)
        } else {
            if (isSkippedPhase) {
                btnSkip.visibility = android.view.View.GONE
                btnNext.text = if (skippedQuestionIndices.isEmpty()) "FINISH" else "NEXT"
                tvQuestionNumber.text = "Skipped Question"
            } else {
                btnNext.text = if ((currentQuestion == questions.size - 1) && skippedQuestionIndices.isEmpty()) "FINISH" else "NEXT"
            }
        }

        if (!isTimerFrozen && !isAnswerSubmitted) {
            startTimer()
        }
    }

    private fun getOptionText(q: Question, index: Int): String {
        return when (index) {
            1 -> q.option1
            2 -> q.option2
            3 -> q.option3
            4 -> q.option4
            else -> ""
        }
    }

    private fun startTimer() {
        timer?.cancel()
        stopTickSound()

        if (isTimerFrozen) {
            tvTimer.text = "❄️"
            tvTimer.setTextColor(Color.parseColor("#03A9F4"))
            return
        }

        tvTimer.setTextColor(Color.parseColor("#424242"))

        isTimerRunning = true
        if (isTickLoaded) {
            tickStreamId = soundPool?.play(tickSoundId, 1f, 1f, 1, -1, 1f) ?: 0
        }

        timer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                tvTimer.text = secondsRemaining.toString()

                if (secondsRemaining <= 5) {
                    tvTimer.setTextColor(Color.RED)
                }
            }

            override fun onFinish() {
                stopTickSound()
                vibratePhone()
                showTimesUpDialog()
            }
        }.start()
    }

    private fun stopTickSound() {
        isTimerRunning = false
        if (tickStreamId != 0) {
            soundPool?.stop(tickStreamId)
            tickStreamId = 0
        }
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }

    private fun showTimesUpDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_times_up, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            dialog.dismiss()
            checkAnswer(fromTimer = true)
        }

        dialog.show()
    }

    private fun checkAnswer(fromTimer: Boolean = false) {
        if (isAnswerSubmitted) return

        if (fromTimer) {
            // Already handled by dialog
        } else if (radioGroup.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show()
            return
        }

        timer?.cancel()
        stopTickSound()

        val selectedAnswer = when (radioGroup.checkedRadioButtonId) {
            R.id.rbOption1 -> 1
            R.id.rbOption2 -> 2
            R.id.rbOption3 -> 3
            R.id.rbOption4 -> 4
            else -> 0
        }

        val question = questions[currentQuestion]
        val optionOrder = optShuffle?.get(question.id) ?: listOf(1, 2, 3, 4)
        val originalAnswerIndex = if (selectedAnswer > 0) optionOrder[selectedAnswer - 1] else 0

        userAnswers[currentQuestion] = originalAnswerIndex

        if (originalAnswerIndex == question.correctAnswer) {
            score++
            playSound(correctSoundId)
            if (isSkippedPhase) {
                skippedScore++
            } else {
                firstTryScore++
            }
        } else {
            playSound(wrongSoundId)
        }

        val isGroup = intent.getBooleanExtra("IS_GROUP", false)
        val roomId = intent.getStringExtra("ROOM_ID") ?: ""

        if (isGroup) {
            isAnswerSubmitted = true
            RoomManager.updatePlayerProgress(roomId, currentQuestion + 1, score)
            setControlsEnabled(false)

            if (currentQuestion == questions.size - 1) {
                finishQuiz()
            }
        } else {
            moveToNext()
        }
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == tickSoundId) {
                isTickLoaded = true
                if (isTimerRunning && tickStreamId == 0) {
                    tickStreamId = soundPool?.play(tickSoundId, 1f, 1f, 1, -1, 1f) ?: 0
                }
            }
        }

        val tickResId = resources.getIdentifier("tick", "raw", packageName)
        if (tickResId != 0) tickSoundId = soundPool?.load(this, tickResId, 1) ?: 0

        val correctResId = resources.getIdentifier("correct", "raw", packageName)
        if (correctResId != 0) correctSoundId = soundPool?.load(this, correctResId, 1) ?: 0

        val wrongResId = resources.getIdentifier("wrong", "raw", packageName)
        if (wrongResId != 0) wrongSoundId = soundPool?.load(this, wrongResId, 1) ?: 0
    }

    private fun playSound(soundId: Int) {
        if (soundId != 0) {
            soundPool?.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun moveToNext() {
        isTimerFrozen = false
        val isGroup = intent.getBooleanExtra("IS_GROUP", false)
        val roomId = intent.getStringExtra("ROOM_ID") ?: ""

        if (isGroup) {
            RoomManager.updatePlayerProgress(roomId, currentQuestion + 1, score)
        }

        if (isSkippedPhase) {
            if (skippedQuestionIndices.isNotEmpty()) {
                currentQuestion = skippedQuestionIndices.removeAt(0)
                showQuestion()
            } else {
                finishQuiz()
            }
        } else {
            if (currentQuestion < questions.size - 1) {
                currentQuestion++
                showQuestion()
            } else if (skippedQuestionIndices.isNotEmpty()) {
                isSkippedPhase = true
                currentQuestion = skippedQuestionIndices.removeAt(0)
                showQuestion()
            } else {
                finishQuiz()
            }
        }
    }

    private fun finishQuiz() {
        val category = intent.getStringExtra("CATEGORY") ?: "General"
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL", questions.size)
        intent.putExtra("FIRST_TRY_SCORE", firstTryScore)
        intent.putExtra("SKIPPED_SCORE", skippedScore)
        intent.putExtra("CATEGORY", category)
        intent.putExtra("QUESTIONS", ArrayList(questions))
        intent.putExtra("USER_ANSWERS", userAnswers)
        intent.putExtra("IS_GROUP", this.intent.getBooleanExtra("IS_GROUP", false))
        intent.putExtra("ROOM_ID", this.intent.getStringExtra("ROOM_ID"))
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        stopTickSound()
        soundPool?.release()
        soundPool = null
        currentQuestionRef?.let { ref -> currentQuestionListener?.let { l -> ref.removeEventListener(l) } }
        roomStatusRef?.let { ref -> roomStatusListener?.let { l -> ref.removeEventListener(l) } }
    }
}