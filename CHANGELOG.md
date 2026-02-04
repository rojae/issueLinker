# IssueLinker Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2025-02-05

### Added
- Initial release
- Parse issue keys from Git branch names using configurable regex
- Open issues in browser with keyboard shortcut (`Cmd+Alt+J` / `Ctrl+Alt+J`)
- Status bar widget showing current issue key
- Internal browser support (IntelliJ's built-in JCEF browser)
- Settings panel at **Settings → Tools → IssueLinker**
  - Host URL configuration
  - URL Path Pattern with placeholder support (`{0}`, `{1}`, `{2}`)
  - Branch Parsing Regex configuration
  - Internal/External browser toggle
- Context menu integration (Editor, Project View)
- Tools menu integration
- Support for multiple issue trackers (Jira, GitHub, GitLab, Linear, etc.)
