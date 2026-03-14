package com.github.rojae.issuelinker.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

data class RecentIssueEntry(var issueKey: String = "", var timestamp: Long = 0L)

data class RecentIssuesState(var entries: MutableList<RecentIssueEntry> = mutableListOf())

@Service(Service.Level.PROJECT)
@State(name = "IssueLinkerRecentIssues", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class RecentIssuesService : PersistentStateComponent<RecentIssuesState> {
    private var state = RecentIssuesState()

    override fun getState(): RecentIssuesState = synchronized(state) { state }

    override fun loadState(state: RecentIssuesState) {
        synchronized(this.state) { this.state = state }
    }

    fun addIssueKey(issueKey: String) {
        synchronized(state) {
            // Remove existing entry with same key
            state.entries.removeAll { it.issueKey == issueKey }
            // Add at the beginning
            state.entries.add(0, RecentIssueEntry(issueKey, System.currentTimeMillis()))
            // Keep only last 10
            if (state.entries.size > MAX_ENTRIES) {
                state.entries = state.entries.take(MAX_ENTRIES).toMutableList()
            }
        }
    }

    fun getRecentIssues(): List<RecentIssueEntry> = synchronized(state) { state.entries.toList() }

    fun removeIssueKey(issueKey: String) {
        synchronized(state) { state.entries.removeAll { it.issueKey == issueKey } }
    }

    companion object {
        const val MAX_ENTRIES = 10

        fun getInstance(project: Project): RecentIssuesService = project.service()
    }
}
