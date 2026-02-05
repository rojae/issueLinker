package com.github.rojae.issuelinker.util

import java.util.regex.PatternSyntaxException

object BranchParserUtil {
    fun parseIssueKey(branchName: String, regex: String): List<String>? {
        // Return null for empty branch name
        if (branchName.isEmpty()) {
            return null
        }

        // Return null for blank regex
        if (regex.isBlank()) {
            return null
        }

        return try {
            val pattern = Regex(regex)
            val matchResult = pattern.find(branchName)

            if (matchResult == null) {
                // No match found
                null
            } else {
                // Extract captured groups (drop group 0 which is the full match)
                val groups = matchResult.groupValues.drop(1)
                groups
            }
        } catch (_: PatternSyntaxException) {
            // Invalid regex pattern
            null
        } catch (_: IllegalArgumentException) {
            // Kotlin Regex throws IllegalArgumentException for invalid patterns
            null
        }
    }
}
