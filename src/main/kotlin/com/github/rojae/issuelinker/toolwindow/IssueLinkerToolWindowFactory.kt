package com.github.rojae.issuelinker.toolwindow

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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
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
            toolWindow.stripeTitle = service?.issueKey ?: "No Issue"
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
                    refreshContent()
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

        val scrollPane =
            JBScrollPane(contentPanel).apply {
                border = JBUI.Borders.empty()
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
                isOpaque = false
                viewport.isOpaque = false
            }
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun refreshContent() {
        contentPanel.removeAll()

        val service = project.getService(IssueLinkerService::class.java)
        val issueKey = service?.issueKey
        val branch = service?.branchName
        val hasIssue = issueKey != null && service?.buildIssueUrl() != null

        contentPanel.add(Box.createVerticalStrut(12))

        // ── Issue Card ──
        contentPanel.add(
            createCard(
                title = issueKey ?: "No Issue",
                subtitle = if (hasIssue) "Detected from branch" else "No issue key detected",
                hasContent = hasIssue,
                chips =
                    listOf(
                        ChipAction("Key", "IssueLinker.CopyIssueKey", hasIssue),
                        ChipAction("Link", "IssueLinker.CopyIssueLink", hasIssue),
                        ChipAction("Markdown", "IssueLinker.CopyAsMarkdown", hasIssue),
                    ),
                primaryAction = PrimaryAction("Open in Browser", "IssueLinker.OpenIssue", hasIssue),
            )
        )

        contentPanel.add(Box.createVerticalStrut(10))

        // ── Branch Card ──
        val hasBranch = branch != null
        contentPanel.add(
            createCard(
                title = branch ?: "No Branch",
                subtitle = if (hasBranch) "Current Git branch" else "No Git repository found",
                hasContent = hasBranch,
                chips =
                    listOf(
                        ChipAction("Link", "IssueLinker.CopyBranchLink", hasBranch),
                        ChipAction("Markdown", "IssueLinker.CopyBranchAsMarkdown", hasBranch),
                    ),
                primaryAction =
                    PrimaryAction("Open in Browser", "IssueLinker.OpenBranchLink", hasBranch),
            )
        )

        contentPanel.add(Box.createVerticalStrut(16))

        // ── Footer ──
        contentPanel.add(createFooter())
        contentPanel.add(Box.createVerticalGlue())

        // Update tool window tab title
        toolWindow.stripeTitle = issueKey ?: branch ?: "No Issue"

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private data class ChipAction(val label: String, val actionId: String, val enabled: Boolean)

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

                        add(Box.createVerticalStrut(12))

                        // Chip row
                        val chipRow =
                            JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
                                isOpaque = false
                                alignmentX = Component.LEFT_ALIGNMENT
                                maximumSize = Dimension(Int.MAX_VALUE, 36)
                                for (chip in chips) {
                                    add(createChip(chip.label, chip.actionId, chip.enabled))
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

    private fun createChip(label: String, actionId: String, enabled: Boolean): JPanel {
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

                if (shortcutText.isNotEmpty() && enabled) {
                    toolTipText = shortcutText
                }

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

    private fun createFooter(): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 32)
            border = JBUI.Borders.empty(0, 18)

            val settingsLink =
                JBLabel("Settings").apply {
                    foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                    font = font.deriveFont(12f)
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(
                        object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                ShowSettingsUtil.getInstance()
                                    .showSettingsDialog(project, "IssueLinker")
                            }
                        }
                    )
                }
            add(settingsLink, BorderLayout.WEST)

            val refreshLink =
                JBLabel("Refresh").apply {
                    foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                    font = font.deriveFont(12f)
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    addMouseListener(
                        object : MouseAdapter() {
                            override fun mouseClicked(e: MouseEvent) {
                                refreshContent()
                            }
                        }
                    )
                }
            add(refreshLink, BorderLayout.EAST)
        }
    }

    fun refresh() {
        refreshContent()
    }

    private fun executeAction(actionId: String) {
        val action = ActionManager.getInstance().getAction(actionId) ?: return
        val dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build()
        val event =
            AnActionEvent.createFromAnAction(
                action,
                null,
                ActionPlaces.TOOLWINDOW_CONTENT,
                dataContext,
            )
        action.actionPerformed(event)
    }
}
