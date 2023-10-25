package com.dashwave.plugin

class Process(cmd:String){
    var pb: ProcessBuilder
    init {
        val command = arrayOf("/bin/bash", "-c", cmd)
        val processBuilder = ProcessBuilder(*command)
        processBuilder.redirectErrorStream(true)
        val homeValue = System.getenv("HOME")
        val pathValue = System.getenv("PATH")
        processBuilder.environment().put("PATH", "${pathValue}:${homeValue}/.dw-cli/tools/:${homeValue}/.dw-cli/bin/")
        this.pb = processBuilder
    }
}