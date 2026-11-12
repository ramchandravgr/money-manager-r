package com.rrparallelstudio.miapp.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Bans raw `fontSize = N.sp` on Compose text primitives. Use [com.rrparallelstudio.miapp.ui.theme.MmrStaticTextStyles].
 */
class MmrRawFontSize(config: Config) : Rule(config) {

    override val issue =
        Issue(
            id = "MmrRawFontSize",
            severity = Severity.Style,
            description = "Use MmrStaticTextStyles factories instead of raw fontSize = …sp",
            debt = Debt.FIVE_MINS,
        )

    private val allowlistedPathFragments =
        listOf(
            "ui/theme/Type.kt",
            "ui/theme/MmrStaticTextStyles.kt",
            "ui/components/MmrSettingsScreenTemplate.kt",
            "ads/MmrAdSlot.kt",
            "widget/",
            "ui/components/MmrSnapWheelColumn.kt",
            "ui/components/MmrAmountCalculatorSheet.kt",
            "navigation/MmrMainNavigationScaffold.kt",
            "navigation/MmrMainNavItem.kt",
        )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (isAllowlisted(expression)) return
        val callee = expression.calleeExpression?.text?.substringAfterLast('.').orEmpty()
        if (callee !in COMPOSE_TEXT_CALLEES) return
        expression.valueArguments.forEach { argument ->
            checkFontSizeArgument(argument)
        }
    }

    private fun checkFontSizeArgument(argument: KtValueArgument) {
        val name = argument.getArgumentName()?.asName?.asString() ?: return
        if (name != "fontSize") return
        val expr = argument.getArgumentExpression() ?: return
        if (isRawSpSize(expr)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(argument),
                    "Replace raw fontSize with a MmrStaticTextStyles factory (style = mmr…Style(color)).",
                ),
            )
        }
    }

    private fun isAllowlisted(expression: KtCallExpression): Boolean {
        val virtualFile = expression.containingKtFile.virtualFile?.path?.replace('\\', '/') ?: return false
        return allowlistedPathFragments.any { fragment -> virtualFile.contains(fragment) }
    }

    private fun isRawSpSize(expression: KtExpression): Boolean {
        val text = expression.text.trim()
        if (text.matches(RAW_SP_LITERAL)) return true
        if (expression is KtDotQualifiedExpression) {
            val receiver = expression.receiverExpression.text.trim()
            val selector = expression.selectorExpression?.text?.trim().orEmpty()
            if (selector == "sp" && receiver.matches(RAW_NUMBER)) return true
        }
        if (expression is KtCallExpression && expression.text.endsWith(".sp")) {
            val callee = expression.calleeExpression?.text
            if (callee == "sp") return true
        }
        return false
    }

    private companion object {
        private val RAW_SP_LITERAL = Regex("^\\d+\\.sp$")
        private val RAW_NUMBER = Regex("^\\d+$")
        private val COMPOSE_TEXT_CALLEES =
            setOf(
                "Text",
                "BasicTextField",
                "TextStyle",
                "SpanStyle",
                "ProvideTextStyle",
                "OutlinedTextField",
            )
    }
}
