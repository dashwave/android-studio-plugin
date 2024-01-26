package com.dashwave.plugin.actions

import com.dashwave.plugin.windows.DashwaveWindow
import com.dashwave.plugin.utils.DwBuild
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class BuildAction: AnAction() {


    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation

        // Enable or disable the action based on your condition
        presentation.isEnabled = DashwaveWindow.runEnabled
        presentation.text = "Run build on dashwave"
        presentation.description = "Run build on dashwave"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if(project != null){
            val buildConfigs = DashwaveWindow.getBuildConfigs(project)
            val build = DwBuild(buildConfigs)
            build.run(project)
        }
    }
}