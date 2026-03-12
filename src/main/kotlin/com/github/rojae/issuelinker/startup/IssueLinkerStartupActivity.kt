package com.github.rojae.issuelinker.startup

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.awt.datatransfer.StringSelection

class IssueLinkerStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Trigger service initialization and widget update after project is fully loaded
        val service = project.getService(IssueLinkerService::class.java) ?: return

        // Show startup notification if issue key is detected
        val issueKey = service.issueKey ?: return
        val issueUrl = service.buildIssueUrl()

        val notification =
            Notification(
                "IssueLinker",
                IssueLinkerBundle.message("startup.notification.title", issueKey),
                IssueLinkerBundle.message("startup.notification.content", issueKey),
                NotificationType.INFORMATION,
            )

        // Add "Open" action if URL is available
        if (issueUrl != null) {
            notification.addAction(
                NotificationAction.createSimpleExpiring(
                    IssueLinkerBundle.message("startup.notification.open")
                ) {
                    BrowserUtil.browse(issueUrl)
                }
            )
        }

        // Add "Copy Key" action
        notification.addAction(
            NotificationAction.createSimpleExpiring(
                IssueLinkerBundle.message("startup.notification.copy")
            ) {
                CopyPasteManager.getInstance().setContents(StringSelection(issueKey))
            }
        )

        Notifications.Bus.notify(notification, project)
    }
}
