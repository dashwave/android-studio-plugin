package com.dashwave.plugin.utils

import com.dashwave.plugin.PluginMode
import com.dashwave.plugin.doesFileExist
import com.dashwave.plugin.openCreateProjectDialog
import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.openapi.project.Project
import okhttp3.internal.wait
import kotlin.concurrent.thread

class DwBuildConfig(clean:Boolean,debug:Boolean,openEmulator:Boolean,module:String,variant:String, pwd:String?){
    var clean:Boolean = clean
    var debug:Boolean = debug
    var openEmulator:Boolean = openEmulator
    var pwd:String? = pwd
    var module:String = module
    var variant:String = variant
    var attachDebugger:Boolean = false
}

class DwBuild(config: DwBuildConfig, dwWindow: DashwaveWindow){
    private var cmd:String = "plugin-build"
    private val openEmulator:Boolean
    private var pwd:String?
    private var dwWindow:DashwaveWindow
    private var attachDebugger:Boolean = false
    init {
        if(PluginMode == "workspace"){
            cmd += " --workspace"
        }
        if(config.clean){
            cmd += " --clean"
        }
        if(config.debug){
            cmd += " --debug"
        }

        if (config.module != ""){
            cmd += " --module ${config.module}"
        }

        if (config.variant != ""){
            cmd += " --variant ${config.variant}"
        }
        if (config.attachDebugger){
            cmd += " --attach-debugger"
        }

        pwd = config.pwd
        openEmulator = config.openEmulator
        attachDebugger = config.attachDebugger
        this.dwWindow = dwWindow
    }

    private fun activateDashwaveWindow(){
//        this.dwWindow.show()
    }

    private fun execute(){
//        DashwaveWindow.displayInfo()
        val buildCmd = DwCmds(cmd, pwd, true, this.dwWindow)
        buildCmd.executeBuild(pwd, openEmulator,attachDebugger)
    }

    fun killEmulator(){
        // kill emulator for this build
    }

    fun run(p:Project){
        activateDashwaveWindow()
        this.dwWindow.clearConsole()
        this.dwWindow.disableRunButton()
        this.dwWindow.enableCancelButton()
        if(this.dwWindow.lastEmulatorProcess != null){
            this.dwWindow.lastEmulatorProcess!!.exit()
        }
        if (!doesFileExist("${pwd}/dashwave.yml")){
            openCreateProjectDialog(pwd, false, this.dwWindow){
                execute()
            }
            return
        }
        execute()
    }
}