package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.util.GitRemoteUrlUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import java.awt.datatransfer.StringSelection

class CopyBranchAsMarkdownAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull()

        val branchName = repository?.currentBranch?.name
        if (branchName == null) {
            showNoBranchNotification(project)
            return
        }

        val remoteUrl = repository.remotes.firstOrNull()?.firstUrl
        if (remoteUrl == null) {
            showNoRemoteNotification(project)
            return
        }

        val branchUrl = GitRemoteUrlUtil.buildBranchUrl(remoteUrl, branchName)
        if (branchUrl != null) {
            val markdown = "[$branchName]($branchUrl)"
            CopyPasteManager.getInstance().setContents(StringSelection(markdown))
            showSuccessNotification(project, markdown)
        } else {
            showNoRemoteNotification(project)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = (e.project != null)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun showSuccessNotification(project: Project, markdown: String) {
        val notification =
            Notification(
                "IssueLinker",
                IssueLinkerBundle.message("action.copyBranchAsMarkdown.success", markdown),
                "",
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }

    private fun showNoBranchNotification(project: Project) {
        val notification =
            Notification(
                "IssueLinker",
                IssueLinkerBundle.message("action.noBranch.title"),
                IssueLinkerBundle.message("action.noBranch.content"),
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }

    private fun showNoRemoteNotification(project: Project) {
        val notification =
            Notification(
                "IssueLinker",
                IssueLinkerBundle.message("action.noRemote.title"),
                IssueLinkerBundle.message("action.noRemote.content"),
                NotificationType.INFORMATION,
            )
        Notifications.Bus.notify(notification, project)
    }
}
