package com.dashwave.plugin.dialogbox

import com.dashwave.plugin.messages.Messages
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.Action

class GitNotConfiguredDialog : DialogWrapper(true) {
    init {
        init()
        title = "Git not configured"
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()
        dialogPanel.add(JLabel(Messages.GIT_NOT_CONFIGURED))
        return dialogPanel
    }

    override fun createActions(): Array<Action> {
        val okButton = okAction
        return arrayOf(okButton)
    }
}