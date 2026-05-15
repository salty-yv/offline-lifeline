package com.example.offlinelifeline.ui.navigation

import com.example.offlinelifeline.ui.i18n.AppStrings

enum class Route {
    Chat,
    Toolbox,
    Guide,
    EmergencyCard,
    Settings;

    fun title(strings: AppStrings): String = when (this) {
        Chat -> strings.routeChat
        Toolbox -> strings.routeToolbox
        Guide -> strings.routeGuide
        EmergencyCard -> strings.routeEmergencyCard
        Settings -> strings.routeSettings
    }

    fun iconLabel(strings: AppStrings): String = when (this) {
        Chat -> strings.routeChatIcon
        Toolbox -> strings.routeToolboxIcon
        Guide -> strings.routeGuideIcon
        EmergencyCard -> strings.routeEmergencyCardIcon
        Settings -> strings.routeSettingsIcon
    }

    fun placeholderText(strings: AppStrings): String = when (this) {
        Chat -> strings.routeChatPlaceholder
        Toolbox -> strings.routeToolboxPlaceholder
        Guide -> strings.routeGuidePlaceholder
        EmergencyCard -> strings.routeEmergencyCardPlaceholder
        Settings -> strings.routeSettingsPlaceholder
    }
}
