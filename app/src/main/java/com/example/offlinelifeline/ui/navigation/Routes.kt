package com.example.offlinelifeline.ui.navigation

enum class Route(
    val title: String,
    val iconLabel: String,
    val placeholderText: String
) {
    Chat(
        title = "对话",
        iconLabel = "聊",
        placeholderText = "文字对话入口"
    ),
    Toolbox(
        title = "工具箱",
        iconLabel = "工",
        placeholderText = "SOS、电量建议等本地工具"
    ),
    Guide(
        title = "指南",
        iconLabel = "指",
        placeholderText = "离线指南列表"
    ),
    EmergencyCard(
        title = "信息卡",
        iconLabel = "卡",
        placeholderText = "个人应急信息卡"
    ),
    Settings(
        title = "设置",
        iconLabel = "设",
        placeholderText = "语言、模型模式和隐私设置将在后续阶段补齐。"
    )
}
