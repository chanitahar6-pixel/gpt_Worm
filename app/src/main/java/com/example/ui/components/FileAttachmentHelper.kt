package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.BufferedReader
import java.io.InputStreamReader

object FileAttachmentHelper {
    fun readFileFromUri(context: Context, uri: Uri): AttachedFilePreview? {
        try {
            var fileName = "document.txt"
            var fileSize = 0L

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = it.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = it.getLong(sizeIndex)
                }
            }

            val mimeType = context.contentResolver.getType(uri) ?: "text/plain"

            // Read text safely up to 250 KB
            val contentBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                var line: String?
                var totalChars = 0
                val maxChars = 200_000

                while (reader.readLine().also { line = it } != null) {
                    if (totalChars > maxChars) {
                        contentBuilder.append("\n... [Truncated for analysis length] ...")
                        break
                    }
                    contentBuilder.append(line).append("\n")
                    totalChars += line?.length ?: 0
                }
            }

            val content = contentBuilder.toString()
            if (fileSize <= 0) {
                fileSize = content.toByteArray().size.toLong()
            }

            return AttachedFilePreview(
                name = fileName,
                size = fileSize,
                mimeType = mimeType,
                content = content
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun buildPromptWithFile(prompt: String, file: AttachedFilePreview): String {
        val extension = file.name.substringAfterLast('.', "txt")
        val cleanPrompt = prompt.ifBlank { "Analyze and explain this file" }
        return """
[Attached File: ${file.name} (${formatFileSize(file.size)})]
```$extension
${file.content.trim()}
```

$cleanPrompt
""".trimIndent()
    }
}
