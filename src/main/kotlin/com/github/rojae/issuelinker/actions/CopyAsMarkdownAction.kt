package com.github.rojae.issuelinker.actions

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import java.awt.datatransfer.StringSelection

class CopyAsMarkdownAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = IssueLinkerService.getInstance(project)
        val key = service.issueKey
        val url = service.buildIssueUrl()

        if (key != null && url != null) {
            val markdown = "[$key]($url)"
            CopyPasteManager.getInstance().setContents(StringSelection(markdown))
            IssueLinkerNotifications.notifySuccess(
                project,
                IssueLinkerBundle.message("action.copyAsMarkdown.success", markdown),
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
