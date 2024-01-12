package com.dashwave.plugin

import com.intellij.execution.ui.ConsoleViewContentType
import java.io.BufferedReader
import java.io.File

class Process(cmd:String){
    private var pb: ProcessBuilder
    init {
        val command = arrayOf("/bin/bash", "-c", cmd)
        val processBuilder = ProcessBuilder(*command)
        processBuilder.redirectErrorStream(true)
        val homeValue = System.getenv("HOME")
        val pathValue = System.getenv("PATH")
        processBuilder.environment().put("PATH", "${pathValue}:${homeValue}/.dw-cli/tools/:${homeValue}/.dw-cli/bin/")
        this.pb = processBuilder
    }

    fun setCmdBasePath(basePath:String?){
        this.pb.directory(File(basePath))
    }

    fun start(){
        val process = this.pb.start()
//        handleOutput(process.inputStream.bufferedReader())
//        handleError(process.errorStream.bufferedReader())
//
//        // Wait for the process to terminate and handle the exit value
//        Thread{
//            val exitCode = process.waitFor()
//            handleExit(exitCode)
//        }.start()
    }

    private fun handleOutput(reader: BufferedReader) {
        Thread {
            reader.forEachLine { line ->
                DashwaveWindow.getConsole().print("$line", ConsoleViewContentType.NORMAL_OUTPUT)
            }
        }.start()
    }

    private fun handleError(reader: BufferedReader) {
        Thread {
            reader.forEachLine { line ->
                DashwaveWindow.getConsole().print("Error: $line", ConsoleViewContentType.ERROR_OUTPUT)
            }
        }.start()
    }

    private fun handleExit(exitCode: Int) {
        DashwaveWindow.getConsole().print("Process exited with code $exitCode", ConsoleViewContentType.NORMAL_OUTPUT)
        // Perform additional actions based on exit code
    }
}