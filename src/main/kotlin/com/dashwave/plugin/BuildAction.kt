package com.dashwave.plugin

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager

class BuildAction: AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val decoder = AnsiEscapeDecoder()

        val cmd = GeneralCommandLine("/bin/bash","-c","dw build")

        cmd.setWorkDirectory(project.basePath)
        val homeValue = System.getenv("HOME")
        val pathValue = System.getenv("PATH")
        cmd.withEnvironment("PATH", "${pathValue}:${homeValue}/.dw-cli/tools/:${homeValue}/.dw-cli/bin/")

        val toolWindowManager = ToolWindowManager.getInstance(project)
        val myWindow = toolWindowManager.getToolWindow("Dashwave")
        myWindow?.show()


        val console = DashwaveWindow.getConsole()
        console.clear()
        val processHandler: ProcessHandler
        var emulatorURL: String? = null
        try {
            processHandler = OSProcessHandler(cmd)
//            console.attachToProcess(processHandler)
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    // Handle process termination if needed
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
                            val process = processBuilder.pb.start()
                            process.inputStream.bufferedReader().use { reader ->
                                while (true) {
                                    val line = reader.readLine() ?: break
                                    DashwaveWindow.displayOutput(line, ConsoleViewContentType.NORMAL_OUTPUT)
                                }
                            }

                            process.waitFor()  // Wait for the process to finish, you can remove this if not needed.
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    decoder.escapeText(event.text, outputType, { text, attributes ->
                        DashwaveWindow.displayOutput(text, ConsoleViewContentType.getConsoleViewType(attributes))
                    })
                }
            })
            processHandler.startNotify()
        } catch (e: ExecutionException) {
            DashwaveWindow.displayOutput(
                "Error executing command: ${e.localizedMessage}\n",
                ConsoleViewContentType.ERROR_OUTPUT
            )
        }



//        val executor = DefaultRunExecutor.getRunExecutorInstance()
//        RunContentManager.getInstance(project).showRunContent(executor, descriptor)
    }

    // Override getActionUpdateThread() when you target 2022.3 or later!
}