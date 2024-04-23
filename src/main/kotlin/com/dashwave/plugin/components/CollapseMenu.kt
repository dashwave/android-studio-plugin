package com.dashwave.plugin.components

import com.intellij.icons.AllIcons
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPopupMenu

class CollapseMenu(label:String){
    private val menu:JPopupMenu = JPopupMenu()
    private val btn:JButton = JButton()
    private var displayOn:Boolean = false
    lateinit var components:List<JComponent>
    init {
        val label:String = label
        btn.apply {
            text = label
            icon = AllIcons.General.ArrowDown
            addActionListener{
                if(displayOn){
                    menu.setSize(0,0)
                }else{
                    menu.show(this, 0, this.height)
                    menu.setSize(preferredSize.width, preferredSize.height)
                }
                displayOn = !displayOn
            }
        }

    }

    fun add(comp:JComponent){
        menu.add(comp)
    }

    fun getComponent():JComponent{
        return btn
    }


}