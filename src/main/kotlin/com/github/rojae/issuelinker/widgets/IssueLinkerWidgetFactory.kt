package com.github.rojae.issuelinker.widgets

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerNotifier
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.github.rojae.issuelinker.services.RecentIssuesService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
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
import javax.swing.Icon
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
                    ApplicationManager.getApplication().invokeLater { updateLabel() }
                }
            },
        )

        // Click - show popup panel, hover effect
        label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        label.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    showPopupPanel(e)
                }

                override fun mouseEntered(e: MouseEvent) {
                    label.isOpaque = true
                    label.background = JBUI.CurrentTheme.ActionButton.hoverBackground()
                    label.repaint()
                }

                override fun mouseExited(e: MouseEvent) {
                    label.isOpaque = false
                    label.repaint()
                }
            }
        )
    }

    private fun showPopupPanel(e: MouseEvent) {
        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey ?: IssueLinkerBundle.message("nokey.popup.title")
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
        val issueKey = service.issueKey
        val hasIssue = issueKey != null && service.buildIssueUrl() != null
        var currentPopup: JBPopup? = null

        val panel =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBUI.Borders.empty(12)
                background = JBUI.CurrentTheme.Popup.BACKGROUND

                // Issue Key Display
                val issueLabel =
                    JBLabel(issueKey ?: IssueLinkerBundle.message("nokey.popup.title")).apply {
                        font = font.deriveFont(Font.BOLD, 16f)
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                add(issueLabel)

                val branch = service?.branchName
                val statusText =
                    when {
                        hasIssue && branch != null ->
                            IssueLinkerBundle.message("popup.branch", branch)
                        branch != null ->
                            IssueLinkerBundle.message("popup.branch", branch) +
                                "\n" +
                                IssueLinkerBundle.message("nokey.popup.hint")
                        else -> IssueLinkerBundle.message("nokey.popup.hint")
                    }
                val statusLabel =
                    JBLabel(statusText).apply {
                        foreground = JBUI.CurrentTheme.Label.disabledForeground()
                        font = font.deriveFont(11f)
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                add(statusLabel)

                add(Box.createVerticalStrut(12))
                add(createSeparator())
                add(Box.createVerticalStrut(8))

                // Issue actions
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.copyIssueKey.text"),
                        "IssueLinker.CopyIssueKey",
                        hasIssue,
                        AllIcons.Actions.Copy,
                    ) {
                        currentPopup?.cancel()
                    }
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.copyIssueLink.text"),
                        "IssueLinker.CopyIssueLink",
                        hasIssue,
                        AllIcons.Ide.Link,
                    ) {
                        currentPopup?.cancel()
                    }
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.copyAsMarkdown.text"),
                        "IssueLinker.CopyAsMarkdown",
                        hasIssue,
                        AllIcons.Actions.Copy,
                    ) {
                        currentPopup?.cancel()
                    }
                )

                add(Box.createVerticalStrut(8))
                add(createSeparator())
                add(Box.createVerticalStrut(8))

                // Browser & branch actions
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.openIssue.text"),
                        "IssueLinker.OpenIssue",
                        hasIssue,
                        AllIcons.General.Web,
                    ) {
                        currentPopup?.cancel()
                    }
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.copyBranchLink.text"),
                        "IssueLinker.CopyBranchLink",
                        true,
                        AllIcons.Ide.Link,
                    ) {
                        currentPopup?.cancel()
                    }
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.copyBranchAsMarkdown.text"),
                        "IssueLinker.CopyBranchAsMarkdown",
                        true,
                        AllIcons.Actions.Copy,
                    ) {
                        currentPopup?.cancel()
                    }
                )
                add(Box.createVerticalStrut(4))
                add(
                    createActionPopupButton(
                        IssueLinkerBundle.message("action.openBranchLink.text"),
                        "IssueLinker.OpenBranchLink",
                        true,
                        AllIcons.General.Web,
                    ) {
                        currentPopup?.cancel()
                    }
                )
                add(Box.createVerticalStrut(4))
                add(
                    createPopupButton(
                        IssueLinkerBundle.message("action.openToolWindow.text"),
                        true,
                        AllIcons.Actions.Preview,
                    ) {
                        currentPopup?.cancel()
                        executeAction("IssueLinker.OpenToolWindow")
                    }
                )

                // Recent Issues section
                val recentService = RecentIssuesService.getInstance(project)
                val recentIssues = recentService.getRecentIssues()
                if (recentIssues.isNotEmpty()) {
                    add(Box.createVerticalStrut(8))
                    add(createSeparator())
                    add(Box.createVerticalStrut(4))
                    add(
                        JBLabel(IssueLinkerBundle.message("recent.title")).apply {
                            font = font.deriveFont(Font.BOLD, 11f)
                            foreground = JBUI.CurrentTheme.Label.disabledForeground()
                            alignmentX = Component.LEFT_ALIGNMENT
                            border = JBUI.Borders.empty(2, 8)
                        }
                    )
                    add(Box.createVerticalStrut(2))
                    for (entry in recentIssues.take(5)) {
                        add(
                            createPopupButton("   ${entry.issueKey}", true) {
                                currentPopup?.cancel()
                                openRecentIssue(entry.issueKey)
                            }
                        )
                        add(Box.createVerticalStrut(2))
                    }
                }

                add(Box.createVerticalStrut(4))
                add(
                    createPopupButton(
                        IssueLinkerBundle.message("popup.settings"),
                        true,
                        AllIcons.General.Settings,
                    ) {
                        currentPopup?.cancel()
                        openSettings()
                    }
                )
            }

        panel.preferredSize = Dimension(280, panel.preferredSize.height)

        // Wire up popup reference after building the panel
        // The popup will be set by showPopupPanel before it's shown
        val wrapper =
            object : JPanel(java.awt.BorderLayout()) {
                init {
                    add(panel, java.awt.BorderLayout.CENTER)
                    isOpaque = false
                }

                fun setPopup(popup: JBPopup) {
                    currentPopup = popup
                }
            }
        return wrapper
    }

    private fun createSeparator(): JSeparator {
        return JSeparator().apply {
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 1)
        }
    }

    private fun createActionPopupButton(
        label: String,
        actionId: String,
        enabled: Boolean,
        icon: Icon? = null,
        onAction: (() -> Unit)? = null,
    ): JComponent {
        val shortcut = KeymapUtil.getFirstKeyboardShortcutText(actionId)
        val text = if (shortcut.isNotEmpty()) "$label  $shortcut" else label
        return createPopupButton(text, enabled, icon) {
            executeAction(actionId)
            onAction?.invoke()
        }
    }

    private fun createPopupButton(
        text: String,
        enabled: Boolean,
        icon: Icon? = null,
        action: () -> Unit,
    ): JComponent {
        return JBLabel(text).apply {
            if (icon != null) {
                this.icon = icon
                this.iconTextGap = 8
            }
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
                        }
                    }
                )
            }
        }
    }

    private fun executeAction(actionId: String) {
        val action = ActionManager.getInstance().getAction(actionId) ?: return
        val dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        @Suppress("DEPRECATION")
        val event =
            AnActionEvent(
                null,
                dataContext,
                ActionPlaces.POPUP,
                action.templatePresentation.clone(),
                ActionManager.getInstance(),
                0,
            )
        action.actionPerformed(event)
    }

    private fun openSettings() {
        ShowSettingsUtil.getInstance()
            .showSettingsDialog(project, IssueLinkerBundle.message("settings.displayName"))
    }

    private fun openRecentIssue(issueKey: String) {
        IssueLinkerService.getInstance(project).openIssueByKey(issueKey)
    }

    private fun buildTooltipText(): String {
        val shortcuts =
            listOf(
                "IssueLinker.OpenIssue" to IssueLinkerBundle.message("action.openIssue.text"),
                "IssueLinker.CopyIssueKey" to IssueLinkerBundle.message("action.copyIssueKey.text"),
                "IssueLinker.CopyIssueLink" to
                    IssueLinkerBundle.message("action.copyIssueLink.text"),
                "IssueLinker.CopyAsMarkdown" to
                    IssueLinkerBundle.message("action.copyAsMarkdown.text"),
                "IssueLinker.CopyBranchLink" to
                    IssueLinkerBundle.message("action.copyBranchLink.text"),
                "IssueLinker.CopyBranchAsMarkdown" to
                    IssueLinkerBundle.message("action.copyBranchAsMarkdown.text"),
                "IssueLinker.OpenBranchLink" to
                    IssueLinkerBundle.message("action.openBranchLink.text"),
                "IssueLinker.OpenToolWindow" to
                    IssueLinkerBundle.message("action.openToolWindow.text"),
            )
        val sb =
            StringBuilder(
                "<html><b>${IssueLinkerBundle.message("popup.title")}</b><br/>" +
                    "${IssueLinkerBundle.message("popup.clickToOpen")}<br/><br/>"
            )
        sb.append("<b>${IssueLinkerBundle.message("popup.shortcuts")}:</b><br/>")
        for ((actionId, desc) in shortcuts) {
            val key = KeymapUtil.getFirstKeyboardShortcutText(actionId)
            if (key.isNotEmpty()) {
                sb.append("$key \u2014 $desc<br/>")
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
        if (issueKey != null) {
            label.text = issueKey
            label.foreground = JBUI.CurrentTheme.Label.foreground()
            label.toolTipText = buildTooltipText()
        } else {
            label.text = IssueLinkerBundle.message("nokey.widget.text")
            label.foreground = JBUI.CurrentTheme.Label.disabledForeground()
            label.toolTipText = IssueLinkerBundle.message("nokey.widget.tooltip")
        }
    }
}
