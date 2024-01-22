package com.dashwave.plugin

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JTextField
import javax.swing.JComponent
import com.intellij.ui.layout.panel

class CreateProjectDialog : DialogWrapper(true) {
    private val textField1 = JTextField(20)
    private val textField2 = JTextField("./",20)
    private val comboBox = ComboBox(arrayOf("GRADLE", "FLUTTER", "RNATIVE"))

    init {
        title = "Create a new dashwave project"
        comboBox.selectedIndex = 0
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return panel {
            row("Project Name:") { textField1() }
            row("Path to root module (relative to project root dir):") { textField2() }
            row("Tech Stack:") { comboBox() }
        }
    }

    fun getInput1(): String = textField1.text
    fun getInput2(): String = textField2.text
    fun getSelectedOption(): String = comboBox.selectedItem as String
}