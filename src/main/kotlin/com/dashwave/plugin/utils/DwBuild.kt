package com.dashwave.plugin.utils

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
}

class DwBuild(config: DwBuildConfig){
    private var cmd:String = "plugin-build"
    private val openEmulator:Boolean
    private var pwd:String?
    init {
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

        pwd = config.pwd
        openEmulator = config.openEmulator
    }

    private fun activateDashwaveWindow(){
        DashwaveWindow.show()
    }

    private fun execute(){
//        DashwaveWindow.displayInfo()
        val buildCmd = DwCmds(cmd, pwd, true)
//        buildCmd.executeBuild(pwd, openEmulator)
    }

    fun killEmulator(){
        // kill emulator for this build
    }

    fun run(p:Project){
        activateDashwaveWindow()
        DashwaveWindow.clearConsole()
        DashwaveWindow.disableRunButton()
        DashwaveWindow.enableCancelButton()
        if(DashwaveWindow.lastEmulatorProcess != null){
            DashwaveWindow.lastEmulatorProcess!!.exit()
        }
        if (!doesFileExist("${pwd}/dashwave.yml")){
            if(!openCreateProjectDialog(pwd, false)){
                DashwaveWindow.enableRunButton()
                DashwaveWindow.disableCancelButton()
                return
            }
        }
        execute()
    }
}