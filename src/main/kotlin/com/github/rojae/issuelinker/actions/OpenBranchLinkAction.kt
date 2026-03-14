package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.services.IssueLinkerService
import com.github.rojae.issuelinker.util.GitRemoteUrlUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import git4idea.repo.GitRepositoryManager

class OpenBranchLinkAction : AnAction(), DumbAware {

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
            BrowserUtil.browse(branchUrl)
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
