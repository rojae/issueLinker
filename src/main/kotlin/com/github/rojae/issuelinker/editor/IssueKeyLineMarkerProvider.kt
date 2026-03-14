package com.github.rojae.issuelinker.editor

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.settings.IssueLinkerSettings
import com.github.rojae.issuelinker.util.UrlBuilderUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPlainText
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.PatternSyntaxException

class IssueKeyLineMarkerProvider : LineMarkerProvider {

    private data class CachedRegex(val source: String, val regex: Regex?)

    private val cached = AtomicReference<CachedRegex?>(null)

    private fun getCompiledRegex(regexStr: String): Regex? {
        val current = cached.get()
        if (current != null && current.source == regexStr) return current.regex
        val newRegex =
            try {
                Regex(regexStr)
            } catch (_: PatternSyntaxException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        cached.set(CachedRegex(regexStr, newRegex))
        return newRegex
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only process leaf elements to avoid duplicate markers
        if (element.children.isNotEmpty() && element !is PsiPlainText) {
            return null
        }

        val settings = IssueLinkerSettings.getInstance()
        val regexStr = settings.branchParsingRegex
        if (regexStr.isBlank()) return null

        val regex = getCompiledRegex(regexStr) ?: return null
        val text = element.text ?: return null

        val match = regex.find(text) ?: return null

        val issueKey = if (match.groupValues.size > 1) match.groupValues[1] else match.value

        val capturedGroups = match.groupValues.drop(1)
        val url = UrlBuilderUtil.buildUrl(settings.hostUrl, settings.urlPathPattern, capturedGroups)

        val tooltipText =
            if (url != null) {
                IssueLinkerBundle.message("linemarker.tooltip", issueKey, url)
            } else {
                issueKey
            }

        val keyStart = match.range.first + (match.value.indexOf(issueKey).coerceAtLeast(0))
        val range =
            TextRange(
                element.textRange.startOffset + keyStart,
                element.textRange.startOffset + keyStart + issueKey.length,
            )

        return LineMarkerInfo(
            element,
            range,
            AllIcons.General.Web,
            { _ -> tooltipText },
            { _, _ -> url?.let { BrowserUtil.browse(it) } },
            GutterIconRenderer.Alignment.LEFT,
            { tooltipText },
        )
    }
}
