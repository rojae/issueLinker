package com.github.rojae.issuelinker.widgets

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerNotifier
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class IssueLinkerWidgetFactory : StatusBarWidgetFactory {
    companion object {
        const val WIDGET_ID = "IssueLinkerWidget"
    }

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = IssueLinkerBundle.message("widget.displayName")

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = IssueLinkerWidget(project)
}

class IssueLinkerWidget(private val project: Project) : CustomStatusBarWidget, Disposable {

    private var statusBar: StatusBar? = null
    private val label = JBLabel()
    private val connection = project.messageBus.connect(this)

    init {
        label.border = JBUI.Borders.empty(0, 4)
        label.toolTipText = buildTooltipText()
        updateLabel()

        // Subscribe to issue key changes
        connection.subscribe(
            IssueLinkerNotifier.TOPIC,
            object : IssueLinkerNotifier {
                override fun issueKeyChanged(issueKey: String?, branchName: String?) {
                    updateLabel()
                }
            },
        )

        // Click - show popup panel
        label.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    showPopupPanel(e)
                }
            }
        )
    }

    private fun showPopupPanel(e: MouseEvent) {
        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey ?: "No Issue"
        val popupContent = createPopupContent()
        val popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupContent, null)
                .setTitle(issueKey)
                .setMovable(false)
                .setResizable(false)
                .setRequestFocus(true)
                .createPopup()

        // Show above the widget
        val point = RelativePoint(e.component, Point(0, -popupContent.preferredSize.height - 5))
        popup.show(point)
    }

    private fun createPopupContent(): JComponent {
        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey
        val hasIssue = issueKey != null && service?.buildIssueUrl() != null

        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBUI.Borders.empty(12)
                background = JBUI.CurrentTheme.Popup.BACKGROUND

                // Issue Key Display
                val issueLabel =
                    JBLabel(issueKey ?: "No Issue").apply {
                        font = font.deriveFont(Font.BOLD, 16f)
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                add(issueLabel)

                val branch = service?.branchName
                val statusText =
                    when {
                        hasIssue && branch != null -> "Branch: $branch"
                        branch != null -> "Branch: $branch (no issue key detected)"
                        else -> "Switch to a branch with issue key"
                    }
                val statusLabel =
                    JBLabel(statusText).apply {
                        foreground = JBUI.CurrentTheme.Label.disabledForeground()
                        font = font.deriveFont(11f)
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                add(statusLabel)

                add(Box.createVerticalStrut(12))
                add(
                    JSeparator().apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                        maximumSize = Dimension(Int.MAX_VALUE, 1)
                    }
                )
                add(Box.createVerticalStrut(8))

                // Action buttons with dynamic shortcut hints
                add(
                    createActionPopupButton(
                        "📋  Copy Issue Key",
                        "IssueLinker.CopyIssueKey",
                        hasIssue,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        "🔗  Copy Issue Link",
                        "IssueLinker.CopyIssueLink",
                        hasIssue,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        "📝  Copy as Markdown",
                        "IssueLinker.CopyAsMarkdown",
                        hasIssue,
                    )
                )

                add(Box.createVerticalStrut(8))
                add(
                    JSeparator().apply {
                        alignmentX = Component.LEFT_ALIGNMENT
                        maximumSize = Dimension(Int.MAX_VALUE, 1)
                    }
                )
                add(Box.createVerticalStrut(8))

                add(
                    createActionPopupButton(
                        "🌐  Open in Browser",
                        "IssueLinker.OpenIssue",
                        hasIssue,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        "🔗  Copy Branch Link",
                        "IssueLinker.CopyBranchLink",
                        true,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        "📝  Copy Branch as Markdown",
                        "IssueLinker.CopyBranchAsMarkdown",
                        true,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        "🌐  Open Branch Link",
                        "IssueLinker.OpenBranchLink",
                        true,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        "📂  Open Tool Window",
                        "IssueLinker.OpenToolWindow",
                        true,
                    )
                )
                add(Box.createVerticalStrut(4))
                add(createPopupButton("⚙️  Settings", true) { openSettings() })
            }

        panel.preferredSize = Dimension(280, panel.preferredSize.height)
        return panel
    }

    private fun createActionPopupButton(
        label: String,
        actionId: String,
        enabled: Boolean,
    ): JComponent {
        val shortcut = KeymapUtil.getFirstKeyboardShortcutText(actionId)
        val text = if (shortcut.isNotEmpty()) "$label  $shortcut" else label
        return createPopupButton(text, enabled) { executeAction(actionId) }
    }

    private fun createPopupButton(text: String, enabled: Boolean, action: () -> Unit): JComponent {
        return JBLabel(text).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(6, 8)
            isEnabled = enabled
            cursor =
                if (enabled) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                else Cursor.getDefaultCursor()
            foreground =
                if (enabled) JBUI.CurrentTheme.Label.foreground()
                else JBUI.CurrentTheme.Label.disabledForeground()

            if (enabled) {
                addMouseListener(
                    object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent) {
                            background = JBUI.CurrentTheme.List.Hover.background(true)
                            isOpaque = true
                            repaint()
                        }

                        override fun mouseExited(e: MouseEvent) {
                            isOpaque = false
                            repaint()
                        }

                        override fun mouseClicked(e: MouseEvent) {
                            action()
                            // Close popup after action
                            var parent = e.component.parent
                            while (parent != null) {
                                if (parent is JPanel) {
                                    val popup =
                                        JBPopupFactory.getInstance().getChildFocusedPopup(parent)
                                    popup?.cancel()
                                    break
                                }
                                parent = parent.parent
                            }
                        }
                    }
                )
            }
        }
    }

    private fun executeAction(actionId: String) {
        val action = ActionManager.getInstance().getAction(actionId) ?: return
        val dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.POPUP, dataContext)
        action.actionPerformed(event)
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "IssueLinker")
    }

    private fun buildTooltipText(): String {
        val shortcuts =
            listOf(
                "IssueLinker.OpenIssue" to "Open Issue in Browser",
                "IssueLinker.CopyIssueKey" to "Copy Issue Key",
                "IssueLinker.CopyIssueLink" to "Copy Issue Link",
                "IssueLinker.CopyAsMarkdown" to "Copy as Markdown",
                "IssueLinker.CopyBranchLink" to "Copy Branch Link",
                "IssueLinker.CopyBranchAsMarkdown" to "Copy Branch as Markdown",
                "IssueLinker.OpenBranchLink" to "Open Branch Link",
                "IssueLinker.OpenToolWindow" to "Open IssueLinker Panel",
            )
        val sb = StringBuilder("<html><b>IssueLinker</b><br/>Click to open panel<br/><br/>")
        sb.append("<b>Shortcuts:</b><br/>")
        for ((actionId, desc) in shortcuts) {
            val key = KeymapUtil.getFirstKeyboardShortcutText(actionId)
            if (key.isNotEmpty()) {
                sb.append("$key — $desc<br/>")
            }
        }
        sb.append("</html>")
        return sb.toString()
    }

    override fun ID(): String = IssueLinkerWidgetFactory.WIDGET_ID

    override fun getComponent(): JComponent = label

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        connection.disconnect()
        statusBar = null
    }

    fun updateLabel() {
        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey
        val branch = service?.branchName
        label.text = issueKey ?: branch ?: IssueLinkerBundle.message("widget.noIssue")
    }
}
