package com.github.rojae.issuelinker.util

import org.junit.Assert.*
import org.junit.Test

class BranchParserUtilTest {

    @Test
    fun `parse standard Jira pattern from feature branch`() {
        val result =
            BranchParserUtil.parseIssueKey(
                branchName = "feature/PROJ-123-some-description",
                regex = "([A-Z][A-Z0-9]+-\\d+)",
            )
        assertEquals(listOf("PROJ-123"), result)
    }

    @Test
    fun `parse issue key from branch without prefix`() {
        val result =
            BranchParserUtil.parseIssueKey(branchName = "PROJ-456", regex = "([A-Z][A-Z0-9]+-\\d+)")
        assertEquals(listOf("PROJ-456"), result)
    }

    @Test
    fun `parse multiple capture groups`() {
        val result =
            BranchParserUtil.parseIssueKey(
                branchName = "feature/PROJ-123-description",
                regex = "([A-Z]+)-(\\d+)",
            )
        assertEquals(listOf("PROJ", "123"), result)
    }

    @Test
    fun `parse with bugfix prefix`() {
        val result =
            BranchParserUtil.parseIssueKey(
                branchName = "bugfix/ABC-999-fix-login",
                regex = "([A-Z][A-Z0-9]+-\\d+)",
            )
        assertEquals(listOf("ABC-999"), result)
    }

    @Test
    fun `no match returns null`() {
        val result =
            BranchParserUtil.parseIssueKey(branchName = "main", regex = "([A-Z][A-Z0-9]+-\\d+)")
        assertNull(result)
    }

    @Test
    fun `branch without issue key returns null`() {
        val result =
            BranchParserUtil.parseIssueKey(
                branchName = "feature/add-new-feature",
                regex = "([A-Z][A-Z0-9]+-\\d+)",
            )
        assertNull(result)
    }

    @Test
    fun `invalid regex returns null without throwing`() {
        val result =
            BranchParserUtil.parseIssueKey(branchName = "feature/PROJ-123", regex = "([invalid")
        assertNull(result)
    }

    @Test
    fun `empty branch name returns null`() {
        val result =
            BranchParserUtil.parseIssueKey(branchName = "", regex = "([A-Z][A-Z0-9]+-\\d+)")
        assertNull(result)
    }

    @Test
    fun `empty regex returns null`() {
        val result = BranchParserUtil.parseIssueKey(branchName = "feature/PROJ-123", regex = "")
        assertNull(result)
    }

    @Test
    fun `blank regex returns null`() {
        val result = BranchParserUtil.parseIssueKey(branchName = "feature/PROJ-123", regex = "   ")
        assertNull(result)
    }

    @Test
    fun `regex without capture group returns empty list on match`() {
        val result =
            BranchParserUtil.parseIssueKey(branchName = "feature/PROJ-123", regex = "[A-Z]+-\\d+")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `parse complex branch name with multiple slashes`() {
        val result =
            BranchParserUtil.parseIssueKey(
                branchName = "users/john/feature/TEAM-42-implement-auth",
                regex = "([A-Z][A-Z0-9]+-\\d+)",
            )
        assertEquals(listOf("TEAM-42"), result)
    }
}
