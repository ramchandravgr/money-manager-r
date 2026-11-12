package com.rrparallelstudio.miapp.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class MmrRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "mmr"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(MmrRawFontSize(config)),
        )
}
