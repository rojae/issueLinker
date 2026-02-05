package com.github.rojae.issuelinker.services

import com.github.rojae.issuelinker.browser.IssueBrowserToolWindowFactory
import com.github.rojae.issuelinker.settings.IssueLinkerSettings
import com.github.rojae.issuelinker.util.BranchParserUtil
import com.github.rojae.issuelinker.util.UrlBuilderUtil
import com.github.rojae.issuelinker.widgets.IssueLinkerWidgetFactory
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.jcef.JBCefApp
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager

@Service(Service.Level.PROJECT)
class IssueLinkerService(private val project: Project) : Disposable {

    private var currentIssueKey: String? = null
    private var currentCapturedGroups: List<String>? = null

    val issueKey: String?
        get() = currentIssueKey

    val capturedGroups: List<String>?
        get() = currentCapturedGroups

    init {
        project.messageBus
            .connect(this)
            .subscribe(
                GitRepository.GIT_REPO_CHANGE,
                GitRepositoryChangeListener { updateFromCurrentBranch() },
            )
        updateFromCurrentBranch()
    }

    private fun updateFromCurrentBranch() {
        val branchName = getCurrentBranchName()
        if (branchName != null) {
            val settings = IssueLinkerSettings.getInstance()
            val capturedGroups =
                BranchParserUtil.parseIssueKey(branchName, settings.branchParsingRegex)
            currentIssueKey = capturedGroups?.firstOrNull()
            currentCapturedGroups = capturedGroups
        } else {
            currentIssueKey = null
            currentCapturedGroups = null
        }
        // Update widget after service state is updated
        updateWidget()
    }

    private fun updateWidget() {
        val statusBar = WindowManager.getInstance().getStatusBar(project) ?: return
        statusBar.updateWidget(IssueLinkerWidgetFactory.WIDGET_ID)
    }

    private fun getCurrentBranchName(): String? {
        return GitRepositoryManager.getInstance(project)
            .repositories
            .firstOrNull()
            ?.currentBranch
            ?.name
    }

    fun buildIssueUrl(): String? {
        val settings = IssueLinkerSettings.getInstance()
        val groups = capturedGroups ?: return null
        return UrlBuilderUtil.buildUrl(settings.hostUrl, settings.urlPathPattern, groups)
    }

    fun openIssueInBrowser(): Boolean {
        val url = buildIssueUrl() ?: return false
        val settings = IssueLinkerSettings.getInstance()

        if (settings.useInternalBrowser && JBCefApp.isSupported()) {
            // Open in IntelliJ's internal browser tool window
            val title = currentIssueKey ?: "Issue"
            IssueBrowserToolWindowFactory.openUrl(project, url, title)
        } else {
            // Open in external system browser
            BrowserUtil.browse(url)
        }
        return true
    }

    override fun dispose() {
        // Cleanup handled by message bus disconnection
    }

    companion object {
        fun getInstance(project: Project): IssueLinkerService = project.service()
    }
}
