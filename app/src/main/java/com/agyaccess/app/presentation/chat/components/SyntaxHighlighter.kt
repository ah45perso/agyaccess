package com.agyaccess.app.presentation.chat.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Colorateur syntaxique léger et 100 % natif Compose (`AnnotatedString`)
 * conçu pour rester fluide même pendant le streaming token par token.
 */
object SyntaxHighlighter {

    private val ColorKeyword = Color(0xFFFF7B72)
    private val ColorString = Color(0xFFA5D6FF)
    private val ColorComment = Color(0xFF8B949E)
    private val ColorNumber = Color(0xFF79C0FF)
    private val ColorType = Color(0xFFFFA657)
    private val ColorAnnotation = Color(0xFFD2A8FF)
    private val ColorDefault = Color(0xFFE6EDF3)

    private val commonKeywords = setOf(
        // Kotlin / Java / JS / TS / Rust / Go
        "fun", "val", "var", "class", "object", "interface", "sealed", "data", "enum",
        "override", "private", "public", "protected", "internal", "suspend", "inline",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
        "throw", "import", "package", "companion", "lateinit", "by", "in", "is", "as",
        "true", "false", "null", "this", "super", "const", "let", "function", "async",
        "await", "export", "default", "switch", "case", "break", "continue", "new",
        // Python
        "def", "lambda", "from", "elif", "pass", "yield", "with", "raise", "None", "True", "False", "and", "or", "not",
        // SQL
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "JOIN", "ON", "GROUP", "ORDER", "LIMIT",
        // Bash / Shell
        "fi", "then", "esac", "done", "echo", "exit", "export", "local"
    )

    private val tokenPattern = Regex(
        """(//[^\n]*|#[^\n]*|/\*[\s\S]*?\*/|""" + // Commentaires
            """"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|""" + // Chaînes
            """@[A-Za-z_][A-Za-z0-9_]*|""" + // Annotations
            """\b\d+(?:\.\d+)?(?:L|f|F)?\b|""" + // Nombres
            """\b[A-Za-z_][A-Za-z0-9_]*\b)""" // Identifiants / Mots-clés
    )

    fun highlight(code: String, language: String?): AnnotatedString = buildAnnotatedString {
        append(code)
        addStyle(SpanStyle(color = ColorDefault), 0, code.length)

        val isJson = language.equals("json", ignoreCase = true)

        for (match in tokenPattern.findAll(code)) {
            val token = match.value
            val start = match.range.first
            val end = match.range.last + 1

            val style = when {
                token.startsWith("//") || token.startsWith("/*") ||
                    (token.startsWith("#") && !isJson) ->
                    SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)

                token.startsWith("\"") || token.startsWith("'") ->
                    SpanStyle(color = ColorString)

                token.startsWith("@") ->
                    SpanStyle(color = ColorAnnotation)

                token.first().isDigit() ->
                    SpanStyle(color = ColorNumber)

                token in commonKeywords ->
                    SpanStyle(color = ColorKeyword, fontWeight = FontWeight.SemiBold)

                token.first().isUpperCase() && token.any { it.isLowerCase() } ->
                    SpanStyle(color = ColorType)

                else -> null
            }

            if (style != null) {
                addStyle(style, start, end)
            }
        }
    }
}
