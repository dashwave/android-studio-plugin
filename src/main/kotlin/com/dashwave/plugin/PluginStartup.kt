package com.dashwave.plugin

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.plugins.performAction
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.impl.ui.Hyperlink
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI.CurrentTheme.ToolWindow
import org.jdesktop.swingx.plaf.LoginPaneUI
import java.awt.Color
import java.awt.Point
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener


class PluginStartup: StartupActivity {

    override fun runActivity(project: Project) {
        val path = project.basePath
        if(path != null){
            val myWindow = ToolWindowManager.getInstance(project).getToolWindow("Dashwave")
            myWindow?.show()
            checkDW(path, project)
        }
    }
}

fun checkDW(pwd: String, project: Project){
    DashwaveWindow.getConsole().clear()
    val dwCmd = "dw"
    try {
        val processBuilder = Process(dwCmd)
        val process = processBuilder.start(false)
        val exitCode = process.waitFor()
        if (exitCode == 0) {
            print("dw is installed successfully")
            DashwaveWindow.displayOutput("✅ dw-cli is installed successfully!\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
            verifyLogin(pwd)
        }else{
            print("dw is not installed")
            DashwaveWindow.show()
            DashwaveWindow.displayOutput("⚠️ dw-cli is not installed\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
            showInstallDW(project)
            var hyperlink = HyperlinkInfo { p: Project ->
                installDW()
            }
            DashwaveWindow.getConsole().printHyperlink("Click here to install dw-cli\n\n", hyperlink)

        }
    } catch (e: Exception) {
        print("issue with running cmd")
        TODO("handle command failure")
    }
}

fun showInstallDW(project: Project){
    val notificationGroup = NotificationGroup.balloonGroup("YourPluginNotificationGroup")

    val notification = notificationGroup.createNotification(
        "Install dw-cli",
        "Please click below to install dw-cli",
        NotificationType.INFORMATION,
        null
    )

    notification.addAction(object : NotificationAction("Install dw-cli") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            installDW()
            notification.expire() // Close the notification after the action is performed
        }
    })

    notification.notify(project)
}

fun installDW(){
    DashwaveWindow.displayOutput("Installing dw-cli ...", ConsoleViewContentType.NORMAL_OUTPUT)
    TODO(
        "not implemented yet"
    )
}
fun verifyLogin(pwd:String){
    val currentUserLoginCmd = "dw user"
    val processBuilder = Process(currentUserLoginCmd)
    val process = processBuilder.start(true)
    val exitCode = process.waitFor()
    if (exitCode == 0){
        if (doesFileExist("$pwd/dashwave.yml")){
            DashwaveWindow.displayOutput("✅ Project is successfully connected to dashwave. Run a cloud build using dashwave icon on toolbar\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
            DashwaveWindow.enableRunButton()

           DashwaveWindow.show()

            val dd = MyCustomDialog()
            dd.show()
        }else {
            DashwaveWindow.displayOutput("⚠️ Project is not connected to dashwave, create a new project\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
            openCreateProjectDialog(pwd)
        }
    }else{
        loginUser(pwd)
    }
}

fun loginUser(pwd:String) {
    val loginUserCmd = "dw login local"
    val processBuilder = Process(loginUserCmd)
    val process = processBuilder.start(true)
    val exitCode = process.waitFor()

    if (exitCode == 0) {
        print("user logged in successfully")

        if (doesFileExist("$pwd/dashwave.yml")){

            DashwaveWindow.enableRunButton()
            print("aagaya yahaa")
            val dd = MyCustomDialog()
            dd.show()
        }else {
            openCreateProjectDialog(pwd)
        }
    }else{
        print("user login failed")
        var hyperlink = HyperlinkInfo { p: Project ->
            loginUser(pwd)
        }
        DashwaveWindow.getConsole().printHyperlink("⚠️ Login failed. Click here to retry", hyperlink)
    }
}
//
//fun showtip() {
//    val notificationGroup = NotificationGroup.balloonGroup("YourPluginNotificationGroup")
//    val notification = notificationGroup.createNotification(
//        "Getting Started with My Plugin",
//        "Click the <strong>My Action</strong> icon in the toolbar to start.",
//        NotificationType.INFORMATION,
//        null
//    )
//    notification.notify(null)
//}



fun doesFileExist(path: String): Boolean {
    val file = File(path)
    return file.exists()
}

fun openCreateProjectDialog(pwd:String){
    val createProjectDialog = CreateProjectDialog()
    if (createProjectDialog.showAndGet()){
        val projectName = createProjectDialog.getInput1()
        val rootDir = createProjectDialog.getInput2()
        val techStack = createProjectDialog.getSelectedOption()
        createProject(projectName, techStack, rootDir,pwd)
    }else{
        DashwaveWindow.show()
        DashwaveWindow.displayOutput("⚠️ Create new project cancelled! You won't be able to run builds on dashwave without creating a project\n\n", ConsoleViewContentType.ERROR_OUTPUT)
//        DashwaveWindow.displayOutput("Click here to create a new dashwave project", ConsoleViewContentType.NORMAL_OUTPUT)
        var hyperlink = HyperlinkInfo { p: Project ->
            openCreateProjectDialog(pwd)
        }
        DashwaveWindow.getConsole().printHyperlink("Click here to create a new dashwave project\n\n", hyperlink)
    }
}

fun createProject(projectName: String, devStack:String, rootDir:String, pwd:String) {
    val createProjectCmd = "dw create-project --no-prompt --pwd=$pwd --name=$projectName --dev-stack=$devStack --root-dir=$rootDir"
    val processBuilder = Process(createProjectCmd)
    val process = processBuilder.start(true)
    val exitCode = process.waitFor()
    if(exitCode == 0){
        print("project created successfully")
        DashwaveWindow.enableRunButton()
        Notifications.Bus.notify(
            Notification(
                "YourPluginNotificationGroup",
                "Project configured with dashwave",
                "Your project is successfully connected to dashwave. Run DW Build from toolbar",
                NotificationType.INFORMATION
            )
        )
        DashwaveWindow.show()
        DashwaveWindow.displayOutput("✅ Project is successfully connected to dashwave. Run a cloud build using dashwave icon on toolbar\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        DashwaveWindow.enableRunButton()
        val dd = MyCustomDialog()
        dd.show()
    }else{
        DashwaveWindow.displayOutput("❌ Dashwave project creation failed\n", ConsoleViewContentType.ERROR_OUTPUT)
        var hyperlink = HyperlinkInfo { p: Project ->
            openCreateProjectDialog(pwd)
        }
        DashwaveWindow.getConsole().printHyperlink("Please try again\n\n", hyperlink)
    }
}