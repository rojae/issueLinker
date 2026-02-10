package com.github.rojae.issuelinker.services

import com.intellij.util.messages.Topic

interface IssueLinkerNotifier {
    companion object {
        val TOPIC = Topic.create("IssueLinker Update", IssueLinkerNotifier::class.java)
    }

    fun issueKeyChanged(issueKey: String?, branchName: String?)
}
