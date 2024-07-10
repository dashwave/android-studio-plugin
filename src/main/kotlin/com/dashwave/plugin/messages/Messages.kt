package com.dashwave.plugin.messages

class Messages {
    companion object {
        val DW_INSTALLED_ALREADY = "✅ Dashwave plugin dependencies are installed\n\n"
        val DW_NOT_INSTALLED = "⚠\uFE0F Dashwave plugin dependencies are not installed\n\n"
        val DW_DEPS_INSTALL_SUCCESS = "✅ Dependencies installed successfully\n\n"
        val DW_DEPS_INSTALL_FAILED = "❌ Dependencies installation failed\n\n"
        val DW_DEPS_CONFIGURING = "🧑🏻‍💻Configuring dependencies \n\n"
        val DW_DEPS_CONFIGURE_SUCCESS = "✅ Dependencies configured successfully\n\n"
        val DW_DEPS_CONFIGURE_FAILED = "❌ Dependencies configuration failed\n\n"
        val DW_LOGIN_FAILED = "⚠️ Login failed. Click here to retry\n\n"
        val PROJECT_CONNECTION_SUCCESS = "✅ Project is successfully connected to dashwave\n\n"
        val PROJECT_CONNECTION_FAILED = "❌ Dashwave project creation failed\n"
        val GIT_NOT_CONFIGURED = "Your local codebase is not currently hosted on a Git repository (GitHub/GitLab). Please ensure your codebase is hosted on Git to use this plugin.\n"
        val GRADLE_SYNC_CANCELLATION_FAILED = "Gradle sync needs to cancelled before proceeding. Please stop it manually.\n"
    }
}