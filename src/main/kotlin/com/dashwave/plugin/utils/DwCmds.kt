package com.dashwave.plugin.utils

import com.dashwave.plugin.installDW
import com.dashwave.plugin.listModulesAndVariants
import com.dashwave.plugin.listUsers
import com.dashwave.plugin.loginUser
import com.dashwave.plugin.notif.BalloonNotif
import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class DwCmds(execCmd:String, wd:String?, log: Boolean, dwWindow: DashwaveWindow){
    private var cmd:String
    private var p:Process
    private var pwd:String?
    private var shouldLog:Boolean
    private var dwWindow:DashwaveWindow
    init {
        shouldLog = log
        cmd = "dw $execCmd --plugin"
        pwd = wd
        p = com.dashwave.plugin.utils.Process(cmd, pwd, log, dwWindow)
        this.dwWindow = dwWindow
    }

    fun executeWithExitCode():Int{
        this.p.start(this.shouldLog)
        val exitCode = this.p.wait()
        if(exitCode == 11){
            this.dwWindow.displayError("Dashwave has a major update, you need to update dependencies\n")
            val hyperlink = HyperlinkInfo { p: Project ->
                installDW(this.pwd, this.dwWindow)
            }
            this.dwWindow.console.printHyperlink("Click here to update\n\n", hyperlink)
        }
        return exitCode
    }

    fun executeWithOutput():Pair<Int,String>{
        this.p.start(this.shouldLog)
        val exitCode = this.p.wait()
        if(exitCode == 11){
            this.dwWindow.displayError("Dashwave has a major update, you need to update dependencies\n")
            val hyperlink = HyperlinkInfo { p: Project ->
                installDW(this.pwd, this.dwWindow)
            }
            this.dwWindow.console.printHyperlink("Click here to update\n\n", hyperlink)
        }

        // get the stdout as string
        return Pair(exitCode, this.p.getOutput())
    }


    fun exit(){
        this.p.exit()
    }

    fun executeBg(){
        Thread{
            this.p.start(this.shouldLog)
        }.start()
    }


    fun executeBuild(pwd:String?, openEmulator:Boolean, attachDebugger:Boolean){
        this.p.start(this.shouldLog)
        this.dwWindow.disableRunButton()
        this.dwWindow.enableCancelButton()
        this.dwWindow.currentBuild = this
        BalloonNotif(
            "Build started",
            "",
            "Build started on dashwave. Your build is running on a remote machine. You can view the logs in console and view emulation after build completes",
            NotificationType.INFORMATION
        ){}.show(dwWindow.p)
        this.dwWindow.changeIcon(this.dwWindow.loadIcon)
        Thread{
            var ex = this.p.wait()
            this.dwWindow.currentBuild = null
            this.dwWindow.changeIcon(this.dwWindow.dwIcon)
            this.dwWindow.enableRunButton()
            this.dwWindow.disableCancelButton()
            listModulesAndVariants(pwd, this.dwWindow)
//          listUsers(pwd, this.dwWindow)
            this.dwWindow.show()
            when(ex){
                0 -> {
                    BalloonNotif(
                        "Build Successful",
                        "",
                        "Build completed successfully!",
                        NotificationType.INFORMATION
                    ){
//                        BrowserUtil.browse("https://console.dashwave.io/home?profile=true")
                    }.show(dwWindow.p)

                    if(!attachDebugger && openEmulator){
                        val emulatorCmd = DwCmds("emulator", pwd, false, this.dwWindow)
                        this.dwWindow.lastEmulatorProcess = emulatorCmd
                        val ex = emulatorCmd.executeWithExitCode()
                    }
                }
                11 -> {
                    this.dwWindow.displayError("Dashwave has a major update, you need to update dependencies\n")
                    val hyperlink = HyperlinkInfo { p: Project ->
                        installDW(pwd, this.dwWindow)
                    }
                    this.dwWindow.console.printHyperlink("Click here to update\n\n", hyperlink)
                }
                // exitcode = 12 means authorize scm failed
                12 -> {
                    BalloonNotif(
                        "Configuration Failed",
                        "Authorize Git",
                        "We could not fetch your project from github/gitlab. Please authorize to provide access",
                        NotificationType.ERROR
                    ){
                        BrowserUtil.browse("https://consoledev.dashwave.io/home?profile=true")
                    }.show(dwWindow.p)
                }
                13 -> {
                    BalloonNotif(
                        "Not Authorized",
                        "Login here",
                        "Your auth token may have expired. Click here to login again",
                        NotificationType.ERROR
                    ){
                        loginUser(pwd, dwWindow)
                    }.show(dwWindow.p)

                    this.dwWindow.displayError("Your auth token seems to have expired. Click below to login again\n")
                    val hyperlink = HyperlinkInfo { p: Project ->
                        loginUser(pwd, this.dwWindow)
                    }
                    this.dwWindow.console.printHyperlink("Login here\n\n", hyperlink)
                }
                14 -> {
                    BalloonNotif(
                        "Build Successful",
                        "",
                        "Build completed, attaching debugger",
                        NotificationType.INFORMATION
                    ){
//                        BrowserUtil.browse("https://console.dashwave.io/home?profile=true")
                    }.show(dwWindow.p)
                    println("attaching debugger")
                    if(attachDebugger){
                        val debuggerCmd = DwCmds("get-debugger", pwd, true, this.dwWindow)
                        debuggerCmd.executeBg()
                    }
                    if(openEmulator){
                        val emulatorCmd = DwCmds("emulator", pwd, false, this.dwWindow)
                        this.dwWindow.lastEmulatorProcess = emulatorCmd
                        val ex = emulatorCmd.executeWithExitCode()
                    }
                }
                else -> {
                    // add try again
                    BalloonNotif(
                        "Build Failed",
                        "",
                        "Your build failed. Please check for logs in the console",
                        NotificationType.ERROR
                    ){}.show(dwWindow.p)
                }
            }
        }.start()
    }
}
