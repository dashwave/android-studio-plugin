package com.dashwave.plugin.utils

import com.dashwave.plugin.installDW
import com.dashwave.plugin.notif.BalloonNotif
import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.keymap.impl.ui.Hyperlink
import com.intellij.openapi.project.Project
import com.intellij.util.Futures.thenRunAsync
import okhttp3.internal.wait
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.system.exitProcess

class DwCmds(execCmd:String, wd:String?, log: Boolean){
    private var cmd:String
    private var p:Process
    private var pwd:String?
    init {
        cmd = "dw $execCmd --plugin"
        pwd = wd
        p = com.dashwave.plugin.utils.Process(cmd, pwd, log)
    }

    fun executeWithExitCode():Int{
        this.p.start()
        val exitCode = this.p.wait()
        if(exitCode == 11){
            DashwaveWindow.displayError("Dashwave has a major update, you need to update dependencies\n")
            val hyperlink = HyperlinkInfo { p: Project ->
                installDW(this.pwd)
            }
            DashwaveWindow.console.printHyperlink("Click here to update\n\n", hyperlink)
        }
        return exitCode
    }

    fun exit(){
        this.p.exit()
    }


    fun executeBuild(pwd:String?, openEmulator:Boolean){
        this.p.start()
        DashwaveWindow.currentBuild = this
        BalloonNotif(
            "Build started",
            "",
            "Build started on dashwave. Your build is running on a remote machine. You can view the logs in console and view emulation after build completes",
            NotificationType.INFORMATION
        ){}.show()
        DashwaveWindow.changeIcon(DashwaveWindow.loadIcon)
        Thread{
            var ex = this.p.wait()
            DashwaveWindow.currentBuild = null
            DashwaveWindow.changeIcon(DashwaveWindow.dwIcon)
            DashwaveWindow.enableRunButton()
            DashwaveWindow.disableCancelButton()
//            DashwaveWindow.show()
            when(ex){
                0 -> {
                    BalloonNotif(
                        "Build Successful",
                        "",
                        "Build completed successfully!",
                        NotificationType.INFORMATION
                    ){
//                        BrowserUtil.browse("https://console.dashwave.io/home?profile=true")
                    }.show()

                    if(openEmulator){
                        val emulatorCmd = DwCmds("emulator", pwd, false)
                        DashwaveWindow.lastEmulatorProcess = emulatorCmd
                        val ex = emulatorCmd.executeWithExitCode()
                    }
                }
                11 -> {
                    DashwaveWindow.displayError("Dashwave has a major update, you need to update dependencies\n")
                    val hyperlink = HyperlinkInfo { p: Project ->
                        installDW(pwd)
                    }
                    DashwaveWindow.console.printHyperlink("Click here to update\n\n", hyperlink)
                }
                // exitcode = 12 means authorize scm failed
                12 -> {
                    BalloonNotif(
                        "Configuration Failed",
                        "Authorize Git",
                        "We could not fetch your project from github/gitlab. Please authorize to provide access",
                        NotificationType.ERROR
                    ){
                        BrowserUtil.browse("https://console.dashwave.io/home?profile=true")
                    }.show()
                }
                else -> {
                    // add try again
                    BalloonNotif(
                        "Build Failed",
                        "",
                        "Your build failed. Please check for logs in the console",
                        NotificationType.ERROR
                    ){}.show()
                }
            }
        }.start()
    }
}
