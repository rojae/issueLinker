package com.github.rojae.issuelinker.settings

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.util.BranchParserUtil
import com.github.rojae.issuelinker.util.UrlBuilderUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class IssueLinkerConfigurable : Configurable {
    private var panel: JPanel? = null
    private val hostUrlField = JBTextField()
    private val urlPathPatternField = JBTextField()
    private val branchParsingRegexField = JBTextField()
    private val useInternalBrowserCheckbox =
        JBCheckBox(IssueLinkerBundle.message("settings.useInternalBrowser.label"))

    // Test section fields
    private val testBranchField = JBTextField()
    private val resultIssueKeyLabel = JBLabel("\u2014")
    private val resultUrlLabel = JBLabel("\u2014")
    private val regexStatusLabel = JBLabel("")

    override fun getDisplayName(): String = IssueLinkerBundle.message("settings.displayName")

    override fun createComponent(): JComponent? {
        // Add document listeners for real-time preview
        val updateListener =
            object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = updateTestResult()

                override fun removeUpdate(e: DocumentEvent) = updateTestResult()

                override fun changedUpdate(e: DocumentEvent) = updateTestResult()
            }

        testBranchField.document.addDocumentListener(updateListener)
        hostUrlField.document.addDocumentListener(updateListener)
        urlPathPatternField.document.addDocumentListener(updateListener)
        branchParsingRegexField.document.addDocumentListener(updateListener)

        testBranchField.emptyText.text = "e.g., feature/PROJ-123-add-login"
        hostUrlField.emptyText.text = "https://jira.company.com"
        urlPathPatternField.emptyText.text = "/browse/{0}"
        branchParsingRegexField.emptyText.text = "([A-Z][A-Z0-9]+-\\d+)"

        panel =
            FormBuilder.createFormBuilder()
                .addComponent(
                    TitledSeparator(IssueLinkerBundle.message("settings.section.connection"))
                )
                .addLabeledComponent(
                    IssueLinkerBundle.message("settings.hostUrl.label"),
                    hostUrlField,
                    true,
                )
                .addComponent(createHelpLabel(IssueLinkerBundle.message("settings.hostUrl.help")))
                .addLabeledComponent(
                    IssueLinkerBundle.message("settings.urlPathPattern.label"),
                    urlPathPatternField,
                    true,
                )
                .addComponent(
                    createHelpLabel(IssueLinkerBundle.message("settings.urlPathPattern.help"))
                )
                .addComponent(
                    TitledSeparator(IssueLinkerBundle.message("settings.section.parsing"))
                )
                .addLabeledComponent(
                    IssueLinkerBundle.message("settings.branchParsingRegex.label"),
                    branchParsingRegexField,
                    true,
                )
                .addComponent(
                    createHelpLabel(IssueLinkerBundle.message("settings.branchParsingRegex.help"))
                )
                .addComponent(regexStatusLabel)
                .addComponent(
                    TitledSeparator(IssueLinkerBundle.message("settings.section.browser"))
                )
                .addComponent(useInternalBrowserCheckbox)
                .addSeparator()
                .addComponent(createTestSection())
                .addComponentFillVertically(JPanel(), 0)
                .panel
        return panel
    }

    private fun createHelpLabel(text: String): JBLabel {
        return JBLabel(text).apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            font = font.deriveFont(font.size2D - 1f)
            border = JBUI.Borders.emptyLeft(20)
        }
    }

    private fun createTestSection(): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.emptyTop(8)

            val titleLabel =
                JBLabel(IssueLinkerBundle.message("settings.test.title")).apply {
                    font = font.deriveFont(java.awt.Font.BOLD)
                    border = JBUI.Borders.emptyBottom(8)
                }
            add(titleLabel)

            val inputPanel =
                JPanel(BorderLayout()).apply {
                    add(
                        JBLabel(IssueLinkerBundle.message("settings.test.branchName")),
                        BorderLayout.WEST,
                    )
                    add(testBranchField, BorderLayout.CENTER)
                    maximumSize =
                        java.awt.Dimension(Int.MAX_VALUE, testBranchField.preferredSize.height)
                }
            add(inputPanel)

            add(javax.swing.Box.createVerticalStrut(8))

            val resultPanel =
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    border =
                        JBUI.Borders.compound(
                            JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR, 1),
                            JBUI.Borders.empty(8),
                        )

                    val keyRow =
                        JPanel(BorderLayout()).apply {
                            isOpaque = false
                            add(
                                JBLabel(IssueLinkerBundle.message("settings.test.issueKey")),
                                BorderLayout.WEST,
                            )
                            add(resultIssueKeyLabel, BorderLayout.CENTER)
                        }
                    add(keyRow)

                    add(javax.swing.Box.createVerticalStrut(4))

                    val urlRow =
                        JPanel(BorderLayout()).apply {
                            isOpaque = false
                            add(
                                JBLabel(IssueLinkerBundle.message("settings.test.issueUrl")),
                                BorderLayout.WEST,
                            )
                            add(resultUrlLabel, BorderLayout.CENTER)
                        }
                    add(urlRow)
                }
            add(resultPanel)
        }
    }

    private fun updateTestResult() {
        val branchName = testBranchField.text
        val regex = branchParsingRegexField.text
        val hostUrl = hostUrlField.text
        val pathPattern = urlPathPatternField.text

        // Validate regex
        val validRegex = isValidRegex(regex)
        if (regex.isNotBlank() && !validRegex) {
            regexStatusLabel.text = IssueLinkerBundle.message("settings.validation.invalidRegex")
            regexStatusLabel.foreground = JBColor.RED
        } else {
            regexStatusLabel.text = ""
        }

        // Parse branch name
        if (branchName.isBlank() || regex.isBlank() || !validRegex) {
            resultIssueKeyLabel.text = "\u2014"
            resultIssueKeyLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()
            resultUrlLabel.text = "\u2014"
            resultUrlLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()
            return
        }

        val capturedGroups = BranchParserUtil.parseIssueKey(branchName, regex)
        val issueKey = capturedGroups?.firstOrNull()

        if (issueKey != null) {
            resultIssueKeyLabel.text = "  $issueKey"
            resultIssueKeyLabel.foreground = JBColor(0x007F00, 0x6AAF6A)

            val url = UrlBuilderUtil.buildUrl(hostUrl, pathPattern, capturedGroups)
            resultUrlLabel.text = if (url != null) "  $url" else "  \u2014"
            resultUrlLabel.foreground =
                if (url != null) JBUI.CurrentTheme.Label.foreground()
                else JBUI.CurrentTheme.Label.disabledForeground()
        } else {
            resultIssueKeyLabel.text = "  " + IssueLinkerBundle.message("settings.test.noMatch")
            resultIssueKeyLabel.foreground = JBColor.RED
            resultUrlLabel.text = "  \u2014"
            resultUrlLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()
        }
    }

    override fun isModified(): Boolean {
        val settings = IssueLinkerSettings.getInstance()
        return hostUrlField.text != settings.hostUrl ||
            urlPathPatternField.text != settings.urlPathPattern ||
            branchParsingRegexField.text != settings.branchParsingRegex ||
            useInternalBrowserCheckbox.isSelected != settings.useInternalBrowser
    }

    override fun apply() {
        val regex = branchParsingRegexField.text
        if (!isValidRegex(regex)) {
            throw ConfigurationException(
                IssueLinkerBundle.message("settings.validation.invalidRegex")
            )
        }

        val settings = IssueLinkerSettings.getInstance()
        settings.hostUrl = hostUrlField.text
        settings.urlPathPattern = urlPathPatternField.text
        settings.branchParsingRegex = branchParsingRegexField.text
        settings.useInternalBrowser = useInternalBrowserCheckbox.isSelected
    }

    override fun reset() {
        val settings = IssueLinkerSettings.getInstance()
        hostUrlField.text = settings.hostUrl
        urlPathPatternField.text = settings.urlPathPattern
        branchParsingRegexField.text = settings.branchParsingRegex
        useInternalBrowserCheckbox.isSelected = settings.useInternalBrowser
        updateTestResult()
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun isValidRegex(regex: String): Boolean {
        return try {
            Regex(regex)
            true
        } catch (_: Exception) {
            false
        }
    }
}
