package com.dashwave.plugin.actions

import com.dashwave.plugin.PluginStartup
import com.dashwave.plugin.windows.DashwaveWindow
import com.dashwave.plugin.utils.DwBuild
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class BuildAction: AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation

        // Enable or disable the action based on your condition

        presentation.isEnabled = PluginStartup.dwWindows?.get(e.project?.name)?.runEnabled?:false
        presentation.text = "Run build on dashwave"
        presentation.description = "Run build on dashwave"
    }

    override fun actionPerformed(e: AnActionEvent) {
        PluginStartup.dwWindows?.get(e.project?.name)?.runButton?.doClick()
    }
}