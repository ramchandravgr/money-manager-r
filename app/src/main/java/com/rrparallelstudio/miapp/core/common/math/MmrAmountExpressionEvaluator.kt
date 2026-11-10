package com.rrparallelstudio.miapp.core.common.math

import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/** Safe arithmetic for amount calculator expressions (+ − × ÷ % ^ and parentheses). */
object MmrAmountExpressionEvaluator {
    const val MAX_MAGNITUDE: Double = 1e12

    sealed interface Preview {
        data object Empty : Preview

        data class Valid(
            val value: Double,
        ) : Preview

        data class Invalid(
            val message: String,
        ) : Preview
    }

    sealed interface ApplyResult {
        data class Ok(
            val amountPlain: String,
        ) : ApplyResult

        data class InvalidExpression(
            val message: String,
        ) : ApplyResult

        data object TooLarge : ApplyResult
    }

    fun seedExpressionFromAmountField(amount: String): String =
        MmrMoneyFormat.stripGroupingSeparators(amount).filter { it.isDigit() || it == '.' }

    fun preview(
        expression: String,
        invalidMessage: String,
    ): Preview {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return Preview.Empty
        return when (val parsed = parse(trimmed)) {
            is ParseOutcome.Ok ->
                if (parsed.value.isFinite()) {
                    Preview.Valid(parsed.value)
                } else {
                    Preview.Invalid(invalidMessage)
                }
            is ParseOutcome.Error -> Preview.Invalid(invalidMessage)
        }
    }

