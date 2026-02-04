package com.github.rojae.issuelinker.settings

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class IssueLinkerConfigurable : Configurable {
    private var panel: JPanel? = null
    private val hostUrlField = JBTextField()
    private val urlPathPatternField = JBTextField()
    private val branchParsingRegexField = JBTextField()
    private val useInternalBrowserCheckbox =
        JBCheckBox(IssueLinkerBundle.message("settings.useInternalBrowser.label"))

    override fun getDisplayName(): String = "IssueLinker"

    override fun createComponent(): JComponent? {
        panel =
            FormBuilder.createFormBuilder()
                .addLabeledComponent(
                    IssueLinkerBundle.message("settings.hostUrl.label"),
                    hostUrlField,
                    true,
                )
                .addLabeledComponent(
                    IssueLinkerBundle.message("settings.urlPathPattern.label"),
                    urlPathPatternField,
                    true,
                )
                .addLabeledComponent(
                    IssueLinkerBundle.message("settings.branchParsingRegex.label"),
                    branchParsingRegexField,
                    true,
                )
                .addSeparator()
                .addComponent(useInternalBrowserCheckbox)
                .addComponentFillVertically(JPanel(), 0)
                .panel
        return panel
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
    }

    override fun disposeUIResources() {
        panel = null
    }

    private fun isValidRegex(regex: String): Boolean {
        return try {
            Regex(regex)
            true
        } catch (e: Exception) {
            false
        }
    }
}
