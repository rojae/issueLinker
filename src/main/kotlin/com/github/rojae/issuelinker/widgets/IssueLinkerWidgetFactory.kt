package com.github.rojae.issuelinker.widgets

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

class IssueLinkerWidgetFactory : StatusBarWidgetFactory {
    companion object {
        const val WIDGET_ID = "IssueLinkerWidget"
    }

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = IssueLinkerBundle.message("widget.displayName")

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = IssueLinkerWidget(project)
}

class IssueLinkerWidget(private val project: Project) :
    StatusBarWidget, StatusBarWidget.TextPresentation, Disposable {

    private var statusBar: StatusBar? = null

    override fun ID(): String = IssueLinkerWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        // Widget updates are triggered by IssueLinkerService after branch change
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getText(): String {
        val service = project.getService(IssueLinkerService::class.java) ?: return "N/A"
        return service.issueKey ?: IssueLinkerBundle.message("widget.noIssue")
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getTooltipText() = IssueLinkerBundle.message("widget.tooltip")

    override fun getClickConsumer() =
        Consumer<MouseEvent> {
            project.getService(IssueLinkerService::class.java)?.openIssueInBrowser()
        }
}
