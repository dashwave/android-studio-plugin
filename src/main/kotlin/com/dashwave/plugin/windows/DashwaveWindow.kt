package com.dashwave.plugin.windows

import com.dashwave.plugin.PluginStartup
import com.dashwave.plugin.components.CollapseMenu
import com.dashwave.plugin.loginUser
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
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBList
import com.jetbrains.rd.util.first
import com.sun.xml.bind.v2.Messages
import java.awt.Color
import java.awt.Dimension
import java.awt.RenderingHints
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.image.BufferedImage
import javax.swing.*

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
    private var buildOpts = CollapseMenu("Build opts")
    private var modulesList: JComboBox<String> = JComboBox<String>()
    private var variantsList: JComboBox<String> = JComboBox<String>()
    private var usersList: JComboBox<String> = JComboBox<String>()
    private var selectedUser: String = ""
    private var lastSelectedUser: String = ""
    var selectedModule:String=""
    var selectedVariant:String=""

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

    fun getBuildConfigs(p:Project):DwBuildConfig{
        val cleanBuild = cleanBuildCheckbox.isSelected
        val debugEnabled = debugEnabledCheckBox.isSelected
        val openEmulator = openEmulatorCheckbox.isSelected
        val module: String = selectedModule
        val variant: String = selectedVariant
        this.displayInfo("open emulator is $openEmulator")
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

    private fun addNewUserButton(pwd:String?):JButton{
        val dwWindow:DashwaveWindow = this
        val addUserButton = JButton(createPlusIcon(16,Color.GRAY)).apply {
            toolTipText = "Add user"
            addActionListener{
                loginUser(pwd, dwWindow)
            }
        }
        addUserButton.setSize(10,10)
        return addUserButton
    }

    fun switchUser(user:String, wd:String?){
        val switchUserCmd = DwCmds("user switch $user", wd, true, this)
        val exitCode = switchUserCmd.executeWithExitCode()
        if (exitCode != 0){
            displayError("❌ Could not switch to user: $user\n\n")
            return
        }
        displayInfo("✅ Successfully switched to user: $user\n\n")
    }

    fun addUsers(users: List<String>?,activeUser:String, wd:String?){
        usersList.removeAllItems()

        if(users == null || users.size == 0){
            usersList.isEnabled = false
            usersList.addItem("No users logged in")
            return
        }
        if(PluginStartup.pluginMode != "workspace"){
            usersList.isEnabled = true
        }
        users?.forEach{ user ->
            usersList.addItem(user)
        }
        this.selectedUser = activeUser
        usersList.selectedItem = activeUser

        usersList.addItemListener(ItemListener {
                if (this.selectedUser == it.item.toString()){
                    return@ItemListener
                }
                this.lastSelectedUser = selectedUser
                this.selectedUser = it.item.toString()
                switchUser(this.selectedUser, wd)
        })
    }

    fun addModulesAndVariants(modulesVariants: Map<String, List<String>>, defaultModule: String, defaultVariant: String){
        modulesList.removeAllItems()
        variantsList.removeAllItems()

        if (modulesVariants.size == 0){
            modulesList.addItem("No modules detected yet")
            variantsList.addItem("No variants detected yet")
            modulesList.isEnabled = false
            variantsList.isEnabled = false
            return
        }
        modulesList.isEnabled = true
        variantsList.isEnabled = true

        modulesList.addItem(defaultModule)
        variantsList.addItem(defaultVariant)

        modulesVariants.keys.toTypedArray().forEach { module ->
            modulesList.addItem(module)
        }

        modulesList.addItemListener(ItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                if(it.item.toString().contains("modules detected")){
                    selectedModule = ""
                    return@ItemListener
                }
                selectedModule = modulesList.selectedItem as String
                variantsList.removeAllItems()
                modulesVariants[selectedModule]?.forEach { variant ->
                    variantsList.addItem(variant)
                }
            }
        })

        variantsList.addItemListener(ItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                if(it.item.toString() == "variants detected"){
                    selectedVariant = ""
                    return@ItemListener
                }
                selectedVariant = variantsList.selectedItem as String
            }
        })
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
            FileDocumentManager.getInstance().saveAllDocuments();
            val configs = getBuildConfigs(this.p)
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

        actionToolbar.add(Box.createHorizontalStrut(50))
        val optsGroup = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEtchedBorder()
        }

//        val optionToolbar = JToolBar(JToolBar.VERTICAL)
//        val optionTitle = JLabel("Build Options")
//        optionToolbar.add(optionTitle)
        buildOpts.add(cleanBuildCheckbox)
        buildOpts.add(debugEnabledCheckBox)
        buildOpts.add(openEmulatorCheckbox)

        optsGroup.add(buildOpts.getComponent())

        modulesList.preferredSize = Dimension(20, modulesList.preferredSize.height)
        variantsList.preferredSize = Dimension(20, variantsList.preferredSize.height)
        usersList.preferredSize = Dimension(20, usersList.preferredSize.height)

        // add non-item label to modules and variants
//        optionToolbar.add(JLabel("Modules"))
        optsGroup.add(modulesList)

//        optionToolbar.add(JLabel("Variants"))
        optsGroup.add(variantsList)

        actionToolbar.add(optsGroup)

        val userGroup = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEtchedBorder()
        }

        actionToolbar.add(Box.createHorizontalStrut(300))

        userGroup.add(usersList)

        val addUserBtn = addNewUserButton(this.p.basePath)
        addUserBtn.setSize(10,10)
        userGroup.add(addUserBtn)
        actionToolbar.add(userGroup)
        if(PluginStartup.pluginMode == "workspace") {
            addUserBtn.isEnabled = false
            usersList.isEnabled = false
        }

        openEmulatorCheckbox.isSelected = true



        panel.add(actionToolbar, BorderLayout.NORTH)
//        panel.add(optionToolbar, BorderLayout.WEST)

        // Set up the console view

        panel.add(console.component, BorderLayout.CENTER)

        val content = contentFactory.createContent(panel, "", false)
        window?.setAnchor(ToolWindowAnchor.BOTTOM, null)
        window?.contentManager?.addContent(content)
    }

}

fun createPlusIcon(size: Int, color: Color): Icon {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2d = image.createGraphics()
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.color = color

    // Draw plus sign
    val thickness = size / 8  // Adjust thickness as desired
    val pad = size / 4
    g2d.fillRect(pad, size / 2 - thickness / 2, size - 2 * pad, thickness)
    g2d.fillRect(size / 2 - thickness / 2, pad, thickness, size - 2 * pad)

    g2d.dispose()
    return ImageIcon(image)
}