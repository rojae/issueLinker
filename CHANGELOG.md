# IssueLinker Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Recent Issues History** — Track last 10 issue keys per project with persistent storage
  - Recent issues section in tool window panel
  - Recent issues section in status bar popup (last 5)
  - Click to open any recent issue in browser
- **Commit Message Template** — Auto-insert issue key prefix (e.g., `[PROJ-123] `) into commit message when opening VCS commit dialog
- **Settings Test Configuration** — Real-time preview panel in Settings
  - Sample branch name input field
  - Live issue key extraction preview
  - Live URL resolution preview
  - Regex validation with error feedback
- **Issue Key Highlight in Editor** — Detects issue key patterns in editor text
  - Underlined with clickable link style
  - Alt+Enter intention action to open issue in browser
  - Works across all file types
- **Startup Notification** — On project open, shows a balloon notification with detected issue key
  - Quick-action buttons: Open in Browser / Copy Key
  - Non-intrusive balloon style
- **No-key State UX** — Clear feedback when no issue key is detected
  - Status bar widget shows "No Issue" in dimmed style
  - Tool window shows "No Issue Linked" with configure shortcut link
  - Popup shows descriptive empty state with configuration hint

### Fixed
- EDT threading safety for widget and tool window UI updates
- Optimized editor annotator with regex caching for better performance
- Eliminated duplicate recent issue entries on repeated git events

## [1.2.0] - 2025-02-07

### Added
- Status bar widget popup panel (click to open)
  - Copy Issue Key - Copy issue key to clipboard
  - Copy Issue Link - Copy full URL to clipboard
  - Copy as Markdown - Copy as `[KEY](URL)` format
  - Open in Browser - Open issue in browser
  - Open Tool Window - Quick access to IssueLinker panel
  - Settings - Quick access to plugin settings
- IssueLinker Tool Window (View → Tool Windows → IssueLinker)
  - Persistent side panel with all actions
  - Current issue display with status
  - Tab shows issue key (e.g., PROJ-123)
  - Refresh button for manual update
- Menu items are disabled when no issue is detected

### Changed
- Minimum supported IDE version: 2024.3

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
