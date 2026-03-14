package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class OpenIssueAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = IssueLinkerService.getInstance(project)

        if (!service.openIssueInBrowser()) {
            IssueLinkerNotifications.notifyNoIssue(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isVisible = project != null
        if (project != null) {
            e.presentation.isEnabled = IssueLinkerService.getInstance(project).issueKey != null
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
