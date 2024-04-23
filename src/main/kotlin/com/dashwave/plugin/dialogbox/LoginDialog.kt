package com.dashwave.plugin

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


class LoginDialog : DialogWrapper(true) {
    private val accessCodeTextField = JPasswordField(20)


    init {
        // TODO: add logo to title
        title = "Authenticate with Dashwave Cloud"
        init()

//        val icon = IconLoader.getIcon("/icons/dashwave13.svg") // Make sure to provide the correct path
//        val image = IconUtil.toImage(icon)
//        window.setIconImage(image)
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.insets = Insets(4, 4, 4, 4) // You can adjust padding here
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2

        this.createTitlePane()

//        val labelWithIcon = JLabel("Login to Dashwave Cloud by entering your access code", icon, SwingConstants.LEFT)
//        panel.add(labelWithIcon, gbc)

        val linkHeadLabel = JLabel("Authenticate the plugin with Dashwave cloud using your personal access code")
        // TODO: add - to view your access code, visit this link: <link>
        gbc.gridy+=20
        gbc.gridx = 0
        gbc.gridwidth = 2
        linkHeadLabel.preferredSize = Dimension(600, linkHeadLabel.preferredSize.height+10)
        panel.add(linkHeadLabel, gbc)

        val label = JLabel("To view your access code, visit this link:")
        gbc.gridy+=20
        gbc.gridwidth = 2
        panel.add(label, gbc)

        val linkLabel = JLabel("<html><a href=''>https://console.dashwave.io/home?profile=true</a></html>")
        linkLabel.cursor = Cursor(Cursor.HAND_CURSOR) // Change the cursor to a hand cursor

        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                BrowserUtil.browse("https://console.dashwave.io/home?profile=true")
            }

            override fun mouseEntered(e: MouseEvent?) {
                linkLabel.text = "<html><a href=''>https://console.dashwave.io/home?profile=true</a></html>" // Underline on hover
            }

            override fun mouseExited(e: MouseEvent?) {
                linkLabel.text = "<html><a href=''>https://console.dashwave.io/home?profile=true</a></html>" // Remove underline when not hovering
            }
        })

        gbc.gridx = 0
        gbc.gridy+=1
        gbc.gridwidth = 10
        linkLabel.preferredSize = Dimension(600, linkLabel.preferredSize.height)
        panel.add(linkLabel, gbc)

        gbc.gridy+=200
        gbc.gridwidth = 1
        panel.add(JLabel("Access Code:"), gbc)

        gbc.gridx++
        gbc.gridwidth = 1
        accessCodeTextField.preferredSize = Dimension(200, accessCodeTextField.preferredSize.height) // Adjust width as needed
        panel.add(accessCodeTextField, gbc)

//        if (accessCodeTextField.text.isEmpty()) {
//            setErrorText("Please enter your access code")
//        }

        // change the ok button to login
        setOKButtonText("Authenticate")

        return panel
    }

    fun getAccessCode(): String {
        if (accessCodeTextField.text.isEmpty()) {
            return ""
        }
        return accessCodeTextField.text
    }
}