    fun apply(
        expression: String,
        invalidMessage: String,
    ): ApplyResult {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return ApplyResult.InvalidExpression(invalidMessage)
        }
        return when (val parsed = parse(trimmed)) {
            is ParseOutcome.Error -> ApplyResult.InvalidExpression(invalidMessage)
            is ParseOutcome.Ok -> {
                val value = parsed.value
                if (!value.isFinite()) {
                    ApplyResult.InvalidExpression(invalidMessage)
                } else if (abs(value) >= MAX_MAGNITUDE) {
                    ApplyResult.TooLarge
                } else {
                    ApplyResult.Ok(formatAppliedAmount(value))
                }
            }
        }
    }

    fun formatPreviewResultLine(
        value: Double,
        locale: Locale = Locale.getDefault(),
    ): String {
        val formatter =
            NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 0
                maximumFractionDigits = 6
            }
        return "= ${formatter.format(value)}"
    }

    fun formatAppliedAmount(value: Double): String = MmrAmountMath.toMoney(value).stripTrailingZeros().toPlainString()

    private sealed interface ParseOutcome {
        data class Ok(
            val value: Double,
        ) : ParseOutcome

        data object Error : ParseOutcome
    }

    private enum class TokenKind {
        NUMBER,
        PLUS,
        MINUS,
        STAR,
        SLASH,
        PERCENT,
        CARET,
        LPAREN,
        RPAREN,
        EOF,
    }

    private data class Token(
        val kind: TokenKind,
        val number: Double = 0.0,
    )

    private class Lexer(
        private val input: String,
    ) {
        private var index = 0
        private var current = tokenizeNext()

        fun peek(): Token = current

        fun consume(): Token {
            val value = current
            current = tokenizeNext()
            return value
        }

        private fun tokenizeNext(): Token {
            while (index < input.length && input[index].isWhitespace()) {
                index++
            }
            if (index >= input.length) return Token(TokenKind.EOF)

            return when (val ch = input[index]) {
                '+' -> {
                    index++
                    Token(TokenKind.PLUS)
                }
                '-' -> {
                    index++
                    Token(TokenKind.MINUS)
                }
                '*' -> {
                    index++
                    Token(TokenKind.STAR)
                }
                '/' -> {
                    index++
                    Token(TokenKind.SLASH)
                }
                '%' -> {
                    index++
                    Token(TokenKind.PERCENT)
                }
                '^' -> {
                    index++
                    Token(TokenKind.CARET)
                }
                '(' -> {
                    index++
                    Token(TokenKind.LPAREN)
                }
                ')' -> {
                    index++
                    Token(TokenKind.RPAREN)
                }
                in '0'..'9', '.' -> readNumber()
                else -> Token(TokenKind.EOF)
            }
        }

        private fun readNumber(): Token {
            val start = index
            var dotCount = 0
            while (index < input.length) {
                when (val ch = input[index]) {
                    in '0'..'9' -> index++
                    '.' -> {
                        if (dotCount > 0) return Token(TokenKind.EOF)
                        dotCount++
                        index++
                    }
                    else -> break
                }
            }
            val raw = input.substring(start, index)
            if (raw == "." || raw.isEmpty()) return Token(TokenKind.EOF)
            val parsed = raw.toBigDecimalOrNull()?.toDouble()
            return if (parsed != null && parsed.isFinite()) {
                Token(TokenKind.NUMBER, parsed)
            } else {
                Token(TokenKind.EOF)
            }
        }
    }

    private class Parser(
        private val lexer: Lexer,
    ) {
        fun parseExpression(): ParseOutcome {
            val value = parseExpr() ?: return ParseOutcome.Error
            if (lexer.peek().kind != TokenKind.EOF) return ParseOutcome.Error
            return ParseOutcome.Ok(value)
        }

        private fun parseExpr(): Double? {
            var value = parseTerm() ?: return null
            while (true) {
                when (lexer.peek().kind) {
                    TokenKind.PLUS -> {
                        lexer.consume()
                        value = value + (parseTerm() ?: return null)
                    }
                    TokenKind.MINUS -> {
                        lexer.consume()
                        value = value - (parseTerm() ?: return null)
                    }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double? {
            var value = parsePower() ?: return null
            while (true) {
                when (lexer.peek().kind) {
                    TokenKind.STAR -> {
                        lexer.consume()
                        value = value * (parsePower() ?: return null)
                    }
                    TokenKind.SLASH -> {
                        lexer.consume()
                        val rhs = parsePower() ?: return null
                        if (rhs == 0.0) return null
                        value = value / rhs
                    }
                    TokenKind.PERCENT -> {
                        lexer.consume()
                        val rhs = parsePower() ?: return null
                        if (rhs == 0.0) return null
                        value = value % rhs
                    }
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double? {
            var value = parseUnary() ?: return null
            while (lexer.peek().kind == TokenKind.CARET) {
                lexer.consume()
                val rhs = parseUnary() ?: return null
                val result = value.pow(rhs)
                if (!result.isFinite()) return null
                value = result
            }
            return value
        }

        private fun parseUnary(): Double? =
            when (lexer.peek().kind) {
                TokenKind.MINUS -> {
                    lexer.consume()
                    val rhs = parseUnary() ?: return null
                    -rhs
                }
                TokenKind.PLUS -> {
                    lexer.consume()
                    parseUnary()
                }
                else -> parsePrimary()
            }

        private fun parsePrimary(): Double? {
            return when (lexer.peek().kind) {
                TokenKind.NUMBER -> {
                    val token = lexer.consume()
                    token.number
                }
                TokenKind.LPAREN -> {
                    lexer.consume()
                    val inner = parseExpr() ?: return null
                    if (lexer.peek().kind != TokenKind.RPAREN) return null
                    lexer.consume()
                    inner
                }
                else -> null
            }
        }
    }

    private fun parse(expression: String): ParseOutcome {
        if (!isAllowedCharacterSet(expression)) return ParseOutcome.Error
        return Parser(Lexer(expression)).parseExpression()
    }

    private fun isAllowedCharacterSet(expression: String): Boolean {
        for (ch in expression) {
            if (ch.isWhitespace()) continue
            val allowed =
                ch.isDigit() ||
                    ch == '.' ||
                    ch == '+' ||
                    ch == '-' ||
                    ch == '*' ||
                    ch == '/' ||
                    ch == '%' ||
                    ch == '^' ||
                    ch == '(' ||
                    ch == ')'
            if (!allowed) return false
        }
        return true
    }
}
