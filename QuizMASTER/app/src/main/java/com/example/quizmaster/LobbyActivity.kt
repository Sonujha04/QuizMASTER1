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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch

class LobbyActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var roomId: String? = null
    private var isHost = false
    private var selectedCategory: String = "General"

    private lateinit var llCreateJoin: View
    private lateinit var llLobby: View
    private lateinit var tvRoomId: TextView
    private lateinit var tvPlayerList: TextView
    private lateinit var btnStartGroupQuiz: Button
    private lateinit var etPlayerName: EditText

    // Host AI Generator Fields
    private lateinit var llHostAiContainer: LinearLayout
    private lateinit var btnHostSelectFile: MaterialCardView
    private lateinit var tvHostSelectLabel: TextView
    private lateinit var tvHostFileName: TextView
    private lateinit var spinnerHostQuestionCount: Spinner
    private lateinit var spinnerHostDifficulty: Spinner
    private lateinit var etHostInstructions: EditText
    private lateinit var btnHostGenerateQuiz: Button
    private lateinit var hostLoadingOverlay: FrameLayout
    private lateinit var tvHostProcessingTitle: TextView
    private lateinit var tilHostCustomCount: com.google.android.material.textfield.TextInputLayout
    private lateinit var etHostCustomCount: com.google.android.material.textfield.TextInputEditText

    private var selectedFileUri: Uri? = null

    // Firebase Listener References for Lifecycle Cleanup
    private var playersRef: DatabaseReference? = null
    private var playersListener: ValueEventListener? = null
    private var statusRef: DatabaseReference? = null
    private var statusListener: ValueEventListener? = null
    private var roomDetailsRef: DatabaseReference? = null
    private var roomDetailsListener: ValueEventListener? = null

    private var hasQuestionsReady = false
    private var playerCount = 0

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            val name = getFileName(uri)
            tvHostFileName.text = "Selected: $name"
            tvHostSelectLabel.text = "Change Document"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        llCreateJoin = findViewById(R.id.llCreateJoin)
        llLobby = findViewById(R.id.llLobby)
        tvRoomId = findViewById(R.id.tvRoomId)
        tvPlayerList = findViewById(R.id.tvPlayerList)
        btnStartGroupQuiz = findViewById(R.id.btnStartGroupQuiz)
        etPlayerName = findViewById(R.id.etPlayerName)

        // Host AI Binding
        llHostAiContainer = findViewById(R.id.llHostAiContainer)
        btnHostSelectFile = findViewById(R.id.btnHostSelectFile)
        tvHostSelectLabel = findViewById(R.id.tvHostSelectLabel)
        tvHostFileName = findViewById(R.id.tvHostFileName)
        spinnerHostQuestionCount = findViewById(R.id.spinnerHostQuestionCount)
        spinnerHostDifficulty = findViewById(R.id.spinnerHostDifficulty)
        etHostInstructions = findViewById(R.id.etHostInstructions)
        btnHostGenerateQuiz = findViewById(R.id.btnHostGenerateQuiz)
        hostLoadingOverlay = findViewById(R.id.hostLoadingOverlay)
        tvHostProcessingTitle = findViewById(R.id.tvHostProcessingTitle)
        tilHostCustomCount = findViewById(R.id.tilHostCustomCount)
        etHostCustomCount = findViewById(R.id.etHostCustomCount)

        setupHostSpinners()

        val btnCreateRoom = findViewById<Button>(R.id.btnCreateRoom)
        val btnJoinRoom = findViewById<Button>(R.id.btnJoinRoom)
        val etRoomId = findViewById<EditText>(R.id.etRoomId)

        btnCreateRoom.setOnClickListener {
            createRoom("General")
        }

        btnJoinRoom.setOnClickListener {
            val id = etRoomId.text.toString().trim().uppercase()
            val name = etPlayerName.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, "Please enter Room Code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            joinRoom(id, name)
        }

        btnStartGroupQuiz.setOnClickListener {
            startQuiz()
        }
        
        btnHostSelectFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        btnHostGenerateQuiz.setOnClickListener {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a study document first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateAiQuestionsForGroup()
        }

        btnStartGroupQuiz.isEnabled = false
    }

    private fun setupHostSpinners() {
        val counts = arrayOf("5 Questions", "10 Questions", "15 Questions", "20 Questions", "25 Questions", "30 Questions", "Custom")
        spinnerHostQuestionCount.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, counts)
        spinnerHostQuestionCount.setSelection(1) // Default 10

        spinnerHostQuestionCount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tilHostCustomCount.visibility = if (counts[position] == "Custom") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val difficulties = arrayOf("Easy", "Medium", "Hard")
        spinnerHostDifficulty.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, difficulties)
        spinnerHostDifficulty.setSelection(1) // Default Medium
    }

    private fun generateAiQuestionsForGroup() {
        val uri = selectedFileUri ?: return
        val currentRoomId = roomId ?: return

        hostLoadingOverlay.visibility = View.VISIBLE
        btnHostGenerateQuiz.isEnabled = false
        tvHostProcessingTitle.text = "Reading document..."

        lifecycleScope.launch {
            try {
                android.util.Log.d("LOBBY_GEN", "Starting extraction for URI: $uri")
                val extractedText = DocumentExtractor.extractText(this@LobbyActivity, uri)
                android.util.Log.d("LOBBY_GEN", "Extraction complete. Length: ${extractedText.length}")

                if (extractedText.isBlank()) {
                    Toast.makeText(this@LobbyActivity, "Could not extract text. Verify file is not empty.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                tvHostProcessingTitle.text = "AI is generating questions..."
                val countText = spinnerHostQuestionCount.selectedItem.toString()
                val questionCount = if (countText == "Custom") {
                    etHostCustomCount.text.toString().toIntOrNull() ?: 10
                } else {
                    countText.split(" ")[0].toIntOrNull() ?: 10
                }
                val difficulty = spinnerHostDifficulty.selectedItem.toString()
                val customInstructions = etHostInstructions.text.toString().trim()

                android.util.Log.d("LOBBY_GEN", "Calling AI with count=$questionCount, diff=$difficulty")
                val (generatedQuestions, errorMsg) = AIQuestionGenerator.generateQuestionsDetailed(
                    extractedText, selectedCategory, difficulty, questionCount, customInstructions
                )
                android.util.Log.d("LOBBY_GEN", "AI Response received. Count: ${generatedQuestions.size}, Error: $errorMsg")

                if (generatedQuestions.isEmpty()) {
                    Toast.makeText(this@LobbyActivity, "Failed: ${errorMsg ?: "No questions generated."}", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this@LobbyActivity, HostReviewActivity::class.java)
                    intent.putExtra("ROOM_ID", currentRoomId)
                    intent.putExtra("DOCUMENT_TEXT", extractedText)
                    intent.putExtra("CATEGORY", selectedCategory)
                    intent.putExtra("DIFFICULTY", difficulty)
                    intent.putExtra("INSTRUCTIONS", customInstructions)
                    intent.putExtra("QUESTIONS", ArrayList(generatedQuestions))
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LobbyActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                hostLoadingOverlay.visibility = View.GONE
                btnHostGenerateQuiz.isEnabled = true
            }
        }
    }

    private fun createRoom(category: String) {
        RoomManager.createRoom(category, emptyList()) { code, errorMsg ->
            if (code != null) {
                roomId = code
                isHost = true
                showLobby(code)
                listenForPlayers(code)
                listenForRoomDetails(code)
                listenForStatus(code)
            } else {
                Toast.makeText(this, "Failed to create room: $errorMsg", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun joinRoom(id: String, name: String) {
        RoomManager.joinRoom(id, name) { errMsg ->
            if (errMsg == null) {
                roomId = id
                isHost = false
                showLobby(id)
                listenForPlayers(id)
                listenForRoomDetails(id)
                listenForStatus(id)
            } else {
                Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLobby(id: String) {
        llCreateJoin.visibility = View.GONE
        llLobby.visibility = View.VISIBLE
        tvRoomId.text = id
        if (isHost) {
            btnStartGroupQuiz.visibility = View.VISIBLE
            llHostAiContainer.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvLobbyTitle).text = "Host Lobby"
        } else {
            btnStartGroupQuiz.visibility = View.GONE
            llHostAiContainer.visibility = View.GONE
            findViewById<TextView>(R.id.tvLobbyTitle).text = "Waiting For Host"
        }
    }

    private fun listenForPlayers(id: String) {
        val ref = database.child("rooms").child(id).child("players")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val players = mutableListOf<String>()
                for (playerSnapshot in snapshot.children) {
                    val playerState = playerSnapshot.getValue(PlayerState::class.java)
                    playerState?.name?.let { players.add("✓ $it") }
                }
                playerCount = players.size
                if (isHost) {
                    updateHostStartButtonState()
                    tvPlayerList.text = if (playerCount == 0) "No players have joined yet." else players.joinToString("\n")
                    findViewById<TextView>(R.id.tvLobbyTitle).text = "Host Lobby ($playerCount Players)"
                } else {
                    tvPlayerList.text = if (playerCount == 0) "Waiting for players..." else players.joinToString("\n")
                    findViewById<TextView>(R.id.tvLobbyTitle).text = "Waiting For Host ($playerCount Players)"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        playersRef = ref
        playersListener = listener
    }

    private fun listenForRoomDetails(id: String) {
        val ref = database.child("rooms").child(id)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val totalQ = snapshot.child("totalQuestions").getValue(Int::class.java) ?: 0
                val categoryName = snapshot.child("category").getValue(String::class.java) ?: "General"
                val hasQuestionsInSnapshot = snapshot.child("questions").exists() && totalQ > 0

                hasQuestionsReady = hasQuestionsInSnapshot
                val tvQuestionStatus = findViewById<TextView>(R.id.tvQuestionStatus)

                if (isHost) {
                    if (hasQuestionsReady) {
                        tvQuestionStatus?.text = "✓ $totalQ Questions Ready ($categoryName)\nClick START QUIZ when all players are in."
                    } else {
                        tvQuestionStatus?.text = "No questions uploaded yet. Upload a study document above to generate questions."
                    }
                    updateHostStartButtonState()
                } else {
                    if (hasQuestionsReady) {
                        tvQuestionStatus?.text = "✓ $totalQ Questions Published ($categoryName)\nWaiting for host to start the quiz..."
                    } else {
                        tvQuestionStatus?.text = "Waiting for host to select and generate AI questions..."
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LobbyActivity, "Error loading room details: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        ref.addValueEventListener(listener)
        roomDetailsRef = ref
        roomDetailsListener = listener
    }

    private fun updateHostStartButtonState() {
        // Host can start quiz if questions are published and at least 1 player (including host) has joined
        btnStartGroupQuiz.isEnabled = hasQuestionsReady && (playerCount >= 0)
    }

    private fun listenForStatus(id: String) {
        val ref = database.child("rooms").child(id).child("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == RoomStatus.STARTED.name) {
                    val intent = Intent(this@LobbyActivity, QuizActivity::class.java)
                    intent.putExtra("IS_GROUP", true)
                    intent.putExtra("ROOM_ID", id)
                    startActivity(intent)
                    finish()
                } else if (status == RoomStatus.FINISHED.name) {
                    Toast.makeText(this@LobbyActivity, "Room finished or host disconnected.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        statusRef = ref
        statusListener = listener
    }

    private fun startQuiz() {
        roomId?.let { id ->
            RoomManager.startQuiz(id)
            val intent = Intent(this, HostDashboardActivity::class.java)
            intent.putExtra("ROOM_ID", id)
            startActivity(intent)
            finish()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        playersRef?.let { ref -> playersListener?.let { l -> ref.removeEventListener(l) } }
        statusRef?.let { ref -> statusListener?.let { l -> ref.removeEventListener(l) } }
        roomDetailsRef?.let { ref -> roomDetailsListener?.let { l -> ref.removeEventListener(l) } }
    }
}