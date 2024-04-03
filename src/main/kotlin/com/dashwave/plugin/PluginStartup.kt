package com.dashwave.plugin

import com.dashwave.plugin.actions.BuildAction
import com.dashwave.plugin.dialogbox.CreateProjectDialog
import com.dashwave.plugin.dialogbox.GitNotConfiguredDialog
import com.dashwave.plugin.dialogbox.ReadyForBuildDialog
import com.dashwave.plugin.messages.Messages
import com.dashwave.plugin.notif.BalloonNotif
import com.dashwave.plugin.utils.DwCmds
import com.dashwave.plugin.utils.Process
import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Color
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import io.ktor.util.*
import java.awt.Font

class PluginStartup: StartupActivity {
    companion object {
        var dwWindows:HashMap<String, DashwaveWindow> = HashMap()
    }

    override fun runActivity(project: Project) {
        val dwWindow = DashwaveWindow(project)
        dwWindow.show()
        dwWindows[project.name] = dwWindow
        checkDW(project, dwWindow)
    }
}

fun checkDW(project: Project, dwWindow: DashwaveWindow) {
    val dwCmd = DwCmds("check-update", project.basePath, true, dwWindow)
    val exitCode = dwCmd.executeWithExitCode()
    if (exitCode == 0) {
        dwWindow.displayInfo(Messages.DW_INSTALLED_ALREADY)
        verifyLogin(project?.basePath, dwWindow)
    }else if(exitCode == 11){
    }else{
        dwWindow.displayInfo(Messages.DW_NOT_INSTALLED)
        showInstallDW(project)
        installDW(project?.basePath, dwWindow)
    }
}

fun showInstallDW(project: Project){
    val notificationGroup = NotificationGroup.balloonGroup("YourPluginNotificationGroup")
    val notification = notificationGroup.createNotification(
        "Dashwave Plugin Not Configured",
        "Installing and configuring deps and modules for plugin",
        NotificationType.INFORMATION,
        null
    )
    notification.notify(project)
}

fun installDW(pwd: String?, dwWindow: DashwaveWindow){
    dwWindow.displayOutput("🔨 Setting up plugin...\n\n", ConsoleViewContentType.NORMAL_OUTPUT)

    // Execute the script
    val process = Process("curl -sSL https://cli.dashwave.io | bash", pwd, true, dwWindow)
    process.start()
    Thread{
        val exitCode = process.wait()
        if (exitCode == 0) {
            dwWindow.displayInfo(Messages.DW_DEPS_INSTALL_SUCCESS)
            dwWindow.displayInfo(Messages.DW_DEPS_CONFIGURING)
            val configCmd = DwCmds("config", pwd, true, dwWindow)
            val exitcode = configCmd.executeWithExitCode()
            if(exitcode == 0){
                dwWindow.displayInfo(Messages.DW_DEPS_CONFIGURE_SUCCESS)
                verifyLogin(pwd, dwWindow)
            }else{
                dwWindow.displayError(Messages.DW_DEPS_CONFIGURE_FAILED)
            }
        } else {
            dwWindow.displayError(Messages.DW_DEPS_INSTALL_FAILED)
        }
    }.start()
}

fun verifyLogin(pwd:String?, dwWindow: DashwaveWindow){
    val currentUserLoginCmd = DwCmds("user", pwd, true, dwWindow)
    val exitCode = currentUserLoginCmd.executeWithExitCode()
    if (exitCode == 0){
        dwWindow.enableRunButton()
        checkProjectConnected(pwd, dwWindow)
    }else{
        loginUser(pwd, dwWindow)
    }
}

