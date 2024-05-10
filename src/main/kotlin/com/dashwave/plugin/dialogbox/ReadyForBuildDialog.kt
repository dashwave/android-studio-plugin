package com.dashwave.plugin.dialogbox

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Action

class ReadyForBuildDialog : DialogWrapper(true) {
    init {
        init()
        title = "Project Created Successfully"
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()

        // Load the image icon
        val icon = IconLoader.getIcon("/icons/dashwave13.svg", ReadyForBuildDialog::class.java.classLoader)
        val iconLabel = JLabel(icon)

        // Add components to the panel
        dialogPanel.add(JLabel("Click on"))
        dialogPanel.add(iconLabel)
        dialogPanel.add(JLabel("run build on dashwave button in the toolbar to start your build"))
        return dialogPanel
    }

    override fun createActions(): Array<Action> {
        val okButton = okAction
        return arrayOf(okButton)
    }
}