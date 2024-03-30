package com.dashwave.plugin.windows

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
import com.intellij.ui.components.JBList
import com.jetbrains.rd.util.first
import java.awt.Dimension
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.Icon
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList

class DashwaveWindow : ToolWindowFactory {
    companion object {
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
        private var modulesList: JComboBox<String> = JComboBox<String>()
        private var variantsList: JComboBox<String> = JComboBox<String>()

        fun show(){
            val toolWindowManager = ToolWindowManager.getInstance(p)
            val myWindow = toolWindowManager.getToolWindow("Dashwave")
            myWindow?.show()
        }

        fun changeIcon(icon:Icon){
            val toolWindowManager = ToolWindowManager.getInstance(p)
            val myWindow = toolWindowManager.getToolWindow("Dashwave")
            myWindow?.setIcon(icon)
        }

        fun clearConsole(){
            console.clear()
        }

        fun getBuildConfigs(p:Project):DwBuildConfig{
            val cleanBuild = cleanBuildCheckbox.isSelected
            val debugEnabled = debugEnabledCheckBox.isSelected
            val openEmulator = openEmulatorCheckbox.isSelected
            val module: String = modulesList.selectedItem?.toString() ?: "app"
            val variant: String = variantsList.selectedItem?.toString() ?: "Debug"
            DashwaveWindow.displayInfo("open emulator is $openEmulator")
            return DwBuildConfig(cleanBuild, debugEnabled, openEmulator, module, variant, p.basePath)
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

        fun addModulesAndVariants(modulesVariants: Map<String, List<String>>, defaultModule: String, defaultVariant: String){
            modulesList.removeAllItems()
            variantsList.removeAllItems()

            modulesList.addItem(defaultModule)
            variantsList.addItem(defaultVariant)

            modulesVariants.keys.toTypedArray().forEach { module ->
                modulesList.addItem(module)
            }

            modulesList.addItemListener(ItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    val selectedModule = modulesList.selectedItem as String

                    variantsList.removeAllItems()
                    modulesVariants[selectedModule]?.forEach { variant ->
                        variantsList.addItem(variant)
                    }
                }
            })

            variantsList.addItemListener(ItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    val selectedVariant = variantsList.selectedItem as String
                }
            })
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        println("\n\ncretate tool window content is called")
        console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        val panel = JPanel(BorderLayout())
        // Set up the toolbar with a button
        val actionToolbar = JToolBar(JToolBar.HORIZONTAL)
        runButton = JButton(AllIcons.Actions.RunAll)
        runButton.addActionListener {
            val configs = getBuildConfigs(p)
            val build = DwBuild(configs)
            build.run(p)
        }

        cancelButton = JButton(AllIcons.Actions.Cancel)
        cancelButton.addActionListener{
            disableCancelButton()
            currentBuild?.exit()
            val stopBuild = DwCmds("stop-build", p.basePath, true)
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

        modulesList.addItemListener(ItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                val selectedModule = modulesList.selectedItem as String
//                displayInfo("Selected module is $selectedModule")
            }
        })
        variantsList.addItemListener(ItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                val selectedVariant = variantsList.selectedItem as String
//                displayInfo("Selected variant is $selectedVariant")
            }
        })

        modulesList.preferredSize = Dimension(100, modulesList.preferredSize.height)
        variantsList.preferredSize = Dimension(100, variantsList.preferredSize.height)

        // add non-item label to modules and variants
        optionToolbar.add(JLabel("Modules"))
        optionToolbar.add(modulesList)

        optionToolbar.add(JLabel("Variants"))
        optionToolbar.add(variantsList)
        openEmulatorCheckbox.isSelected = true

        panel.add(actionToolbar, BorderLayout.NORTH)
        panel.add(optionToolbar, BorderLayout.WEST)

        // Set up the console view

        panel.add(console.component, BorderLayout.CENTER)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.setAnchor(ToolWindowAnchor.BOTTOM, null)
        toolWindow.contentManager.addContent(content)
    }

}
