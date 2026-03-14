package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.TimeUnit

object IssueLinkerNotifications {

    private const val GROUP_ID = "IssueLinker"
    private const val EXPIRE_DELAY_SECONDS = 2L

    fun notifySuccess(project: Project, message: String) {
        val notification = Notification(GROUP_ID, message, "", NotificationType.INFORMATION)
        Notifications.Bus.notify(notification, project)
        AppExecutorUtil.getAppScheduledExecutorService()
            .schedule({ notification.expire() }, EXPIRE_DELAY_SECONDS, TimeUnit.SECONDS)
    }

    fun notifyNoIssue(project: Project) {
        val notification =
            Notification(
                GROUP_ID,
                IssueLinkerBundle.message("notification.noIssue.title"),
                IssueLinkerBundle.message("notification.noIssue.content"),
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }

    fun notifyNoBranch(project: Project) {
        val notification =
            Notification(
                GROUP_ID,
                IssueLinkerBundle.message("action.noBranch.title"),
                IssueLinkerBundle.message("action.noBranch.content"),
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }

    fun notifyNoRemote(project: Project) {
        val notification =
            Notification(
                GROUP_ID,
                IssueLinkerBundle.message("action.noRemote.title"),
                IssueLinkerBundle.message("action.noRemote.content"),
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }
}
