package com.dashwave.plugin

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import com.jetbrains.rd.util.string.println
import java.io.BufferedReader
import java.io.File

class Process(cmd:String){
    private var pb: ProcessBuilder
    private var cmd: String
    private val key = Key.create<Any>("ConsoleTypeCustom")
    init {
        val command = arrayOf("/bin/bash", "-c", cmd)
        this.cmd = cmd
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

    fun start(log:Boolean): java.lang.Process{
//        DashwaveWindow.getConsole().print("${this.cmd}\n", ConsoleViewContentType.NORMAL_OUTPUT)
        val process = this.pb.start()
        if (log) {
            handleOutput(process.inputStream.bufferedReader())
            handleError(process.errorStream.bufferedReader())
        }
        return process
//
//        // Wait for the process to terminate and handle the exit value
//        Thread{
//            val exitCode = process.waitFor()
//            handleExit(exitCode)
//        }.start()
    }

    private fun handleOutput(reader: BufferedReader) {
        val decoder = AnsiEscapeDecoder()
        val outputListener = AnsiEscapeDecoder.ColoredTextAcceptor { text, attributes ->
            DashwaveWindow.displayOutput(text, ConsoleViewContentType.getConsoleViewType(attributes))
        }
        Thread {
            reader.forEachLine { line ->
                    decoder.escapeText("$line\n",ProcessOutputTypes.STDOUT,outputListener)
            }
        }.start()
    }

    private fun handleError(reader: BufferedReader) {
        val decoder = AnsiEscapeDecoder()
        val outputListener = AnsiEscapeDecoder.ColoredTextAcceptor { text, attributes ->
            DashwaveWindow.displayOutput(text, ConsoleViewContentType.getConsoleViewType(attributes))
        }
        Thread {
            reader.forEachLine { line ->
                decoder.escapeText("$line\n", ProcessOutputTypes.STDERR, outputListener)
            }
        }.start()
    }

    private fun handleExit(exitCode: Int) {
        DashwaveWindow.getConsole().print("Process exited with code $exitCode", ConsoleViewContentType.NORMAL_OUTPUT)
        // Perform additional actions based on exit code
    }
}