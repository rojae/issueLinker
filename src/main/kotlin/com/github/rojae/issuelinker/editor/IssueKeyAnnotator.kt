package com.github.rojae.issuelinker.editor

import com.github.rojae.issuelinker.settings.IssueLinkerSettings
import com.github.rojae.issuelinker.util.UrlBuilderUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPlainText
import com.intellij.ui.JBColor
import java.awt.Font
import java.util.regex.PatternSyntaxException

class IssueKeyAnnotator : Annotator {

    private var cachedRegexStr: String? = null
    private var cachedRegex: Regex? = null

    private fun getCompiledRegex(regexStr: String): Regex? {
        if (regexStr != cachedRegexStr) {
            cachedRegexStr = regexStr
            cachedRegex =
                try {
                    Regex(regexStr)
                } catch (_: PatternSyntaxException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
        }
        return cachedRegex
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Only process leaf elements to avoid duplicate annotations
        if (element.children.isNotEmpty() && element !is PsiPlainText) {
            return
        }

        val settings = IssueLinkerSettings.getInstance()
        val regexStr = settings.branchParsingRegex
        if (regexStr.isBlank()) return

        val regex = getCompiledRegex(regexStr) ?: return

        val text = element.text ?: return
        val startOffset = element.textRange.startOffset

        regex.findAll(text).forEach { matchResult ->
            val issueKey =
                if (matchResult.groupValues.size > 1) {
                    matchResult.groupValues[1]
                } else {
                    matchResult.value
                }

            // Calculate the range of the issue key within the match
            val keyStart =
                matchResult.range.first + (matchResult.value.indexOf(issueKey).coerceAtLeast(0))
            val keyEnd = keyStart + issueKey.length
            val range = TextRange(startOffset + keyStart, startOffset + keyEnd)

            // Build the URL for this issue key
            val capturedGroups = matchResult.groupValues.drop(1)
            val url =
                UrlBuilderUtil.buildUrl(settings.hostUrl, settings.urlPathPattern, capturedGroups)

            val linkColor = JBColor(0x2470B3, 0x589DF6)
            val attributes =
                TextAttributes().apply {
                    foregroundColor = linkColor
                    fontType = Font.PLAIN
                    effectType = EffectType.LINE_UNDERSCORE
                    effectColor = linkColor
                }

            val message = if (url != null) "Open $issueKey" else issueKey

            holder
                .newAnnotation(HighlightSeverity.INFORMATION, message)
                .range(range)
                .enforcedTextAttributes(attributes)
                .also { builder ->
                    if (url != null) {
                        builder.withFix(OpenIssueIntentionAction(issueKey, url))
                    }
                }
                .create()
        }
    }
}
