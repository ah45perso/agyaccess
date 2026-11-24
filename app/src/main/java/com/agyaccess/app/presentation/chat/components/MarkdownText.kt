package com.agyaccess.app.presentation.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agyaccess.app.presentation.theme.CodeBorderColor
import com.agyaccess.app.presentation.theme.CodeHeaderColor
import com.agyaccess.app.presentation.theme.CodeSurfaceColor
import kotlinx.coroutines.delay

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class CodeFence(val language: String, val code: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class BulletItem(val bullet: String, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

/**
 * Moteur de rendu Markdown 100 % Jetpack Compose (sans WebView, zéro risque XSS)
 * optimisé pour le streaming token par token avec blocs de code colorés et copiables.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val inlineCodeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val linkColor = MaterialTheme.colorScheme.primary

    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = parseInlineMarkdown(block.text, inlineCodeBg, linkColor),
                            style = style,
                            color = textColor
                        )
                    }

                    is MarkdownBlock.CodeFence -> {
                        CodeBlockCard(
                            language = block.language,
                            code = block.code
                        )
                    }

                    is MarkdownBlock.Quote -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseInlineMarkdown(block.text, inlineCodeBg, linkColor),
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = textColor.copy(alpha = 0.88f)
                            )
                        }
                    }

                    is MarkdownBlock.BulletItem -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${block.bullet} ",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = parseInlineMarkdown(block.text, inlineCodeBg, linkColor),
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                        }
                    }

                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.text, inlineCodeBg, linkColor),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1_800L)
            copied = false
        }
    }

    val highlighted = remember(code, language) {
        SyntaxHighlighter.highlight(code, language)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CodeSurfaceColor,
        border = BorderStroke(1.dp, CodeBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeHeaderColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.ifBlank { "code" }.lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8B949E)
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = "Copier le code",
                        tint = if (copied) Color(0xFF3FB950) else Color(0xFF8B949E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlighted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val lines = raw.replace("\r\n", "\n").split("\n")
    val result = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        val text = paragraphBuffer.toString().trim()
        if (text.isNotEmpty()) {
            result.add(MarkdownBlock.Paragraph(text))
        }
        paragraphBuffer.clear()
    }

    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trimStart()

        // Bloc de code clôturé ```lang ... ``` (tolère un bloc non encore fermé en streaming)
        if (trimmed.startsWith("```")) {
            flushParagraph()
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                codeLines.add(lines[index])
                index++
            }
            if (index < lines.size && lines[index].trimStart().startsWith("```")) {
                index++
            }
            result.add(MarkdownBlock.CodeFence(language, codeLines.joinToString("\n")))
            continue
        }

        // Ligne vide = séparateur de paragraphe
        if (trimmed.isEmpty()) {
            flushParagraph()
            index++
            continue
        }

        // Titres # à ######
        val headingMatch = HEADING_REGEX.matchEntire(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            val level = headingMatch.groupValues[1].length
            val title = headingMatch.groupValues[2].trim()
            result.add(MarkdownBlock.Heading(level, title))
            index++
            continue
        }

        // Citations >
        if (trimmed.startsWith("> ")) {
            flushParagraph()
            result.add(MarkdownBlock.Quote(trimmed.removePrefix("> ").trim()))
            index++
            continue
        }

        // Listes à puces ou numérotées
        val bulletMatch = BULLET_REGEX.matchEntire(trimmed)
        if (bulletMatch != null) {
            flushParagraph()
            val marker = if (bulletMatch.groupValues[1] in setOf("-", "*", "+")) "•" else bulletMatch.groupValues[1]
            result.add(MarkdownBlock.BulletItem(marker, bulletMatch.groupValues[2].trim()))
            index++
            continue
        }

        if (paragraphBuffer.isNotEmpty()) {
            paragraphBuffer.append('\n')
        }
        paragraphBuffer.append(line)
        index++
    }

    flushParagraph()
    return result
}

private fun parseInlineMarkdown(
    text: String,
    inlineCodeBg: Color,
    linkColor: Color
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in INLINE_TOKEN_REGEX.findAll(text)) {
        val start = match.range.first
        if (start > cursor) {
            append(text.substring(cursor, start))
        }
        val token = match.value
        when {
            token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                val inner = token.substring(1, token.length - 1)
                val spanStart = length
                append(inner)
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = inlineCodeBg,
                        fontSize = 13.sp
                    ),
                    spanStart,
                    length
                )
            }

            token.startsWith("**") && token.endsWith("**") && token.length >= 4 -> {
                val inner = token.substring(2, token.length - 2)
                val spanStart = length
                append(inner)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), spanStart, length)
            }

            token.startsWith("~~") && token.endsWith("~~") && token.length >= 4 -> {
                val inner = token.substring(2, token.length - 2)
                val spanStart = length
                append(inner)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), spanStart, length)
            }

            token.startsWith("*") && token.endsWith("*") && token.length >= 2 -> {
                val inner = token.substring(1, token.length - 1)
                val spanStart = length
                append(inner)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), spanStart, length)
            }

            token.startsWith("[") -> {
                val label = match.groupValues[1]
                val url = match.groupValues[2]
                val spanStart = length
                append(label)
                if (url.startsWith("https://", ignoreCase = true)) {
                    addStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        ),
                        spanStart,
                        length
                    )
                }
            }

            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

private val HEADING_REGEX = Regex("^(#{1,6})\\s+(.+)$")
private val BULLET_REGEX = Regex("^([-*+]|\\d+\\.)\\s+(.+)$")
private val INLINE_TOKEN_REGEX = Regex(
    """`[^`]+`|\*\*[^*]+\*\*|~~[^~]+~~|\*[^*\n]+\*|\[([^]]+)]\((https://[^)\s]+)\)"""
)
