package com.github.rojae.issuelinker.startup

import com.github.rojae.issuelinker.services.IssueLinkerService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class IssueLinkerStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Trigger service initialization and widget update after project is fully loaded
        project.getService(IssueLinkerService::class.java)
    }
}
