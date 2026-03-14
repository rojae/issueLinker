package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import java.awt.datatransfer.StringSelection

class CopyIssueKeyAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = IssueLinkerService.getInstance(project)
        val key = service.issueKey

        if (key != null) {
            CopyPasteManager.getInstance().setContents(StringSelection(key))
            IssueLinkerNotifications.notifySuccess(
                project,
                IssueLinkerBundle.message("action.copyIssueKey.success", key),
            )
        } else {
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
