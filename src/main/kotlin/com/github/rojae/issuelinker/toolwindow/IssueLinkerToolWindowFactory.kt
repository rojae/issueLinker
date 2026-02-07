package com.github.rojae.issuelinker.toolwindow

import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSeparator

class IssueLinkerToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = IssueLinkerToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}

class IssueLinkerToolWindowPanel(private val project: Project) :
    JBPanel<JBPanel<*>>(BorderLayout()) {

    private val issueKeyLabel = JBLabel()
    private val statusLabel = JBLabel()

    init {
        border = JBUI.Borders.empty(12)
        buildUI()
        refresh()
    }

    private fun buildUI() {
        // Header
        val headerPanel =
            JPanel(BorderLayout()).apply {
                isOpaque = false
                val titleLabel =
                    JBLabel("IssueLinker").apply { font = font.deriveFont(Font.BOLD, 14f) }
                add(titleLabel, BorderLayout.WEST)

                val refreshButton = createLinkButton("↻ Refresh") { refresh() }
                add(refreshButton, BorderLayout.EAST)
            }
        add(headerPanel, BorderLayout.NORTH)

        // Content
        val contentPanel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                isOpaque = false
                border = JBUI.Borders.emptyTop(16)

                // Issue Key Section
                add(createSectionLabel("Current Issue"))
                add(Box.createVerticalStrut(8))

                issueKeyLabel.apply {
                    font = font.deriveFont(Font.BOLD, 18f)
                    alignmentX = Component.LEFT_ALIGNMENT
                }
                add(issueKeyLabel)

                statusLabel.apply {
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                    alignmentX = Component.LEFT_ALIGNMENT
                }
                add(statusLabel)

                add(Box.createVerticalStrut(20))
                add(JSeparator().apply { alignmentX = Component.LEFT_ALIGNMENT })
                add(Box.createVerticalStrut(16))

                // Copy Actions
                add(createSectionLabel("Copy"))
                add(Box.createVerticalStrut(8))

                val copyButtonsPanel =
                    JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                        isOpaque = false
                        alignmentX = Component.LEFT_ALIGNMENT
                        add(createActionButton("Issue Key") { copyIssueKey() })
                        add(createActionButton("Issue Link") { copyIssueLink() })
                        add(createActionButton("Markdown") { copyAsMarkdown() })
                    }
                add(copyButtonsPanel)

                add(Box.createVerticalStrut(20))
                add(JSeparator().apply { alignmentX = Component.LEFT_ALIGNMENT })
                add(Box.createVerticalStrut(16))

                // Actions
                add(createSectionLabel("Actions"))
                add(Box.createVerticalStrut(8))

                val actionButtonsPanel =
                    JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                        isOpaque = false
                        alignmentX = Component.LEFT_ALIGNMENT
                        add(createActionButton("Open in Browser") { openInBrowser() })
                        add(createActionButton("Settings") { openSettings() })
                    }
                add(actionButtonsPanel)
            }
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun createSectionLabel(text: String): JBLabel {
        return JBLabel(text).apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            font = font.deriveFont(11f)
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    private fun createActionButton(text: String, action: () -> Unit): JButton {
        return JButton(text).apply {
            isFocusPainted = false
            addActionListener { action() }
        }
    }

    private fun createLinkButton(text: String, action: () -> Unit): JBLabel {
        return JBLabel(text).apply {
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        action()
                    }
                }
            )
        }
    }

    fun refresh() {
        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey

        if (issueKey != null) {
            issueKeyLabel.text = issueKey
            statusLabel.text = "Detected from current branch"
        } else {
            issueKeyLabel.text = "No Issue"
            statusLabel.text = "Switch to a branch with an issue key"
        }
    }

    private fun copyIssueKey() {
        val service = project.getService(IssueLinkerService::class.java)
        service?.issueKey?.let { CopyPasteManager.getInstance().setContents(StringSelection(it)) }
    }

    private fun copyIssueLink() {
        val service = project.getService(IssueLinkerService::class.java)
        service?.buildIssueUrl()?.let {
            CopyPasteManager.getInstance().setContents(StringSelection(it))
        }
    }

    private fun copyAsMarkdown() {
        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey
        val issueUrl = service?.buildIssueUrl()
        if (issueKey != null && issueUrl != null) {
            val markdown = "[$issueKey]($issueUrl)"
            CopyPasteManager.getInstance().setContents(StringSelection(markdown))
        }
    }

    private fun openInBrowser() {
        project.getService(IssueLinkerService::class.java)?.openIssueInBrowser()
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "IssueLinker")
    }
}
