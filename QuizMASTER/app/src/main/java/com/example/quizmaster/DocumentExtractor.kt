package com.example.quizmaster

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.BufferedReader
import java.io.InputStreamReader

object DocumentExtractor {

    /**
     * Extracts text from the given Uri based on its file type.
     * Supports PDF, DOCX, PPTX, and TXT.
     */
    fun extractText(context: Context, uri: Uri): String {
        val extension = getFileExtension(context, uri)?.lowercase()
        return try {
            when (extension) {
                "pdf" -> extractFromPdf(context, uri)
                "docx" -> extractFromDocx(context, uri)
                "pptx" -> extractFromPptx(context, uri)
                "txt" -> extractFromTxt(context, uri)
                else -> {
                    // Fallback to reading as text if extension is unknown
                    extractFromTxt(context, uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractFromPdf(context: Context, uri: Uri): String {
        return try {
            PDFBoxResourceLoader.init(context)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    PDFTextStripper().getText(document)
                }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractFromDocx(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                XWPFDocument(inputStream).use { document ->
                    document.paragraphs.joinToString("\n") { it.text }
                }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractFromPptx(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                XMLSlideShow(inputStream).use { ppt ->
                    val text = StringBuilder()
                    for (slide in ppt.slides) {
                        for (shape in slide.shapes) {
                            if (shape is org.apache.poi.sl.usermodel.TextShape<*, *>) {
                                text.append(shape.text).append("\n")
                            }
                        }
                    }
                    text.toString()
                }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractFromTxt(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        
        Log.d("EXTRACTOR", "MimeType: $mimeType")

        if (mimeType != null) {
            when {
                mimeType.contains("pdf", ignoreCase = true) -> return "pdf"
                mimeType.contains("word", ignoreCase = true) || mimeType.contains("officedocument.wordprocessingml", ignoreCase = true) -> return "docx"
                mimeType.contains("presentation", ignoreCase = true) || mimeType.contains("powerpoint", ignoreCase = true) -> return "pptx"
                mimeType.contains("text", ignoreCase = true) -> return "txt"
            }
        }
        
        // Try to get from file name if mime type fails or is generic
        return try {
            val fileName = getFileName(context, uri)?.lowercase() ?: ""
            Log.d("EXTRACTOR", "FileName: $fileName")
            when {
                fileName.endsWith(".pdf") -> "pdf"
                fileName.endsWith(".docx") -> "docx"
                fileName.endsWith(".doc") -> "docx" // Handle as docx, POI might handle it or fail gracefully
                fileName.endsWith(".pptx") -> "pptx"
                fileName.endsWith(".ppt") -> "pptx"
                fileName.endsWith(".txt") -> "txt"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result
    }
}
