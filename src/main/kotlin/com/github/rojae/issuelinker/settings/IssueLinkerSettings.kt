package com.github.rojae.issuelinker.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

data class IssueLinkerSettingsState(
    var hostUrl: String = "https://jira.company.com",
    var urlPathPattern: String = "/browse/{0}",
    var branchParsingRegex: String = "([A-Z][A-Z0-9]+-\\d+)",
    var useInternalBrowser: Boolean = true,
)

@Service(Service.Level.APP)
@State(name = "IssueLinkerSettings", storages = [Storage("IssueLinkerSettings.xml")])
class IssueLinkerSettings : PersistentStateComponent<IssueLinkerSettingsState> {
    private var state = IssueLinkerSettingsState()

    override fun getState(): IssueLinkerSettingsState = state

    override fun loadState(state: IssueLinkerSettingsState) {
        this.state = state
    }

    var hostUrl: String
        get() = state.hostUrl
        set(value) {
            state.hostUrl = value
        }

    var urlPathPattern: String
        get() = state.urlPathPattern
        set(value) {
            state.urlPathPattern = value
        }

    var branchParsingRegex: String
        get() = state.branchParsingRegex
        set(value) {
            state.branchParsingRegex = value
        }

    var useInternalBrowser: Boolean
        get() = state.useInternalBrowser
        set(value) {
            state.useInternalBrowser = value
        }

    companion object {
        fun getInstance(): IssueLinkerSettings = service()
    }
}
