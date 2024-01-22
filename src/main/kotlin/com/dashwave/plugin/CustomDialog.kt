package com.dashwave.plugin

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class MyCustomDialog : DialogWrapper(true) {
    init {
        init()
        title = "DW Build"
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()

        // Load the image icon
        val icon = IconLoader.getIcon("/icons/dashwave13.svg")
        val iconLabel = JLabel(icon)

        // Add components to the panel
        dialogPanel.add(iconLabel)
        dialogPanel.add(JLabel("Click the 'dw build' icon in the toolbar to start a cloud build."))

        return dialogPanel
    }
}