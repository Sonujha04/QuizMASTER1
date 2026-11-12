package com.example.quizmaster

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AIQuestionGenerator {

    private fun getGenerativeModel(modelName: String = "gemini-3.8-flash"): GenerativeModel {
        val config = generationConfig {
            responseMimeType = "application/json"
        }
        return Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            generationConfig = config
        )
    }

    /**
     * Generates a list of validated, non-repeated MCQs from the document text.
     * Returns a Pair containing the list of questions and an optional error message.
     */
    suspend fun generateQuestionsDetailed(
        documentText: String,
        category: String,
        difficulty: String,
        count: Int,
        instructions: String = "",
    ): Pair<List<Question>, String?> = withContext(Dispatchers.IO) {

        val cleanedText = documentText.trim().replace("\\s+".toRegex(), " ")
        if (cleanedText.isBlank()) return@withContext emptyList<Question>() to "Extracted document text is empty."

        val processedText = if (cleanedText.length > 30000) cleanedText.substring(0, 30000) else cleanedText

        val prompt = """
            You are generating multiple-choice questions for an educational quiz.
            
            DOCUMENT CONTENT:
            $processedText
            
            CATEGORY: $category
            DIFFICULTY: $difficulty
            NUMBER OF QUESTIONS: $count
            TOPIC/INSTRUCTION: $instructions
            
            STRICT RULES:
            1. Generate questions ONLY from the supplied document content.
            2. Do not use external knowledge. Do not invent facts.
            3. Respect the selected category.
            4. Respect the selected difficulty.
            5. Follow the TOPIC/INSTRUCTION if provided.
            6. Generate exactly $count questions.
            7. Every question must have exactly four unique options.
            8. Only one option must be correct.
            9. Provide a brief explanation for the correct answer.
            
            JSON SCHEMA:
            {
              "questions": [
                {
                  "question": "string",
                  "options": ["string", "string", "string", "string"],
                  "correctAnswer": integer,
                  "explanation": "string"
                }
              ]
            }
            Note: "correctAnswer" must be the 1-based index (1-4) of the correct option in the "options" array.
            
            IMPORTANT: Return ONLY the JSON object. Do not include markdown formatting or any other text before or after the JSON.
        """.trimIndent()

        val candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
        var lastError = ""

        for (modelName in candidateModels) {
            try {
                Log.d("AI_GEN", "Sending request using model: $modelName...")
                val model = getGenerativeModel(modelName)
                val response = model.generateContent(prompt)
                val jsonText = response.text
                
                if (jsonText.isNullOrBlank()) {
                    lastError = "AI returned empty content with model $modelName."
                    continue
                }
                
                Log.d("AI_GEN", "Received Response from $modelName: $jsonText")
                val questions = parseAndValidateQuestions(jsonText)
                
                if (questions.isNotEmpty()) {
                    val finalQuestions = questions.distinctBy { it.question.lowercase().trim() }.take(count)
                    return@withContext finalQuestions to null
                } else {
                    lastError = "Could not parse questions from response of $modelName."
                }
            } catch (e: Exception) {
                val rawMessage = e.message ?: "Unknown error"
                val causeMessage = e.cause?.message
                val fullError = if (causeMessage != null) "$rawMessage ($causeMessage)" else rawMessage

                Log.e("AI_GEN_ERROR", "Error with model $modelName (${e.javaClass.simpleName}): $fullError")
                lastError = fullError
                
                // Let's check for specific response stopped errors if we can by string name
                if (e.javaClass.simpleName.contains("ResponseStopped")) {
                    return@withContext emptyList<Question>() to "AI stopped the response (Safety filters). Try a different document."
                }
            }
        }

        val userFriendlyError = "Failed to generate questions. Check that google-services.json is valid and the Gemini Developer API is enabled in Firebase Console > AI Logic. Error: $lastError"
        return@withContext emptyList<Question>() to userFriendlyError
    }

    // Keeping the old method for compatibility, but updating implementation to call the detailed one
    suspend fun generateQuestions(
        documentText: String,
        category: String,
        difficulty: String,
        count: Int
    ): List<Question> {
        return generateQuestionsDetailed(documentText, category, difficulty, count).first
    }

    private fun parseAndValidateQuestions(jsonText: String): List<Question> {
        val questions = mutableListOf<Question>()
        try {
            // Robust JSON extraction: Find the first '{' and last '}'
            val startIdx = jsonText.indexOf('{')
            val endIdx = jsonText.lastIndexOf('}')
            
            if ((startIdx == -1) || (endIdx == -1) || (endIdx <= startIdx)) {
                Log.e("AI_GEN", "No JSON object found in response")
                return emptyList()
            }
            
            val cleanedJson = jsonText.substring(startIdx, endIdx + 1)
            Log.d("AI_GEN", "Cleaned JSON for parsing: $cleanedJson")
            
            val jsonObject = JSONObject(cleanedJson)
            val jsonArray = when {
                jsonObject.has("questions") -> jsonObject.getJSONArray("questions")
                jsonObject.has("quiz") -> jsonObject.getJSONArray("quiz")
                jsonObject.has("results") -> jsonObject.getJSONArray("results")
                else -> {
                    // Search for any JSONArray in the root object
                    val keys = jsonObject.keys()
                    var foundArray: org.json.JSONArray? = null
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = jsonObject.optJSONArray(key)
                        if (value != null) {
                            foundArray = value
                            break
                        }
                    }
                    foundArray ?: return emptyList()
                }
            }
            
            for (i in 0 until jsonArray.length()) {
                val qObj = jsonArray.getJSONObject(i)
                val questionText = qObj.optString("question", qObj.optString("text"))
                val optionsArray = qObj.optJSONArray("options")
                val correctAnswer = qObj.optInt("correctAnswer", qObj.optInt("answer", 0))
                
                if (questionText.isNotBlank() && (optionsArray != null) && (optionsArray.length() == 4)) {
                    val options = mutableListOf<String>()
                    for (j in 0 until 4) options.add(optionsArray.getString(j))
                    
                    if ((options.distinct().size == 4) && (correctAnswer in 1..4)) {
                        val id = java.util.UUID.randomUUID().toString()
                        questions.add(Question(id, questionText, options[0], options[1], options[2], options[3], correctAnswer))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AI_GEN", "JSON Parse Error: ${e.message}")
        }
        return questions
    }
}
