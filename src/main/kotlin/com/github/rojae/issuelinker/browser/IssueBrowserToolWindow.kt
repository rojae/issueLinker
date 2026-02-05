package com.github.rojae.issuelinker.browser

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class IssueBrowserToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel =
            JPanel(BorderLayout()).apply {
                val message =
                    if (JBCefApp.isSupported()) {
                        "Click an issue link or press Cmd+Alt+J to load issue"
                    } else {
                        "JCEF browser is not supported in this environment"
                    }
                add(JLabel(message, SwingConstants.CENTER), BorderLayout.CENTER)
            }

        val content = ContentFactory.getInstance().createContent(panel, "Issue", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true

    companion object {
        const val TOOL_WINDOW_ID = "Issue Browser"

        fun openUrl(project: Project, url: String, title: String) {
            if (!JBCefApp.isSupported()) {
                // Fallback to external browser if JCEF not supported
                com.intellij.ide.BrowserUtil.browse(url)
                return
            }

            ApplicationManager.getApplication().invokeLater {
                val toolWindowManager = ToolWindowManager.getInstance(project)
                val toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID)

                // If tool window doesn't exist (shouldn't happen if plugin.xml is correct)
                if (toolWindow == null) {
                    // Fallback to external browser
                    com.intellij.ide.BrowserUtil.browse(url)
                    return@invokeLater
                }

                // Create browser content
                val browser = JBCefBrowser(url)
                val panel =
                    JPanel(BorderLayout()).apply { add(browser.component, BorderLayout.CENTER) }

                val contentFactory = ContentFactory.getInstance()
                val content = contentFactory.createContent(panel, title, true)
                content.setDisposer { browser.dispose() }

                // Remove old content and add new
                toolWindow.contentManager.removeAllContents(true)
                toolWindow.contentManager.addContent(content)
                toolWindow.contentManager.setSelectedContent(content)

                // Show and activate the tool window
                toolWindow.show { toolWindow.activate(null) }
            }
        }
    }
}
