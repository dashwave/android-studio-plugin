package com.dashwave.plugin.notif

import com.dashwave.plugin.windows.DashwaveWindow
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

class BalloonNotif(title:String, actionTitle:String ,description:String,type: NotificationType, action:()->Unit){
    private var title:String
    private var description:String
    private var action:()->Unit
    private var type:NotificationType
    private var actionTitle:String
    init {
        this.title = title
        this.description = description
        this.action = action
        this.type = type
        this.actionTitle = actionTitle
    }
    fun show(project:Project) {
        val notification = Notification(
            "YourPluginNotificationGroup",
            this.title,
            this.description,
            this.type,
        )
        if(actionTitle !=  ""){
            notification.addAction(object : NotificationAction(actionTitle){
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    action()
                }
            })
        }
        Notifications.Bus.notify(notification, project)
    }
}