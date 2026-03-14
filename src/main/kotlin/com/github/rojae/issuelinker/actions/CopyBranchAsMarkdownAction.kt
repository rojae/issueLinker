package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.github.rojae.issuelinker.util.GitRemoteUrlUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import git4idea.repo.GitRepositoryManager
import java.awt.datatransfer.StringSelection

class CopyBranchAsMarkdownAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull()

        val branchName = repository?.currentBranch?.name
        if (branchName == null) {
            IssueLinkerNotifications.notifyNoBranch(project)
            return
        }

        val remoteUrl = repository.remotes.firstOrNull()?.firstUrl
        if (remoteUrl == null) {
            IssueLinkerNotifications.notifyNoRemote(project)
            return
        }

        val branchUrl = GitRemoteUrlUtil.buildBranchUrl(remoteUrl, branchName)
        if (branchUrl != null) {
            val markdown = "[$branchName]($branchUrl)"
            CopyPasteManager.getInstance().setContents(StringSelection(markdown))
            IssueLinkerNotifications.notifySuccess(
                project,
                IssueLinkerBundle.message("action.copyBranchAsMarkdown.success", markdown),
            )
        } else {
            IssueLinkerNotifications.notifyNoRemote(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isVisible = project != null
        if (project != null) {
            e.presentation.isEnabled = IssueLinkerService.getInstance(project).branchName != null
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
