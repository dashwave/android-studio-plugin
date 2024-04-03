package com.dashwave.plugin.windows

import com.dashwave.plugin.actions.BuildAction
import com.dashwave.plugin.utils.DwBuild
import com.dashwave.plugin.utils.DwBuildConfig
import com.dashwave.plugin.utils.DwCmds
import com.dashwave.plugin.utils.Process
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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AnimatedIcon
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.Icon
import javax.swing.JLabel

class DashwaveWindow(project: Project){
    var lastEmulatorProcess:DwCmds? = null
    var currentBuild:DwCmds? = null
    lateinit var console: ConsoleView
    lateinit var runButton: JButton
    lateinit var cancelButton: JButton
    lateinit var p: Project
    var dwIcon: Icon = IconLoader.getIcon("/icons/dashwave13.svg")
    var loadIcon: Icon = AnimatedIcon.Default()
    var runEnabled = false
    private  var cleanBuildCheckbox:JCheckBox = JCheckBox("Clean Build")
    private  var debugEnabledCheckBox: JCheckBox = JCheckBox("Enable Debug")
    private  var openEmulatorCheckbox:JCheckBox = JCheckBox("Open Emulator")
    var window:ToolWindow? = null

    init {
        p = project
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.registerToolWindow(
            "Dashwave",  // ID of the tool window
            false,            // canCloseContent - whether the tool window can be closed
            ToolWindowAnchor.BOTTOM  // The anchor location
        )
        window = toolWindow
        toolWindow.setIcon(IconLoader.getIcon("/icons/dashwave13.svg"))
        createToolWindowContent()
    }


    fun show(){
        window?.show()
    }
    fun changeIcon(icon:Icon){
        window?.setIcon(icon)
    }

    fun clearConsole(){
        console.clear()
    }

    fun getBuildConfigs():DwBuildConfig{
        val cleanBuild = cleanBuildCheckbox.isSelected
        val debugEnabled = debugEnabledCheckBox.isSelected
        val openEmulator = openEmulatorCheckbox.isSelected
        return DwBuildConfig(cleanBuild, debugEnabled, openEmulator,p.basePath)
    }

    fun displayOutput(s:String, type:ConsoleViewContentType){
        console.print(s, type)
    }

    fun displayInfo(s:String){
        console.print(s, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    fun displayError(s:String){
        console.print(s, ConsoleViewContentType.ERROR_OUTPUT)
    }

    fun disableRunButton(){
        runEnabled = false
        runButton.isEnabled = false
        cleanBuildCheckbox.isEnabled = false
        debugEnabledCheckBox.isEnabled = false
        openEmulatorCheckbox.isEnabled = false
    }

    fun enableRunButton(){
        runEnabled = true
        runButton.isEnabled = true
        cleanBuildCheckbox.isEnabled = true
        debugEnabledCheckBox.isEnabled = true
        openEmulatorCheckbox.isEnabled = true
    }

    fun disableCancelButton(){
        cancelButton.isEnabled = false
    }
    fun enableCancelButton(){
        cancelButton.isEnabled = true
    }


    private fun createToolWindowContent() {
        println("\n\ncretate tool window content is called\n\n")
        console = TextConsoleBuilderFactory.getInstance().createBuilder(p).console
        val contentFactory = ContentFactory.getInstance()
        val panel = JPanel(BorderLayout())
        // Set up the toolbar with a button
        val actionToolbar = JToolBar(JToolBar.HORIZONTAL)
        runButton = JButton(AllIcons.Actions.RunAll)
        runButton.addActionListener {
            val configs = getBuildConfigs()
            val build = DwBuild(configs, this)
            build.run(p)
        }

        cancelButton = JButton(AllIcons.Actions.Cancel)
        cancelButton.addActionListener{
            disableCancelButton()
            currentBuild?.exit()
            val stopBuild = DwCmds("stop-build", p.basePath, true, this)
            stopBuild.executeWithExitCode()
            enableRunButton()
        }

        actionToolbar.add(runButton)
        actionToolbar.add(cancelButton)
        disableCancelButton()
        disableRunButton()

        val optionToolbar = JToolBar(JToolBar.VERTICAL)
        val optionTitle = JLabel("Build Options")
        optionToolbar.add(optionTitle)
        optionToolbar.add(cleanBuildCheckbox)
        optionToolbar.add(debugEnabledCheckBox)
        optionToolbar.add(openEmulatorCheckbox)
        openEmulatorCheckbox.isSelected = true

        panel.add(actionToolbar, BorderLayout.NORTH)
        panel.add(optionToolbar, BorderLayout.WEST)

        // Set up the console view

        panel.add(console.component, BorderLayout.CENTER)

        val content = contentFactory.createContent(panel, "", false)
        window?.setAnchor(ToolWindowAnchor.BOTTOM, null)
        window?.contentManager?.addContent(content)
    }

}
