package com.dashwave.plugin.utils

import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import okhttp3.internal.wait
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class Process(cmd:String, pwd:String?, log:Boolean, dwWindow: DashwaveWindow){
    private var ph:ProcessHandler
    private val latch = CountDownLatch(1)
    init {
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
                if(log) {
                    decodeAndPrintString(event.text, outputType, dwWindow)
                }
            }
            override fun processTerminated(event: ProcessEvent) {
                super.processTerminated(event)
                latch.countDown()
            }
        })
    }

    fun start(){
        ph.startNotify()
    }

    fun wait():Int{
        latch.await() // This will block until processTerminated is called
        return ph.exitCode ?: 0
    }

    fun exit(){
        ph.destroyProcess()
    }
}
private fun decodeAndPrintString(s:String, p: Key<*>, dwWindow: DashwaveWindow){
    val decoder = AnsiEscapeDecoder()
    val outputListener = AnsiEscapeDecoder.ColoredTextAcceptor { text, attributes ->
        dwWindow.displayOutput(text, ConsoleViewContentType.getConsoleViewType(attributes))
    }
    decoder.escapeText(s, p, outputListener)
}