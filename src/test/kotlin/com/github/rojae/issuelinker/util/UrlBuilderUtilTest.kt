package com.github.rojae.issuelinker.util

import org.junit.Assert.*
import org.junit.Test

class UrlBuilderUtilTest {

    @Test
    fun `build basic URL with single placeholder`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/browse/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/browse/PROJ-123", result)
    }

    @Test
    fun `build URL with multiple placeholders`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/projects/{0}/issues/{1}",
                capturedGroups = listOf("PROJ", "123"),
            )
        assertEquals("https://jira.company.com/projects/PROJ/issues/123", result)
    }

    @Test
    fun `normalize host URL with trailing slash`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com/",
                pathPattern = "/browse/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/browse/PROJ-123", result)
    }

    @Test
    fun `normalize path pattern without leading slash`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "browse/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/browse/PROJ-123", result)
    }

    @Test
    fun `handle both trailing and missing leading slash`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com/",
                pathPattern = "browse/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/browse/PROJ-123", result)
    }

    @Test
    fun `empty host URL returns null`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "",
                pathPattern = "/browse/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertNull(result)
    }

    @Test
    fun `blank host URL returns null`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "   ",
                pathPattern = "/browse/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertNull(result)
    }

    @Test
    fun `empty captured groups with placeholders leaves placeholders`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/browse/{0}",
                capturedGroups = emptyList(),
            )
        assertEquals("https://jira.company.com/browse/{0}", result)
    }

    @Test
    fun `missing placeholder index leaves placeholder unchanged`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/browse/{0}/{1}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/browse/PROJ-123/{1}", result)
    }

    @Test
    fun `path pattern without placeholders works`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/issues",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/issues", result)
    }

    @Test
    fun `empty path pattern returns host URL only`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com", result)
    }

    @Test
    fun `placeholder with higher index`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/browse/{2}",
                capturedGroups = listOf("A", "B", "C"),
            )
        assertEquals("https://jira.company.com/browse/C", result)
    }

    @Test
    fun `multiple same placeholders are all replaced`() {
        val result =
            UrlBuilderUtil.buildUrl(
                hostUrl = "https://jira.company.com",
                pathPattern = "/{0}/details/{0}",
                capturedGroups = listOf("PROJ-123"),
            )
        assertEquals("https://jira.company.com/PROJ-123/details/PROJ-123", result)
    }
}
