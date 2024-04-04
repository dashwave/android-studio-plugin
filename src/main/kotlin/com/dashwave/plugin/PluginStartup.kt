package com.dashwave.plugin

import com.dashwave.plugin.dialogbox.CreateProjectDialog
import com.dashwave.plugin.dialogbox.ReadyForBuildDialog
import com.dashwave.plugin.messages.Messages
import com.dashwave.plugin.utils.DwCmds
import com.dashwave.plugin.utils.Process
import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.*
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.serialization.json.*
import java.awt.Color
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.awt.Font

class PluginStartup: StartupActivity {

    override fun runActivity(project: Project) {
        DashwaveWindow.p = project
        DashwaveWindow.show()
        checkDW(project)
    }
}

fun checkDW(project: Project) {
    val dwCmd = DwCmds("check-update", "", true)
    val exitCode = dwCmd.executeWithExitCode()
    if (exitCode == 0) {
        DashwaveWindow.displayInfo(Messages.DW_INSTALLED_ALREADY)
        verifyLogin(project?.basePath)
    }else if(exitCode == 11){
    }else{
        DashwaveWindow.displayInfo(Messages.DW_NOT_INSTALLED)
        showInstallDW(project)
        installDW(project?.basePath)
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

fun installDW(pwd: String?){
    DashwaveWindow.displayOutput("🔨 Setting up plugin...\n\n", ConsoleViewContentType.NORMAL_OUTPUT)

    // Execute the script
    val process = Process("curl -sSL https://cli.dashwave.io | bash", pwd, true)
    process.start(false)
    Thread{
        val exitCode = process.wait()
        if (exitCode == 0) {
            DashwaveWindow.displayInfo(Messages.DW_DEPS_INSTALL_SUCCESS)
            DashwaveWindow.displayInfo(Messages.DW_DEPS_CONFIGURING)
            val configCmd = DwCmds("config", pwd, true)
            val exitcode = configCmd.executeWithExitCode()
            if(exitcode == 0){
                DashwaveWindow.displayInfo(Messages.DW_DEPS_CONFIGURE_SUCCESS)
                verifyLogin(pwd)
            }else{
                DashwaveWindow.displayError(Messages.DW_DEPS_CONFIGURE_FAILED)
            }
        } else {
            DashwaveWindow.displayError(Messages.DW_DEPS_INSTALL_FAILED)
        }
    }.start()
}

fun verifyLogin(pwd:String?){
    listUsers(pwd)
    DashwaveWindow.addModulesAndVariants(HashMap<String,List<String>>(), "", "")
    val currentUserLoginCmd = DwCmds("user", "", true)
    val exitCode = currentUserLoginCmd.executeWithExitCode()
    if (exitCode == 0){
        DashwaveWindow.enableRunButton()
        checkProjectConnected(pwd)
    }else{
        loginUser(pwd)
    }
}

fun checkProjectConnected(pwd:String?){
    listUsers(pwd)
    if (doesFileExist("$pwd/dashwave.yml")){
        DashwaveWindow.enableRunButton()

        listModulesAndVariants(pwd)

        DashwaveWindow.displayOutput("✅ Project is successfully connected to dashwave. Run a cloud build using dashwave icon on toolbar\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        val dd = ReadyForBuildDialog()
        dd.show()
    }else {
        DashwaveWindow.displayOutput("⚠️ This project is not connected to dashwave, create a new project on dashwave\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
        openCreateProjectDialog(pwd, true)
    }
}

fun loginUser(pwd:String?) {
    val loginDialog = LoginDialog()
    var accessCode: String = ""

    loginDialog.show()

    if (loginDialog.exitCode == DialogWrapper.OK_EXIT_CODE) {
        accessCode = loginDialog.getAccessCode()
    }else if (loginDialog.exitCode == DialogWrapper.CANCEL_EXIT_CODE){
        // handle cancellation logic if any
        return
    }

    val loginUserCmd = "login $accessCode"
    val exitCode = DwCmds(loginUserCmd, "", true).executeWithExitCode()
    if (exitCode == 0) {
        checkProjectConnected(pwd)
    }else{
        var hyperlink = HyperlinkInfo { p: Project ->
            loginUser(pwd)
        }
        DashwaveWindow.console.printHyperlink(Messages.DW_LOGIN_FAILED, hyperlink)
    }
}

fun doesFileExist(path: String): Boolean {
    val file = File(path)
    return file.exists()
}

fun listUsers(pwd: String?){
    val usersCmd = DwCmds("user ls", pwd, false)
    val cmdOutput = usersCmd.executeWithOutput()
    if(cmdOutput.first != 0){
        DashwaveWindow.displayError("❌ Could not find logged in users\n"+cmdOutput.second)
        return
    }
    val jsonText = cmdOutput.second.trim()
    val cleanedJsonString = jsonText.dropWhile { it.code <= 32 }
    println(cleanedJsonString)
    val jsonObject = Json.parseToJsonElement(cleanedJsonString).jsonObject

    val users = jsonObject["users"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
    val activeUser = jsonObject["active_user"]?.toString()

    DashwaveWindow.addUsers(users, activeUser?:"",pwd)
}
fun listModulesAndVariants(pwd:String?) {
    val configsCmd = DwCmds("build configs", pwd, false)
    var cmdOutput = configsCmd.executeWithOutput()
    if(cmdOutput.first != 0){
        DashwaveWindow.displayError("❌ Could not find modules and in variants\n"+cmdOutput.second)
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

    DashwaveWindow.addModulesAndVariants(map, defaultModule, defaultVariant)
}

fun openCreateProjectDialog(pwd:String?, openTip:Boolean):Boolean{
    val createProjectDialog = CreateProjectDialog()
    if (createProjectDialog.showAndGet()){
        val projectName = createProjectDialog.getProjectName()
        val rootDir = createProjectDialog.getRootDir()
        val techStack = createProjectDialog.getSelectedTechStack()
        val success = createProject(projectName, techStack, rootDir,pwd, openTip)
        return success
    }
    DashwaveWindow.show()
    val yellowOutput = ConsoleViewContentType("YellowOutput", TextAttributes(Color.YELLOW, null,null, null, Font.PLAIN))
    DashwaveWindow.displayOutput("⚠️ You must create a new project to be able to run builds on dashwave\n\n", yellowOutput)
    var hyperlink = HyperlinkInfo { p: Project ->
        openCreateProjectDialog(pwd, openTip)
    }
    DashwaveWindow.console.printHyperlink("Click here", hyperlink)
    DashwaveWindow.displayOutput(" to create a new dashwave project\n\n", ConsoleViewContentType.NORMAL_OUTPUT)
    return false
}

fun createProject(projectName: String, devStack:String, rootDir:String,pwd:String?, openTip: Boolean) :Boolean{
    val createProjectCmd = "create-project --no-prompt --name=$projectName --dev-stack=$devStack --root-dir=$rootDir"
    val exitCode = DwCmds(createProjectCmd, pwd, true).executeWithExitCode()
    if(exitCode == 0){
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
        DashwaveWindow.displayInfo(Messages.PROJECT_CONNECTION_SUCCESS)

        if(openTip) {
            val dd = ReadyForBuildDialog()
            dd.show()
        }
        return true
    }
    DashwaveWindow.displayError(Messages.PROJECT_CONNECTION_FAILED)
    var hyperlink = HyperlinkInfo { p: Project ->
        openCreateProjectDialog(pwd, openTip)
    }
    DashwaveWindow.console.printHyperlink("Please try again\n\n", hyperlink)
    return false
}