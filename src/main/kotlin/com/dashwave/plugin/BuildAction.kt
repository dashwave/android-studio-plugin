package com.dashwave.plugin

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import java.util.Optional

class BuildAction: AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val presentation = e.presentation

        // Enable or disable the action based on your condition
        presentation.isEnabled = DashwaveWindow.runEnabled
        presentation.text = "dw build"
        presentation.description = "run a build on dashwave cloud"
    }


    private var buildProcHandler: ProcessHandler? = null

    fun initBuildProcHandler(basePath: String) {
        val decoder = AnsiEscapeDecoder()
        var buildCmd = "dw plugin-build --pwd=$basePath"
        if (DashwaveWindow.cleanBuild) {
            buildCmd += " --clean"
        }
        if (DashwaveWindow.debugEnabled) {
            buildCmd += " --debug"
        }
        val cmd = GeneralCommandLine("/bin/bash","-c",buildCmd)

        cmd.setWorkDirectory(basePath)
        val homeValue = System.getenv("HOME")
        val pathValue = System.getenv("PATH")
        cmd.withEnvironment("PATH", "${pathValue}:${homeValue}/.dw-cli/tools/:${homeValue}/.dw-cli/bin/")

        val processHandler: ProcessHandler
        var emulatorURL: String? = null
        try {
            processHandler = OSProcessHandler(cmd)
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode != 0) {
                        var hyperlink = HyperlinkInfo{p:Project ->
                            DashwaveWindow.runButton.doClick()
                        }
                        DashwaveWindow.getConsole().printHyperlink("\nPlease try again!", hyperlink)
                    }
                    DashwaveWindow.enableRunButton()
                    DashwaveWindow.disableCancelButton()
                }
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val patternEmulatorConnection = """\s*EMULATOR CONNECTED SUCCESSFULLY (.+)""".toRegex()
                    patternEmulatorConnection.find(event.text)?.let { matchResult ->
                        val url = matchResult.groups[1]?.value
                        emulatorURL = url
                    }
                    if (event.text.contains("APK INSTALLED SUCCESSFULLY")) {
                        val emulatorCmd = "dw scrcpy ${emulatorURL}"
                        try {
                            val processBuilder = Process(emulatorCmd)
                            processBuilder.start(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    decoder.escapeText(event.text, outputType) { text, attributes ->
                        DashwaveWindow.displayOutput(text, ConsoleViewContentType.getConsoleViewType(attributes))
                    }
                }
            })
            buildProcHandler = processHandler
        } catch (e: ExecutionException) {
            DashwaveWindow.displayOutput(
                "Error executing command: ${e.localizedMessage}\n",
                ConsoleViewContentType.ERROR_OUTPUT
            )
        }
    }

    fun startBuildProcHandler() {
        buildProcHandler?.startNotify()
    }

    fun terminateBuildProcHandler(basePath: String?) {
        buildProcHandler?.destroyProcess()
        val emulatorCmd = "dw stop-build"
        try {
            val processBuilder = Process(emulatorCmd)
            processBuilder.setCmdBasePath(basePath)
            processBuilder.start(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        DashwaveWindow.getConsole().print("Build Cancelled\n", ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val myWindow = toolWindowManager.getToolWindow("Dashwave")
        myWindow?.show()

        val console = DashwaveWindow.getConsole()
        DashwaveWindow.enableCancelButton()
        DashwaveWindow.disableRunButton()
        console.clear()
        val pwd = project.basePath
        if (pwd != null) {
            initBuildProcHandler(pwd)
            startBuildProcHandler()
        }
    }

    // Override getActionUpdateThread() when you target 2022.3 or later!
}