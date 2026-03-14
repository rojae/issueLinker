package com.github.rojae.issuelinker.vcs

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.changes.ui.CommitMessageProvider

class IssueLinkerCommitMessageProvider : CommitMessageProvider {

    override fun getCommitMessage(forChangelist: LocalChangeList, project: Project): String? {
        val service = IssueLinkerService.getInstance(project)
        val issueKey = service.issueKey ?: return null

        return IssueLinkerBundle.message("commit.message.prefix", issueKey) + " "
    }
}
