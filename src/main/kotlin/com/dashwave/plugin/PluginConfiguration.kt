package com.dashwave.plugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "PluginSettings",
    storages = [Storage("PluginSettings.xml")]
)
class PluginConfiguration: PersistentStateComponent<PluginConfiguration.State> {
    data class State(
        var pluginMode:String = "local",
        var pluginEnv:String = ""
    )

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(): PluginConfiguration {
            return ServiceManager.getService(PluginConfiguration::class.java)
        }
    }
}