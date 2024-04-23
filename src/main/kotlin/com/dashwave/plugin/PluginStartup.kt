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
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.serialization.json.*
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
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ServiceManager
import io.ktor.util.*
import java.awt.Font

var PluginMode:String = ""
var PluginEnv:String = ""
class PluginStartup: StartupActivity {
    companion object {
        var dwWindows:HashMap<String, DashwaveWindow> = HashMap()
        var pluginMode:String = ""
        var pluginEnv:String = ""
    }

    init {
        val pluginConfig = PluginConfiguration.getInstance()
        pluginMode = pluginConfig.state.pluginMode
        pluginEnv = pluginConfig.state.pluginEnv
    }

    override fun runActivity(project: Project) {
        val dwWindow = DashwaveWindow(project)
        dwWindow.show()
        dwWindows[project.name] = dwWindow
        checkDW(project, dwWindow)
        PluginMode = pluginMode
        PluginEnv = pluginEnv
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
    process.start(false)
    Thread{
        val exitCode = process.wait()
        if (exitCode == 0) {
            dwWindow.displayInfo(Messages.DW_DEPS_INSTALL_SUCCESS)
            dwWindow.displayInfo(Messages.DW_DEPS_CONFIGURING)

            val configCmd = DwCmds("config", pwd, true, dwWindow)
            val exitcode = configCmd.executeWithExitCode()
            if(exitcode == 0){
                if(PluginMode == "workspace"){
                    dwWindow.displayInfo("🔨 Setting up workspace plugin...\n")
                    var workspaceCmd = "setup-workspace"
                    if(PluginEnv != ""){
                        workspaceCmd += " -e ${PluginEnv}"
                    }
                    val setupWorkspaceCmd = DwCmds(workspaceCmd, pwd, true, dwWindow)
                    val exitCode = setupWorkspaceCmd.executeWithExitCode()
                    if(exitCode == 0){
                        verifyLogin(pwd, dwWindow)
                    }else{
                        dwWindow.displayError("❌ Could not setup plugin. Please contact us at hello@dashwave.io")
                    }
                    return@Thread
                }

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
    listUsers(pwd, dwWindow)
    dwWindow.addModulesAndVariants(HashMap<String,List<String>>(), "", "")
    val currentUserLoginCmd = DwCmds("user", pwd, true, dwWindow)
    val exitCode = currentUserLoginCmd.executeWithExitCode()
    if (exitCode == 0){
        dwWindow.enableRunButton()
        checkProjectConnected(pwd, dwWindow)
    }else{
        if(PluginMode == "workspace") {
            dwWindow.displayError("❌ User is not setup correctly. Please contact us at hello@dashwave.io")
            return
        }
        loginUser(pwd, dwWindow)
    }
}

fun checkProjectConnected(pwd:String?, dwWindow: DashwaveWindow){
    listUsers(pwd, dwWindow)
    // check if .git folder exists
    val gitConfigFilepath = "$pwd/.git"
    if (!doesFileExist(gitConfigFilepath)){
        if(PluginMode == "workspace"){
            dwWindow.displayError("❌ There is some issue in setting up your project (.git doesn't exist), please contact us at hello@dashwave.io")
            return
        }
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
        listModulesAndVariants(pwd, dwWindow)
        dwWindow.displayOutput("✅ Project is successfully connected to dashwave. Run a cloud build using dashwave icon on toolbar\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        val dd = ReadyForBuildDialog()
        dd.show()
    }else {
        if(PluginMode == "worksapce"){
            dwWindow.displayError("❌ There is some issue in setting up your project (dashwave.yml doesn't exist), please contact us at hello@dashwave.io")
            return
        }
        dwWindow.displayOutput("⚠️ This project is not connected to dashwave, create a new project on dashwave\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        openCreateProjectDialog(pwd, true, dwWindow){}
    }
}

fun loginUser(pwd:String?, dwWindow: DashwaveWindow) {
    val loginDialog = LoginDialog()
    var accessCode: String = ""

    loginDialog.show()

    if (loginDialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
        accessCode = loginDialog.getAccessCode()
    }else if (loginDialog.exitCode == DialogWrapper.CANCEL_EXIT_CODE){
        // handle cancellation logic if any
        return
    }

    var loginUserCmd = "login $accessCode"
    if(PluginEnv != ""){
        loginUserCmd += " -e $PluginEnv"
    }
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

fun listUsers(pwd: String?, dwWindow: DashwaveWindow){
    val usersCmd = DwCmds("user ls", pwd, false, dwWindow)
    val cmdOutput = usersCmd.executeWithOutput()
    if(cmdOutput.first != 0){
        dwWindow.displayError("❌ Could not find logged in users\n"+cmdOutput.second)
        return
    }
    val jsonText = cmdOutput.second.trim()
    val cleanedJsonString = jsonText.dropWhile { it.code <= 32 }
    println(cleanedJsonString)
    val jsonObject = Json.parseToJsonElement(cleanedJsonString).jsonObject

    val users = jsonObject["users"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
    val activeUser = jsonObject["active_user"]?.toString()

    dwWindow.addUsers(users, activeUser?:"",pwd)
}
fun listModulesAndVariants(pwd:String?, dwWindow: DashwaveWindow) {
    val configsCmd = DwCmds("build configs", pwd, false, dwWindow)
    var cmdOutput = configsCmd.executeWithOutput()
    if(cmdOutput.first != 0){
        dwWindow.displayError("❌ Could not find modules and in variants\n"+cmdOutput.second)
        return
    }
    val jsonText = cmdOutput.second.trim()
    val cleanedJsonString = jsonText.dropWhile { it.code <= 32 }
    val jsonObject = Json.parseToJsonElement(cleanedJsonString).jsonObject
    val map = mutableMapOf<String, List<String>>()
    var defaultModule: String = ""
    var defaultVariant: String = ""
    var foundDefault:Boolean = false
    jsonObject.forEach { (key, value) ->
        val list = value.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
        if (key == "default"){
            if (list.size >= 2){
                defaultModule = list[0]
                defaultVariant = list[1]
                foundDefault = true
            } else {
                println("Warning: 'default' project does not contain enough build types.")
            }
        }
        map[key] = list
    }

    if (!foundDefault && map.isNotEmpty()) {
        map.entries.first().let { firstEntry ->
            defaultModule = firstEntry.key
            defaultVariant = firstEntry.value.firstOrNull() ?: ""
        }
    }

    dwWindow.addModulesAndVariants(map, defaultModule, defaultVariant)
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