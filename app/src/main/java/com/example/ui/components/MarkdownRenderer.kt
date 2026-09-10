package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBlockBg
import com.example.ui.theme.CodeBlockHeaderBg
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurplePrimary
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    textColor: Color = TextWhitePrimary
) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> CodeBlockView(block.language, block.code)
                is Block.Heading -> HeadingView(block.level, block.text, textColor)
                is Block.Quote -> QuoteView(block.text, textColor)
                is Block.Table -> TableView(block.headers, block.rows, textColor)
                is Block.BulletList -> BulletListView(block.items, textColor)
                is Block.OrderedList -> OrderedListView(block.items, textColor)
                is Block.Paragraph -> ParagraphView(block.text, textColor)
            }
        }
    }
}

private sealed class Block {
    data class Paragraph(val text: String) : Block()
    data class Heading(val level: Int, val text: String) : Block()
    data class Code(val language: String, val code: String) : Block()
    data class Quote(val text: String) : Block()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : Block()
    data class BulletList(val items: List<String>) : Block()
    data class OrderedList(val items: List<String>) : Block()
}

private fun parseMarkdownBlocks(markdown: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Check for code fence
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim().ifEmpty { "code" }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            // skip closing fence
            if (i < lines.size && lines[i].trim().startsWith("```")) {
                i++
            }
            blocks.add(Block.Code(language, codeLines.joinToString("\n")))
            continue
        }

        // Headings
        if (line.startsWith("### ")) {
            blocks.add(Block.Heading(3, line.removePrefix("### ").trim()))
            i++
            continue
        } else if (line.startsWith("## ")) {
            blocks.add(Block.Heading(2, line.removePrefix("## ").trim()))
            i++
            continue
        } else if (line.startsWith("# ")) {
            blocks.add(Block.Heading(1, line.removePrefix("# ").trim()))
            i++
            continue
        }

        // Blockquotes
        if (line.trim().startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(Block.Quote(quoteLines.joinToString("\n")))
            continue
        }

        // Tables
        if (line.contains("|") && line.trim().startsWith("|")) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size && lines[i].contains("|") && lines[i].trim().startsWith("|")) {
                tableLines.add(lines[i])
                i++
            }
            if (tableLines.size >= 2) {
                val headers = tableLines[0].split("|")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                // skip separator line if present (|---|---|)
                val rowLines = if (tableLines[1].contains("-")) tableLines.drop(2) else tableLines.drop(1)
                val rows = rowLines.map { row ->
                    row.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                }
                blocks.add(Block.Table(headers, rows))
                continue
            }
        }

        // Bullet lists
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
            val items = mutableListOf<String>()
            while (i < lines.size && (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* "))) {
                items.add(lines[i].trim().substring(2).trim())
                i++
            }
            blocks.add(Block.BulletList(items))
            continue
        }

        // Ordered lists
        val orderedRegex = Regex("^(\\d+)\\.\\s+(.*)")
        if (orderedRegex.matches(line.trim())) {
            val items = mutableListOf<String>()
            while (i < lines.size && orderedRegex.matches(lines[i].trim())) {
                val match = orderedRegex.find(lines[i].trim())
                if (match != null) {
                    items.add(match.groupValues[2])
                }
                i++
            }
            blocks.add(Block.OrderedList(items))
            continue
        }

        // Paragraph
        if (line.isNotBlank()) {
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size && lines[i].isNotBlank() &&
                !lines[i].trim().startsWith("```") &&
                !lines[i].startsWith("#") &&
                !lines[i].trim().startsWith(">") &&
                !lines[i].trim().startsWith("- ") &&
                !lines[i].trim().startsWith("* ") &&
                !orderedRegex.matches(lines[i].trim())
            ) {
                paragraphLines.add(lines[i])
                i++
            }
            blocks.add(Block.Paragraph(paragraphLines.joinToString("\n")))
        } else {
            i++
        }
    }

    return blocks
}

@Composable
private fun CodeBlockView(language: String, code: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }
    val lines = remember(code) { code.lines() }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CodeBlockBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2B224C), RoundedCornerShape(10.dp))
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeBlockHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    color = NeonPurpleGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextGraySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("code", code)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "Copied code to clipboard", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = if (isCopied) NeonPurplePrimary else TextGraySecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Code Content
            AnimatedVisibility(visible = isExpanded) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                        .horizontalScroll(scrollState)
                ) {
                    // Line numbers
                    Column(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.indices.forEach { idx ->
                            Text(
                                text = "${idx + 1}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height((lines.size * 18).dp)
                            .background(Color(0xFF261E45))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Code text with basic syntax highlight
                    Column {
                        lines.forEach { line ->
                            Text(
                                text = highlightSyntax(line),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun highlightSyntax(line: String): AnnotatedString {
    return buildAnnotatedString {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
            withStyle(SpanStyle(color = Color(0xFF6B7280), fontStyle = FontStyle.Italic)) {
                append(line)
            }
            return@buildAnnotatedString
        }

        val keywords = setOf(
            "val", "var", "fun", "class", "interface", "object", "return", "if", "else", "for",
            "while", "import", "package", "public", "private", "protected", "override", "suspend",
            "const", "data", "def", "from", "as", "let", "const", "function", "async", "await"
        )

        val words = line.split(Regex("(?<=[\\s(),.:;=])|(?=[\\s(),.:;=])"))
        var inQuotes = false

        for (word in words) {
            when {
                word.startsWith("\"") || word.endsWith("\"") || inQuotes -> {
                    withStyle(SpanStyle(color = Color(0xFF34D399))) {
                        append(word)
                    }
                    if (word.startsWith("\"") && !word.endsWith("\"")) inQuotes = true
                    if (word.endsWith("\"") && !word.startsWith("\"")) inQuotes = false
                }
                word in keywords -> {
                    withStyle(SpanStyle(color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                word.toIntOrNull() != null -> {
                    withStyle(SpanStyle(color = Color(0xFF60A5FA))) {
                        append(word)
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = Color(0xFFF3F4F6))) {
                        append(word)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadingView(level: Int, text: String, textColor: Color) {
    val (fontSize, fontWeight) = when (level) {
        1 -> 20.sp to FontWeight.Bold
        2 -> 18.sp to FontWeight.SemiBold
        else -> 16.sp to FontWeight.Medium
    }
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = NeonPurpleGlow,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun QuoteView(text: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF16122C))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(NeonPurplePrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextGraySecondary,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun BulletListView(items: List<String>, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, end = 8.dp)
                        .size(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonPurplePrimary)
                )
                Text(
                    text = parseInlineMarkdown(item),
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun OrderedListView(items: List<String>, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}.",
                    color = NeonPurpleGlow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = parseInlineMarkdown(item),
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun TableView(headers: List<String>, rows: List<List<String>>, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF141026),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF271F47), RoundedCornerShape(8.dp))
    ) {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.horizontalScroll(scrollState).padding(8.dp)) {
            // Header Row
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                headers.forEach { h ->
                    Text(
                        text = h,
                        color = NeonPurpleGlow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF2D2452), thickness = 1.dp)

            // Rows
            rows.forEach { r ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    r.forEach { cell ->
                        Text(
                            text = cell,
                            color = textColor,
                            fontSize = 13.sp,
                            modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParagraphView(text: String, textColor: Color) {
    Text(
        text = parseInlineMarkdown(text),
        color = textColor,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}

fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            // Bold: **text**
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Inline code: `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            background = Color(0xFF261D45),
                            color = NeonPurpleGlow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                    continue
                }
            }

            // Italic: *text*
            if (text[i] == '*') {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}
