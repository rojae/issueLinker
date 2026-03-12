package com.github.rojae.issuelinker.editor

import com.github.rojae.issuelinker.IssueLinkerBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class OpenIssueIntentionAction(private val issueKey: String, private val url: String) :
    IntentionAction {

    override fun getText(): String = IssueLinkerBundle.message("annotator.openIssue", issueKey)

    override fun getFamilyName(): String = "IssueLinker"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        BrowserUtil.browse(url)
    }

    override fun startInWriteAction(): Boolean = false
}
