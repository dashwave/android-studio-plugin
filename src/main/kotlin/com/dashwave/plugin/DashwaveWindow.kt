package com.dashwave.plugin

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JToolBar


class DashwaveWindow : ToolWindowFactory {
    companion object {
        private lateinit var console: ConsoleView
        fun displayOutput(s:String, type: ConsoleViewContentType){
            console.print(s,type)
        }
        fun getConsole():ConsoleView {
            return console
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        // Set up the toolbar with a button
        val toolbar = JToolBar(JToolBar.VERTICAL)
//        val button = JButton("Run Command")
//
//        button.addActionListener {
//            // Handle button click
//            displayOutput("hello", ConsoleViewContentType.NORMAL_OUTPUT)
//        }
//        toolbar.add(button)
        panel.add(toolbar, BorderLayout.WEST)
        console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        // Set up the console view

        panel.add(console.component, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, null)
        toolWindow.contentManager.addContent(content)
    }

}
