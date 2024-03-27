package com.dashwave.plugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


class LoginDialog : DialogWrapper(true) {
    private val textField1 = JTextField(20)


    init {
        title = "Log in to Dashwave Cloud"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()

//        val linkLabel = JLabel("<html><a href=''>DashwaveDashwaveDashwaveDashwaveDashwaveDashwaveDashwave</a></html>")
//        linkLabel.cursor = Cursor(Cursor.HAND_CURSOR) // Change the cursor to a hand cursor
//
//        linkLabel.addMouseListener(object : MouseAdapter() {
//            override fun mouseClicked(e: MouseEvent?) {
//                BrowserUtil.browse("http://dashwave.io")
//            }
//
//            override fun mouseEntered(e: MouseEvent?) {
//                linkLabel.text = "<html><a href='' style='color: #0000FF;'><u>Dashwave</u></a></html>" // Underline on hover
//            }
//
//            override fun mouseExited(e: MouseEvent?) {
//                linkLabel.text = "<html><a href=''>Dashwave</a></html>" // Remove underline when not hovering
//            }
//        })
//
//        dialogPanel.add(linkLabel)
//
//        // add a new line
//        dialogPanel.add(JLabel(""))
//        dialogPanel.add(JLabel(""))

        // Load the image icon
//        val icon = IconLoader.getIcon("/icons/dashwave13.svg")
//        val iconLabel = JLabel(icon)

        // Add components to the panel
//        dialogPanel.add(iconLabel)

        // Add label to the text field
        dialogPanel.add(JLabel("Access Code:"))
        dialogPanel.add(textField1)

        if (textField1.text.isEmpty()) {
            setErrorText("Please enter your access code")
        }

        // change the ok button to login
        setOKButtonText("Login")

        return dialogPanel
    }

    fun getAccessCode(): String {
        if (textField1.text.isEmpty()) {
            return ""
        }
        return textField1.text
    }
}

