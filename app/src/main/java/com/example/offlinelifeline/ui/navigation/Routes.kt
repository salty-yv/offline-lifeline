package com.example.offlinelifeline.ui.navigation

enum class Route(
    val title: String,
    val iconLabel: String,
    val placeholderText: String
) {
    Chat(
        title = "对话",
        iconLabel = "聊",
        placeholderText = "文字聊天入口将在 Mock LLM 阶段接入。"
    ),
    Toolbox(
        title = "工具箱",
        iconLabel = "工",
        placeholderText = "SOS、电量建议等本地工具将在后续阶段实现。"
    ),
    Guide(
        title = "指南",
        iconLabel = "指",
        placeholderText = "离线指南列表将在数据层完成后接入。"
    ),
    EmergencyCard(
        title = "信息卡",
        iconLabel = "卡",
        placeholderText = "个人应急信息卡将在本地存储阶段实现。"
    ),
    Settings(
        title = "设置",
        iconLabel = "设",
        placeholderText = "语言、模型模式和隐私设置将在后续阶段补齐。"
    )
}
