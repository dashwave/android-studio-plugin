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
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JToolBar
import com.intellij.icons.AllIcons;
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.JLabel

class DashwaveWindow : ToolWindowFactory {
    companion object {
        private lateinit var console: ConsoleView
        private lateinit var runButton: JButton
        private lateinit var cancelButton: JButton
        fun displayOutput(s:String, type: ConsoleViewContentType){
            console.print(s,type)
        }
        fun getConsole():ConsoleView {
            return console
        }

        fun disableRunButton(){
            runButton.isEnabled = false
        }

        fun enableRunButton(){
            runButton.isEnabled = true
        }

        fun disableCancelButton(){
            cancelButton.isEnabled = false
        }
        fun enableCancelButton(){
            cancelButton.isEnabled = true
        }
        var cleanBuild:Boolean = false
        var debugEnabled:Boolean = false
    }

    private var buildAction:BuildAction? = null


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())

        // Set up the toolbar with a button
        val actionToolbar = JToolBar(JToolBar.HORIZONTAL)

        runButton = JButton(AllIcons.Actions.RunAll)
        runButton.addActionListener {
            val console = DashwaveWindow.getConsole()
            console.clear()
            disableRunButton()
            enableCancelButton()
            buildAction = BuildAction()
            buildAction?.initBuildProcHandler(project.basePath)
            buildAction?.startBuildProcHandler()
        }

        cancelButton = JButton(AllIcons.Actions.Cancel)
        cancelButton.addActionListener{
            disableCancelButton()
            // call dw stop-build cmd here
            buildAction?.terminateBuildProcHandler(project.basePath)
        }

        actionToolbar.add(runButton)
        actionToolbar.add(cancelButton)
        disableCancelButton()

        val optionToolbar = JToolBar(JToolBar.VERTICAL)
        val optionTitle = JLabel("Build Options")
        val cleanBuildCheckbox = JCheckBox("Clean Build")
        cleanBuildCheckbox.addItemListener(ItemListener { e ->
            cleanBuild = e.stateChange == ItemEvent.SELECTED
        })
        val debugBuildCheckbox = JCheckBox("Debug")
        debugBuildCheckbox.addItemListener(ItemListener { e->
            debugEnabled = e.stateChange == ItemEvent.SELECTED
        })
        optionToolbar.add(optionTitle)
        optionToolbar.add(cleanBuildCheckbox)
        optionToolbar.add(debugBuildCheckbox)

        panel.add(actionToolbar, BorderLayout.NORTH)
        panel.add(optionToolbar, BorderLayout.WEST)
        console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        // Set up the console view

        panel.add(console.component, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, null)
        toolWindow.contentManager.addContent(content)
    }

}
