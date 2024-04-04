package com.dashwave.plugin.utils

import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import okhttp3.internal.wait
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Process(cmd:String, pwd:String?, log:Boolean){
    private var ph:ProcessHandler
    private val latch = CountDownLatch(1)
    private val outputBuilder = StringBuilder()
    private var command:String
    init {
        command = cmd
        val cmd = GeneralCommandLine("/bin/bash","-c",cmd)
        if(pwd != null && pwd != ""){
            cmd.setWorkDirectory(pwd)
        }
        val homeValue = System.getenv("HOME")
        val pathValue = System.getenv("PATH")
        cmd.withEnvironment("PATH", "${pathValue}:${homeValue}/.dw-cli/tools/:${homeValue}/.dw-cli/bin/")
        ph = OSProcessHandler(cmd)
        ph.addProcessListener(object:ProcessAdapter(){
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val text = event.text.trim()
                if (text.contains(cmd.commandLineString)){
                    return
                }
                outputBuilder.append(text)
                if(log) {
                    decodeAndPrintString("$text\n", outputType)
                }
            }
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                latch.countDown()
            }
        })
    }

    fun start(log: Boolean){
        if(log){
            DashwaveWindow.displayInfo("${this.command}\n\n")
        }
        ph.startNotify()
    }

    fun wait():Int{
        latch.await() // This will block until processTerminated is called
        return ph.exitCode ?: 0
    }

    fun exit(){
        ph.destroyProcess()
    }

    fun getOutput(): String {
        return outputBuilder.toString()
    }
}
private fun decodeAndPrintString(s:String, p: Key<*>){
    val decoder = AnsiEscapeDecoder()
    val outputListener = AnsiEscapeDecoder.ColoredTextAcceptor { text, attributes ->
        DashwaveWindow.displayOutput(text, ConsoleViewContentType.getConsoleViewType(attributes))
    }
    decoder.escapeText(s, p, outputListener)
}