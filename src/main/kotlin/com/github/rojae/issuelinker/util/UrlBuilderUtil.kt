package com.github.rojae.issuelinker.util

/** Utility for building URLs from host, path pattern, and captured regex groups. */
object UrlBuilderUtil {
    /**
     * Builds a URL by combining host and path pattern, replacing placeholders with captured groups.
     *
     * @param hostUrl The base URL (e.g., "https://jira.company.com")
     * @param pathPattern The path pattern with placeholders (e.g., "/browse/{0}")
     * @param capturedGroups List of captured regex groups to replace placeholders
     * @return The built URL, or null if hostUrl is blank
     *
     * Examples:
     * - buildUrl("https://jira.company.com", "/browse/{0}", listOf("PROJ-123")) →
     *   "https://jira.company.com/browse/PROJ-123"
     * - buildUrl("https://jira.company.com", "/projects/{0}/issues/{1}", listOf("PROJ", "123")) →
     *   "https://jira.company.com/projects/PROJ/issues/123"
     * - buildUrl("https://jira.company.com/", "/browse/{0}", listOf("PROJ-123")) →
     *   "https://jira.company.com/browse/PROJ-123"
     * - buildUrl("", "/browse/{0}", listOf("PROJ-123")) → null
     */
    fun buildUrl(hostUrl: String, pathPattern: String, capturedGroups: List<String>): String? {
        // Return null if host is blank
        if (hostUrl.isBlank()) {
            return null
        }

        // Normalize host: remove trailing slash
        val normalizedHost = hostUrl.trimEnd('/')

        // Normalize path: add leading slash if non-empty and missing
        val normalizedPath =
            if (pathPattern.isNotEmpty() && !pathPattern.startsWith('/')) {
                "/$pathPattern"
            } else {
                pathPattern
            }

        // Replace placeholders {0}, {1}, {2}, etc. with corresponding captured groups
        var resultPath = normalizedPath
        capturedGroups.forEachIndexed { index, group ->
            resultPath = resultPath.replace("{$index}", group)
        }

        // Combine host and path
        return normalizedHost + resultPath
    }
}
