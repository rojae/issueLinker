package com.github.rojae.issuelinker.toolwindow

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerNotifier
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.github.rojae.issuelinker.services.RecentIssuesService
import com.github.rojae.issuelinker.settings.IssueLinkerSettings
import com.github.rojae.issuelinker.util.UrlBuilderUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.ScrollPaneConstants

class IssueLinkerToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = IssueLinkerToolWindowPanel(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
        updateStripeTitle(project, toolWindow)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    companion object {
        fun updateStripeTitle(project: Project, toolWindow: ToolWindow) {
            val service = project.getService(IssueLinkerService::class.java)
            toolWindow.stripeTitle =
                service?.issueKey ?: IssueLinkerBundle.message("nokey.widget.text")
        }
    }
}

class IssueLinkerToolWindowPanel(private val project: Project, private val toolWindow: ToolWindow) :
    JBPanel<JBPanel<*>>(BorderLayout()), Disposable {

    private val contentPanel = JPanel()
    private val connection = project.messageBus.connect(this)

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        buildUI()
        refreshContent()

        connection.subscribe(
            IssueLinkerNotifier.TOPIC,
            object : IssueLinkerNotifier {
                override fun issueKeyChanged(issueKey: String?, branchName: String?) {
                    ApplicationManager.getApplication().invokeLater { refreshContent() }
                }
            },
        )
        Disposer.register(toolWindow.disposable, this)
    }

    override fun dispose() {
        connection.disconnect()
    }

    private fun buildUI() {
        contentPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        // Toolbar (Feature 5)
        add(createToolbar(), BorderLayout.NORTH)

        val scrollPane =
            JBScrollPane(contentPanel).apply {
                border = JBUI.Borders.empty()
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                isOpaque = false
                viewport.isOpaque = false
            }
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun createToolbar(): JComponent {
        val actionGroup =
            DefaultActionGroup().apply {
                add(
                    object :
                        AnAction(
                            IssueLinkerBundle.message("toolbar.refresh.text"),
                            IssueLinkerBundle.message("toolbar.refresh.description"),
                            AllIcons.Actions.Refresh,
                        ) {
                        override fun actionPerformed(e: AnActionEvent) {
                            refreshContent()
                        }

                        override fun getActionUpdateThread() = ActionUpdateThread.BGT
                    }
                )
                add(
                    object :
                        AnAction(
                            IssueLinkerBundle.message("toolbar.openBrowser.text"),
                            IssueLinkerBundle.message("toolbar.openBrowser.description"),
                            AllIcons.General.Web,
                        ) {
                        override fun actionPerformed(e: AnActionEvent) {
                            executeAction("IssueLinker.OpenIssue")
                        }

                        override fun update(e: AnActionEvent) {
                            val service = project.getService(IssueLinkerService::class.java)
                            e.presentation.isEnabled = service?.issueKey != null
                        }

                        override fun getActionUpdateThread() = ActionUpdateThread.BGT
                    }
                )
                add(
                    object :
                        AnAction(
                            IssueLinkerBundle.message("toolbar.settings.text"),
                            IssueLinkerBundle.message("toolbar.settings.description"),
                            AllIcons.General.Settings,
                        ) {
                        override fun actionPerformed(e: AnActionEvent) {
                            ShowSettingsUtil.getInstance()
                                .showSettingsDialog(
                                    project,
                                    IssueLinkerBundle.message("settings.displayName"),
                                )
                        }

                        override fun getActionUpdateThread() = ActionUpdateThread.BGT
                    }
                )
            }
        val toolbar =
            ActionManager.getInstance()
                .createActionToolbar("IssueLinkerToolWindow", actionGroup, true)
        toolbar.targetComponent = this
        return toolbar.component
    }

    private fun refreshContent() {
        contentPanel.removeAll()

        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service.issueKey
        val branch = service.branchName
        val hasIssue = issueKey != null && service.buildIssueUrl() != null

        contentPanel.add(Box.createVerticalStrut(12))

        // Issue Card
        contentPanel.add(
            createCard(
                title = issueKey ?: IssueLinkerBundle.message("nokey.toolwindow.title"),
                subtitle =
                    if (hasIssue) IssueLinkerBundle.message("toolwindow.issue.subtitle")
                    else IssueLinkerBundle.message("nokey.toolwindow.subtitle"),
                hasContent = hasIssue,
                chips =
                    listOf(
                        ChipAction(
                            IssueLinkerBundle.message("toolwindow.chip.key"),
                            "IssueLinker.CopyIssueKey",
                            hasIssue,
                            IssueLinkerBundle.message("toolwindow.chip.copyKey"),
                        ),
                        ChipAction(
                            IssueLinkerBundle.message("toolwindow.chip.link"),
                            "IssueLinker.CopyIssueLink",
                            hasIssue,
                            IssueLinkerBundle.message("toolwindow.chip.copyIssueLink"),
                        ),
                        ChipAction(
                            IssueLinkerBundle.message("toolwindow.chip.markdown"),
                            "IssueLinker.CopyAsMarkdown",
                            hasIssue,
                            IssueLinkerBundle.message("toolwindow.chip.copyMarkdown"),
                        ),
                    ),
                primaryAction =
                    PrimaryAction(
                        IssueLinkerBundle.message("toolwindow.openBrowser"),
                        "IssueLinker.OpenIssue",
                        hasIssue,
                    ),
            )
        )

        contentPanel.add(Box.createVerticalStrut(10))

        // Branch Card
        val hasBranch = branch != null
        contentPanel.add(
            createCard(
                title = branch ?: IssueLinkerBundle.message("toolwindow.branch.title"),
                subtitle =
                    if (hasBranch) IssueLinkerBundle.message("toolwindow.branch.subtitle")
                    else IssueLinkerBundle.message("toolwindow.branch.noRepo"),
                hasContent = hasBranch,
                chips =
                    listOf(
                        ChipAction(
                            IssueLinkerBundle.message("toolwindow.chip.link"),
                            "IssueLinker.CopyBranchLink",
                            hasBranch,
                            IssueLinkerBundle.message("toolwindow.chip.copyBranchUrl"),
                        ),
                        ChipAction(
                            IssueLinkerBundle.message("toolwindow.chip.markdown"),
                            "IssueLinker.CopyBranchAsMarkdown",
                            hasBranch,
                            IssueLinkerBundle.message("toolwindow.chip.copyBranchMarkdown"),
                        ),
                    ),
                primaryAction =
                    PrimaryAction(
                        IssueLinkerBundle.message("toolwindow.openBrowser"),
                        "IssueLinker.OpenBranchLink",
                        hasBranch,
                    ),
            )
        )

        contentPanel.add(Box.createVerticalStrut(10))

        // Recent Issues Section
        contentPanel.add(createRecentIssuesSection())

        contentPanel.add(Box.createVerticalGlue())

        // Update tool window tab title (Feature 8)
        toolWindow.stripeTitle =
            issueKey ?: branch ?: IssueLinkerBundle.message("nokey.widget.text")
        toolWindow.contentManager.contents.firstOrNull()?.displayName = issueKey ?: ""

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private data class ChipAction(
        val label: String,
        val actionId: String,
        val enabled: Boolean,
        val description: String,
    )

    private data class PrimaryAction(val label: String, val actionId: String, val enabled: Boolean)

    private fun createCard(
        title: String,
        subtitle: String,
        hasContent: Boolean,
        chips: List<ChipAction>,
        primaryAction: PrimaryAction,
    ): JPanel {
        return object : JPanel() {
                init {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    isOpaque = false
                    alignmentX = Component.LEFT_ALIGNMENT
                    border = JBUI.Borders.empty(0, 14, 0, 14)
                }

                override fun paintComponent(g: Graphics) {
                    val g2 = g as Graphics2D
                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON,
                    )
                    g2.color = JBUI.CurrentTheme.Editor.BORDER_COLOR
                    g2.drawRoundRect(0, 0, width - 1, height - 1, 12, 12)
                    super.paintComponent(g)
                }
            }
            .apply {
                val innerPanel =
                    JPanel().apply {
                        layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        isOpaque = false
                        border = JBUI.Borders.empty(14, 16, 14, 16)

                        // Title
                        add(
                            JBLabel(title).apply {
                                font = font.deriveFont(Font.BOLD, 16f)
                                foreground =
                                    if (hasContent) JBUI.CurrentTheme.Label.foreground()
                                    else JBUI.CurrentTheme.Label.disabledForeground()
                                alignmentX = Component.LEFT_ALIGNMENT
                            }
                        )

                        add(Box.createVerticalStrut(2))

                        // Subtitle
                        add(
                            JBLabel(subtitle).apply {
                                foreground = JBUI.CurrentTheme.Label.disabledForeground()
                                font = font.deriveFont(11f)
                                alignmentX = Component.LEFT_ALIGNMENT
                            }
                        )

                        if (!hasContent) {
                            add(Box.createVerticalStrut(8))
                            add(
                                JBLabel(IssueLinkerBundle.message("nokey.toolwindow.configure"))
                                    .apply {
                                        foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                                        font = font.deriveFont(12f)
                                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        alignmentX = Component.LEFT_ALIGNMENT
                                        addMouseListener(
                                            object : MouseAdapter() {
                                                override fun mouseClicked(e: MouseEvent) {
                                                    ShowSettingsUtil.getInstance()
                                                        .showSettingsDialog(
                                                            project,
                                                            IssueLinkerBundle.message(
                                                                "settings.displayName"
                                                            ),
                                                        )
                                                }
                                            }
                                        )
                                    }
                            )
                        }

                        add(Box.createVerticalStrut(12))

                        // Chip row
                        val chipRow =
                            JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
                                isOpaque = false
                                alignmentX = Component.LEFT_ALIGNMENT
                                maximumSize = Dimension(Int.MAX_VALUE, 36)
                                for (chip in chips) {
                                    add(
                                        createChip(
                                            chip.label,
                                            chip.actionId,
                                            chip.enabled,
                                            chip.description,
                                        )
                                    )
                                }
                            }
                        add(chipRow)

                        add(Box.createVerticalStrut(8))

                        // Primary action button
                        add(
                            createPrimaryButton(
                                primaryAction.label,
                                primaryAction.actionId,
                                primaryAction.enabled,
                            )
                        )
                    }
                add(innerPanel)
            }
    }

    private fun createChip(
        label: String,
        actionId: String,
        enabled: Boolean,
        description: String,
    ): JPanel {
        val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId)

        return object : JPanel(BorderLayout()) {
            private var hovered = false

            init {
                isOpaque = false
                cursor =
                    if (enabled) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    else Cursor.getDefaultCursor()

                val chipLabel =
                    JBLabel(label).apply {
                        font = font.deriveFont(12f)
                        foreground =
                            if (enabled) JBUI.CurrentTheme.Label.foreground()
                            else JBUI.CurrentTheme.Label.disabledForeground()
                        border = JBUI.Borders.empty(4, 10)
                    }
                add(chipLabel, BorderLayout.CENTER)

                toolTipText =
                    if (enabled && shortcutText.isNotEmpty()) "$description ($shortcutText)"
                    else description

                if (enabled) {
                    addMouseListener(
                        object : MouseAdapter() {
                            override fun mouseEntered(e: MouseEvent) {
                                hovered = true
                                repaint()
                            }

                            override fun mouseExited(e: MouseEvent) {
                                hovered = false
                                repaint()
                            }

                            override fun mouseClicked(e: MouseEvent) {
                                executeAction(actionId)
                            }
                        }
                    )
                }
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                if (hovered && enabled) {
                    g2.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
                } else {
                    g2.color =
                        JBUI.CurrentTheme.ActionButton.hoverBackground().let {
                            java.awt.Color(it.red, it.green, it.blue, 60)
                        }
                }
                g2.fillRoundRect(0, 0, width, height, 8, 8)
                super.paintComponent(g)
            }
        }
    }

    private fun createPrimaryButton(label: String, actionId: String, enabled: Boolean): JPanel {
        val shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId)
        val displayText = if (shortcutText.isNotEmpty()) "$label  $shortcutText" else label

        return object : JPanel(BorderLayout()) {
            private var hovered = false

            init {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
                maximumSize = Dimension(Int.MAX_VALUE, 34)
                preferredSize = Dimension(0, 34)
                cursor =
                    if (enabled) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    else Cursor.getDefaultCursor()

                val btnLabel =
                    JBLabel(displayText).apply {
                        font = font.deriveFont(Font.BOLD, 12f)
                        foreground =
                            if (enabled) JBUI.CurrentTheme.Link.Foreground.ENABLED
                            else JBUI.CurrentTheme.Label.disabledForeground()
                        horizontalAlignment = JBLabel.CENTER
                    }
                add(btnLabel, BorderLayout.CENTER)

                if (enabled) {
                    addMouseListener(
                        object : MouseAdapter() {
                            override fun mouseEntered(e: MouseEvent) {
                                hovered = true
                                repaint()
                            }

                            override fun mouseExited(e: MouseEvent) {
                                hovered = false
                                repaint()
                            }

                            override fun mouseClicked(e: MouseEvent) {
                                executeAction(actionId)
                            }
                        }
                    )
                }
            }

            override fun paintComponent(g: Graphics) {
                val g2 = g as Graphics2D
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                if (hovered && enabled) {
                    g2.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
                    g2.fillRoundRect(0, 0, width, height, 8, 8)
                }
                super.paintComponent(g)
            }
        }
    }

    private fun createRecentIssuesSection(): JPanel {
        val recentService = RecentIssuesService.getInstance(project)
        val recentIssues = recentService.getRecentIssues()

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(0, 14, 0, 14)

            // Section title
            add(
                JBLabel(IssueLinkerBundle.message("recent.title")).apply {
                    font = font.deriveFont(Font.BOLD, 13f)
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                    alignmentX = Component.LEFT_ALIGNMENT
                    border = JBUI.Borders.emptyBottom(6)
                }
            )

            if (recentIssues.isEmpty()) {
                add(
                    JBLabel(IssueLinkerBundle.message("recent.empty")).apply {
                        foreground = JBUI.CurrentTheme.Label.disabledForeground()
                        font = font.deriveFont(12f)
                        alignmentX = Component.LEFT_ALIGNMENT
                    }
                )
            } else {
                for (entry in recentIssues) {
                    add(createRecentIssueItem(entry.issueKey))
                    add(Box.createVerticalStrut(2))
                }
            }
        }
    }

    private fun createRecentIssueItem(issueKey: String): JComponent {
        return JBLabel(issueKey).apply {
            font = font.deriveFont(12f)
            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.empty(3, 4)
            toolTipText = IssueLinkerBundle.message("recent.open", issueKey)
            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        val label = e.component as JBLabel
                        label.isOpaque = true
                        label.background = JBUI.CurrentTheme.List.Hover.background(true)
                        label.repaint()
                    }

                    override fun mouseExited(e: MouseEvent) {
                        val label = e.component as JBLabel
                        label.isOpaque = false
                        label.repaint()
                    }

                    override fun mouseClicked(e: MouseEvent) {
                        if (e.button == MouseEvent.BUTTON1 && !e.isPopupTrigger) {
                            openRecentIssue(issueKey)
                        }
                    }

                    override fun mousePressed(e: MouseEvent) {
                        if (e.isPopupTrigger) {
                            showRecentIssueContextMenu(e.component, e.x, e.y, issueKey)
                        }
                    }

                    override fun mouseReleased(e: MouseEvent) {
                        if (e.isPopupTrigger) {
                            showRecentIssueContextMenu(e.component, e.x, e.y, issueKey)
                        }
                    }
                }
            )
        }
    }

    private fun showRecentIssueContextMenu(
        component: java.awt.Component,
        x: Int,
        y: Int,
        issueKey: String,
    ) {
        val settings = IssueLinkerSettings.getInstance()
        val url =
            UrlBuilderUtil.buildUrl(settings.hostUrl, settings.urlPathPattern, listOf(issueKey))

        val popup = JPopupMenu()

        popup.add(
            JMenuItem(IssueLinkerBundle.message("recent.context.openBrowser")).apply {
                addActionListener { openRecentIssue(issueKey) }
            }
        )

        popup.add(
            JMenuItem(IssueLinkerBundle.message("recent.context.copyKey")).apply {
                addActionListener {
                    CopyPasteManager.getInstance().setContents(StringSelection(issueKey))
                }
            }
        )

        if (url != null) {
            popup.add(
                JMenuItem(IssueLinkerBundle.message("recent.context.copyLink")).apply {
                    addActionListener {
                        CopyPasteManager.getInstance().setContents(StringSelection(url))
                    }
                }
            )

            popup.add(
                JMenuItem(IssueLinkerBundle.message("recent.context.copyMarkdown")).apply {
                    addActionListener {
                        CopyPasteManager.getInstance()
                            .setContents(StringSelection("[$issueKey]($url)"))
                    }
                }
            )
        }

        popup.addSeparator()

        popup.add(
            JMenuItem(IssueLinkerBundle.message("recent.context.remove")).apply {
                addActionListener {
                    RecentIssuesService.getInstance(project).removeIssueKey(issueKey)
                    refreshContent()
                }
            }
        )

        popup.show(component, x, y)
    }

    private fun openRecentIssue(issueKey: String) {
        IssueLinkerService.getInstance(project).openIssueByKey(issueKey)
    }

    fun refresh() {
        refreshContent()
    }

    private fun executeAction(actionId: String) {
        val action = ActionManager.getInstance().getAction(actionId) ?: return
        val dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        @Suppress("DEPRECATION")
        val event =
            AnActionEvent(
                null,
                dataContext,
                ActionPlaces.TOOLWINDOW_CONTENT,
                action.templatePresentation.clone(),
                ActionManager.getInstance(),
                0,
            )
        action.actionPerformed(event)
    }
}
