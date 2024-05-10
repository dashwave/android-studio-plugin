package com.dashwave.plugin.dialogbox

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.layout.panel
import java.awt.*
import javax.swing.*

class CreateProjectDialog(project: Project): DialogWrapper(project) {
    private val projectNameTextField = JTextField()
    private val rootModulePathTextField = JTextField("./")
    private val nativeRadioButton = JRadioButton("Native (Java/Kotlin)")
    private val rNativeRadioButton = JRadioButton("RNative")
    private val flutterRadioButton = JRadioButton("Flutter")
    private val projectTypeButtonGroup = ButtonGroup()

    init {
        init()
        title = "New Dashwave Project"
        projectTypeButtonGroup.add(nativeRadioButton)
        projectTypeButtonGroup.add(flutterRadioButton)
        projectTypeButtonGroup.add(rNativeRadioButton)
        nativeRadioButton.isSelected = true
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.insets = Insets(4, 4, 4, 4) // You can adjust padding here
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        val icon = IconLoader.getIcon("/icons/dashwave13.svg", CreateProjectDialog::class.java.classLoader) // Make sure to provide the correct path
        val labelWithIcon = JLabel("Create a new project to be able to run builds on dashwave", icon, SwingConstants.LEFT)
        panel.add(labelWithIcon, gbc)

        gbc.gridy+=2
        gbc.gridwidth = 1
        panel.add(JLabel("Project name*:"), gbc)

        gbc.gridx++
        gbc.gridwidth = 1
        projectNameTextField.preferredSize = Dimension(200, projectNameTextField.preferredSize.height) // Adjust width as needed
        panel.add(projectNameTextField, gbc)

        gbc.gridx = 0
        gbc.gridy+=2
        gbc.gridwidth = 1
        panel.add(JLabel("Root module path:"), gbc)

        gbc.gridx++
        gbc.gridwidth = 1
        rootModulePathTextField.preferredSize = Dimension(200, projectNameTextField.preferredSize.height)
        panel.add(rootModulePathTextField, gbc)

        gbc.gridx = 0
        gbc.gridy+=2
        gbc.gridwidth = 1
        panel.add(JLabel("Project Type*:"), gbc)

        gbc.gridy+=2
        val projectTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        projectTypePanel.add(nativeRadioButton)
        projectTypePanel.add(rNativeRadioButton)
        projectTypePanel.add(flutterRadioButton)
        panel.add(projectTypePanel, gbc)

        return panel
    }

    fun getProjectName():String{
        return projectNameTextField.text
    }

    fun getRootDir():String{
        return rootModulePathTextField.text
    }

    fun getSelectedTechStack():String{
        return when{
            nativeRadioButton.isSelected -> "GRADLE"
            flutterRadioButton.isSelected -> "FLUTTER"
            rNativeRadioButton.isSelected -> "REACTNATIVE"
            else -> ""
        }
    }
}