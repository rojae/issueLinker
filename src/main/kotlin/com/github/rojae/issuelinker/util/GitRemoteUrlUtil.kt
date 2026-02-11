package com.github.rojae.issuelinker.util

/**
 * Utility for converting Git remote URLs to web-browsable branch URLs.
 *
 * Supports SSH and HTTPS formats for GitHub, GitLab, and Bitbucket.
 */
object GitRemoteUrlUtil {

    /**
     * Builds a web-browsable URL for the given branch on the remote repository.
     *
     * @param remoteUrl The Git remote URL (SSH or HTTPS format)
     * @param branchName The branch name to link to
     * @return The web URL for the branch, or null if the remote URL cannot be parsed
     *
     * Examples:
     * - buildBranchUrl("git@github.com:org/repo.git", "feature/ABC-123") ->
     *   "https://github.com/org/repo/tree/feature/ABC-123"
     * - buildBranchUrl("https://github.com/org/repo.git", "main") ->
     *   "https://github.com/org/repo/tree/main"
     * - buildBranchUrl("git@gitlab.com:org/repo.git", "develop") ->
     *   "https://gitlab.com/org/repo/-/tree/develop"
     * - buildBranchUrl("git@bitbucket.org:org/repo.git", "release/1.0") ->
     *   "https://bitbucket.org/org/repo/src/release/1.0"
     */
    fun buildBranchUrl(remoteUrl: String, branchName: String): String? {
        val baseUrl = convertToHttpsUrl(remoteUrl) ?: return null
        val branchPath = getBranchPath(baseUrl, branchName)
        return "$baseUrl$branchPath"
    }

    /**
     * Converts a Git remote URL (SSH or HTTPS) to a clean HTTPS base URL.
     *
     * @param remoteUrl The raw Git remote URL
     * @return The HTTPS base URL without trailing .git, or null if unparseable
     */
    internal fun convertToHttpsUrl(remoteUrl: String): String? {
        val url = remoteUrl.trim()
        if (url.isBlank()) return null

        val httpsUrl =
            when {
                // SSH format: git@github.com:org/repo.git
                url.matches(Regex("^[\\w.-]+@[\\w.-]+:.+$")) -> {
                    val atIndex = url.indexOf('@')
                    val colonIndex = url.indexOf(':', atIndex)
                    val host = url.substring(atIndex + 1, colonIndex)
                    val path = url.substring(colonIndex + 1)
                    "https://$host/$path"
                }
                // HTTPS format: https://github.com/org/repo.git
                url.startsWith("https://") || url.startsWith("http://") -> {
                    url.replaceFirst("http://", "https://")
                }
                else -> return null
            }

        // Remove trailing .git
        return httpsUrl.removeSuffix(".git").trimEnd('/')
    }

    /**
     * Returns the branch path segment based on the hosting platform.
     *
     * @param baseUrl The HTTPS base URL of the repository
     * @param branchName The branch name
     * @return The platform-specific branch path
     */
    private fun getBranchPath(baseUrl: String, branchName: String): String {
        return when {
            baseUrl.contains("gitlab.com") || baseUrl.contains("gitlab.") -> "/-/tree/$branchName"
            baseUrl.contains("bitbucket.org") || baseUrl.contains("bitbucket.") ->
                "/src/$branchName"
            // GitHub and all other platforms default to /tree/
            else -> "/tree/$branchName"
        }
    }
}