fun checkProjectConnected(pwd:String?, dwWindow: DashwaveWindow){
    // check if .git folder exists
    val gitConfigFilepath = "$pwd/.git"
    if (!doesFileExist(gitConfigFilepath)){
        dwWindow.displayOutput("❌ ${Messages.GIT_NOT_CONFIGURED}", ConsoleViewContentType.ERROR_OUTPUT)
        val notif = BalloonNotif(
            "Could not find .git folder",
            "",
            "This is not a git repository, initialise git and push codebase to proceed ",
            NotificationType.ERROR,
        ){}
        notif.show(dwWindow.p)

        val dialog = GitNotConfiguredDialog()
        dialog.show()
        return
    }

    if (doesFileExist("$pwd/dashwave.yml")){
        dwWindow.enableRunButton()
        dwWindow.displayOutput("✅ Project is successfully connected to dashwave. Run a cloud build using dashwave icon on toolbar\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        val dd = ReadyForBuildDialog()
        dd.show()
    }else {
        dwWindow.displayOutput("⚠️ This project is not connected to dashwave, create a new project on dashwave\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        openCreateProjectDialog(pwd, true, dwWindow){}
    }
}

fun loginUser(pwd:String?, dwWindow: DashwaveWindow) {
    val loginUserCmd = "login"
    val exitCode = DwCmds(loginUserCmd, pwd, true, dwWindow).executeWithExitCode()
    if (exitCode == 0) {
        dwWindow.enableRunButton()
        checkProjectConnected(pwd, dwWindow)
    }else{
        var hyperlink = HyperlinkInfo { p: Project ->
            loginUser(pwd, dwWindow)
        }
        dwWindow.console.printHyperlink(Messages.DW_LOGIN_FAILED, hyperlink)
    }
}

fun doesFileExist(path: String): Boolean {
    val file = File(path)
    return file.exists()
}

fun openCreateProjectDialog(pwd:String?, openTip:Boolean, dwWindow: DashwaveWindow,buildAction:()->Unit){
    ApplicationManager.getApplication().invokeLater{
        val createProjectDialog = CreateProjectDialog(dwWindow.p)
        if (createProjectDialog.showAndGet()){
            val projectName = createProjectDialog.getProjectName()
            val rootDir = createProjectDialog.getRootDir()
            val techStack = createProjectDialog.getSelectedTechStack()
            val success = createProject(projectName, techStack, rootDir,pwd, openTip, dwWindow)
            if(success){
                buildAction()
            }
        }else{
//            dwWindow.show()
            dwWindow.enableRunButton()
            dwWindow.disableCancelButton()
            val yellowOutput = ConsoleViewContentType("YellowOutput", TextAttributes(Color.YELLOW, null,null, null, Font.PLAIN))
            dwWindow.displayOutput("⚠️ You must create a new project to be able to run builds on dashwave\n\n", yellowOutput)
            var hyperlink = HyperlinkInfo { p: Project ->
                openCreateProjectDialog(pwd, openTip, dwWindow,buildAction)
            }
            dwWindow.console.printHyperlink("Click here", hyperlink)
            dwWindow.displayOutput(" to create a new dashwave project\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        }
    }
}

fun createProject(projectName: String, devStack:String, rootDir:String,pwd:String?, openTip: Boolean, dwWindow: DashwaveWindow) :Boolean{
    val createProjectCmd = "create-project --no-prompt --name=$projectName --dev-stack=$devStack --root-dir=$rootDir"
    val exitCode = DwCmds(createProjectCmd, pwd, true, dwWindow).executeWithExitCode()
    if(exitCode == 0){
        dwWindow.enableRunButton()
        Notifications.Bus.notify(
            Notification(
                "YourPluginNotificationGroup",
                "Project configured with dashwave",
                "Your project is successfully connected to dashwave. Run DW Build from toolbar",
                NotificationType.INFORMATION
            )
        )
//        dwWindow.show()
        dwWindow.displayInfo(Messages.PROJECT_CONNECTION_SUCCESS)

        if(openTip) {
            val dd = ReadyForBuildDialog()
            dd.show()
        }
        return true
    }
    dwWindow.displayError(Messages.PROJECT_CONNECTION_FAILED)
    if(exitCode == 13){
        var hyperlink = HyperlinkInfo { p: Project ->
            loginUser(pwd, dwWindow)
        }
        dwWindow.console.printHyperlink("Login here\n\n", hyperlink)
        return false
    }
    var hyperlink = HyperlinkInfo { p: Project ->
        openCreateProjectDialog(pwd, openTip, dwWindow){}
    }
    dwWindow.console.printHyperlink("Please try again\n\n", hyperlink)
    return false
}