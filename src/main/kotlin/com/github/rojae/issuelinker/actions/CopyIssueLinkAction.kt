package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import java.awt.datatransfer.StringSelection

class CopyIssueLinkAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = IssueLinkerService.getInstance(project)
        val url = service.buildIssueUrl()

        if (url != null) {
            CopyPasteManager.getInstance().setContents(StringSelection(url))
            showSuccessNotification(project, url)
        } else {
            showNoIssueNotification(project)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = (e.project != null)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun showSuccessNotification(project: Project, url: String) {
        val notification =
            Notification(
                "IssueLinker",
                IssueLinkerBundle.message("action.copyIssueLink.success", url),
                "",
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }

    private fun showNoIssueNotification(project: Project) {
        val notification =
            Notification(
                "IssueLinker",
                IssueLinkerBundle.message("notification.noIssue.title"),
                IssueLinkerBundle.message("notification.noIssue.content"),
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }
}
