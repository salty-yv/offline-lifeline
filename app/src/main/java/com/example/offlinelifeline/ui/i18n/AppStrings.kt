package com.example.offlinelifeline.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.offlinelifeline.core.model.ToolType
import com.example.offlinelifeline.device.battery.BatteryStatus

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { ZhAppStrings }

fun appStringsFor(languageTag: String): AppStrings {
    return if (languageTag.startsWith("en")) EnAppStrings else ZhAppStrings
}

interface AppStrings {
    val languageTag: String
    val languageName: String
    val routeChat: String
    val routeChatIcon: String
    val routeChatPlaceholder: String
    val routeToolbox: String
    val routeToolboxIcon: String
    val routeToolboxPlaceholder: String
    val routeGuide: String
    val routeGuideIcon: String
    val routeGuidePlaceholder: String
    val routeEmergencyCard: String
    val routeEmergencyCardIcon: String
    val routeEmergencyCardPlaceholder: String
    val routeSettings: String
    val routeSettingsIcon: String
    val routeSettingsPlaceholder: String

    val settingsTitle: String
    val languageSectionTitle: String
    val languageSectionBody: String
    val simplifiedChinese: String
    val english: String
    val currentLanguage: String

    val clearCurrentDialogTitle: String
    val clearCurrentDialogBody: String
    val confirmClear: String
    val cancel: String
    val deleteConversationTitle: String
    fun deleteConversationBody(title: String): String
    val confirmDelete: String
    val history: String
    val newConversation: String
    val defaultConversationTitle: String
    val clearCurrent: String
    val delete: String
    val camera: String
    val gallery: String
    val processingImage: String
    val chatInputPlaceholder: String
    val freeChatInputPlaceholder: String
    val stop: String
    val send: String
    val image: String
    val pendingImageContentDescription: String
    val attachedImageContentDescription: String
    val recommendedTools: String
    val freeChatModeOn: String
    val freeChatModeOff: String

    val generatingReply: String
    val checkingRealModel: String
    val realModelMissing: String
    val checksumFailed: String
    val realModelReady: String
    val realModelReadyToLoad: String
    val mockModelLoading: String
    val mockModelReady: String
    val mockModelReleased: String
    val mockModelPending: String

    val searchGuides: String
    val noGuideResults: String
    val backToList: String

    val emergencyCardEditorTitle: String
    val name: String
    val bloodType: String
    val allergies: String
    val chronicConditions: String
    val medications: String
    val emergencyContact: String
    val notes: String
    val hideSensitiveFields: String
    val save: String
    val showToRescuers: String
    val backToEdit: String
    val emergencyInformation: String
    val notFilled: String

    val sosFlashlightTitle: String
    val sosFlashlightBody: String
    val screenSosTitle: String
    val screenSosBody: String
    val batteryAdviceTitle: String
    val batteryAdviceBody: String
    val emergencyCardToolBody: String
    val debugLogTitle: String
    val debugLogBody: String
    val flashlightStatus: String
    val flashlightOn: String
    val flashlightOff: String
    val sosFlashStatus: String
    val running: String
    val notRunning: String
    val turnOn: String
    val turnOff: String
    val startSos: String
    val stopSos: String
    val currentBattery: String
    val unknown: String
    val charging: String
    val notCharging: String
    val refresh: String
    val debugLogNote: String
    val recordSnapshot: String
    val exportTxt: String
    fun exported(path: String): String
    val backToToolbox: String
    val exit: String

    fun toolName(toolType: ToolType): String
    fun batteryAdvice(status: BatteryStatus): List<String>
}

object ZhAppStrings : AppStrings {
    override val languageTag = "zh-CN"
    override val languageName = "中文"
    override val routeChat = "对话"
    override val routeChatIcon = "聊"
    override val routeChatPlaceholder = "文字对话入口"
    override val routeToolbox = "工具箱"
    override val routeToolboxIcon = "工"
    override val routeToolboxPlaceholder = "SOS、电量建议等本地工具"
    override val routeGuide = "指南"
    override val routeGuideIcon = "指"
    override val routeGuidePlaceholder = "离线指南列表"
    override val routeEmergencyCard = "信息卡"
    override val routeEmergencyCardIcon = "卡"
    override val routeEmergencyCardPlaceholder = "个人应急信息卡"
    override val routeSettings = "设置"
    override val routeSettingsIcon = "设"
    override val routeSettingsPlaceholder = "语言、模型模式和隐私设置"

    override val settingsTitle = "设置"
    override val languageSectionTitle = "语言"
    override val languageSectionBody = "切换后界面会立即更新，选择会保存在本机。"
    override val simplifiedChinese = "中文"
    override val english = "English"
    override val currentLanguage = "当前语言"

    override val clearCurrentDialogTitle = "清空当前对话？"
    override val clearCurrentDialogBody = "这会删除当前对话里的本地消息，并重置模型上下文。这个操作不能撤销。"
    override val confirmClear = "确认清空"
    override val cancel = "取消"
    override val deleteConversationTitle = "删除这条对话？"
    override fun deleteConversationBody(title: String) = "这会删除「$title」里的本地消息记录。删除后不能撤销。"
    override val confirmDelete = "确认删除"
    override val history = "历史记录"
    override val newConversation = "新建对话"
    override val defaultConversationTitle = "新对话"
    override val clearCurrent = "清空当前"
    override val delete = "删除"
    override val camera = "拍照"
    override val gallery = "相册"
    override val processingImage = "正在处理图片"
    override val chatInputPlaceholder = "描述你的情况，也可以附加图片"
    override val freeChatInputPlaceholder = "随便聊，内置知识库仍可检索"
    override val stop = "停止"
    override val send = "发送"
    override val image = "图片"
    override val pendingImageContentDescription = "待发送图片"
    override val attachedImageContentDescription = "已附加图片"
    override val recommendedTools = "推荐工具"
    override val freeChatModeOn = "自由对话"
    override val freeChatModeOff = "自由对话"

    override val generatingReply = "正在生成回复..."
    override val checkingRealModel = "正在检查真实模型文件..."
    override val realModelMissing = "真实模型不可用，可使用 Mock 对话、离线指南和工具箱。"
    override val checksumFailed = "模型校验失败，已阻止真实模型加载；仍可使用 Mock 和离线工具。"
    override val realModelReady = "真实模型已就绪，LiteRT-LM 引擎已加载。"
    override val realModelReadyToLoad = "真实模型文件已就绪，正在准备 LiteRT-LM 引擎。"
    override val mockModelLoading = "正在初始化 Mock 模型..."
    override val mockModelReady = "Mock 模型已就绪"
    override val mockModelReleased = "Mock 模型已释放"
    override val mockModelPending = "Mock 模型待初始化"

    override val searchGuides = "搜索指南"
    override val noGuideResults = "没有找到匹配的离线指南"
    override val backToList = "返回列表"

    override val emergencyCardEditorTitle = "本地个人应急信息卡"
    override val name = "姓名"
    override val bloodType = "血型"
    override val allergies = "过敏史"
    override val chronicConditions = "慢性病"
    override val medications = "常用药"
    override val emergencyContact = "紧急联系人"
    override val notes = "备注"
    override val hideSensitiveFields = "默认隐藏敏感字段"
    override val save = "保存"
    override val showToRescuers = "展示给救援人员"
    override val backToEdit = "返回编辑"
    override val emergencyInformation = "应急信息"
    override val notFilled = "未填写"

    override val sosFlashlightTitle = "SOS 闪光灯"
    override val sosFlashlightBody = "打开手电筒或发出 SOS 闪烁信号。"
    override val screenSosTitle = "屏幕 SOS"
    override val screenSosBody = "全屏高亮显示 SOS，并保持屏幕常亮。"
    override val batteryAdviceTitle = "电量保护建议"
    override val batteryAdviceBody = "读取本机电量并给出离线省电建议。"
    override val emergencyCardToolBody = "本地保存并展示给救援人员。"
    override val debugLogTitle = "Debug Log 导出"
    override val debugLogBody = "导出本地日志文件，不自动上传。"
    override val flashlightStatus = "闪光灯状态"
    override val flashlightOn = "已打开"
    override val flashlightOff = "已关闭"
    override val sosFlashStatus = "SOS 闪烁"
    override val running = "运行中"
    override val notRunning = "未运行"
    override val turnOn = "打开"
    override val turnOff = "关闭"
    override val startSos = "启动 SOS"
    override val stopSos = "停止 SOS"
    override val currentBattery = "当前电量"
    override val unknown = "未知"
    override val charging = "状态：正在充电"
    override val notCharging = "状态：未充电"
    override val refresh = "刷新"
    override val debugLogNote = "日志只保存在本机。导出文件会写入应用专属外部目录。"
    override val recordSnapshot = "记录稳定性快照"
    override val exportTxt = "导出 .txt"
    override fun exported(path: String) = "已导出：$path"
    override val backToToolbox = "返回工具箱"
    override val exit = "退出"

    override fun toolName(toolType: ToolType): String = when (toolType) {
        ToolType.SOS_FLASHLIGHT -> sosFlashlightTitle
        ToolType.SCREEN_SOS -> screenSosTitle
        ToolType.BATTERY_SAVER_ADVICE -> batteryAdviceTitle
        ToolType.EMERGENCY_CARD -> emergencyCardEditorTitle
        ToolType.OFFLINE_GUIDE -> routeGuide
        ToolType.DEBUG_LOG_EXPORT -> debugLogTitle
    }

    override fun batteryAdvice(status: BatteryStatus): List<String> {
        val advice = mutableListOf<String>()
        val percent = status.percent
        if (percent == null) {
            advice += "无法读取当前电量时，按低电量处理。"
        } else if (percent <= 15) {
            advice += "电量很低，优先保留求救、查看指南和短消息能力。"
        } else if (percent <= 30) {
            advice += "电量偏低，减少连续对话和高亮屏幕使用。"
        } else {
            advice += "电量暂时可用，仍建议提前准备固定求救信息。"
        }
        if (!status.isCharging) {
            advice += "降低屏幕亮度，关闭震动和不必要的后台应用。"
            advice += "关闭 Wi-Fi、蓝牙、定位，或在不需要通信时开启飞行模式。"
            advice += "优先查看离线指南，减少连续本地模型生成。"
            advice += "准备一条短求救信息：位置线索、人数、伤情、电量。"
            advice += "SOS 闪光灯和屏幕高亮只在必要时短时使用。"
        } else {
            advice += "正在充电时，先保存关键指南和求救信息，避免拔电后临时整理。"
        }
        return advice
    }
}

object EnAppStrings : AppStrings {
    override val languageTag = "en-US"
    override val languageName = "English"
    override val routeChat = "Chat"
    override val routeChatIcon = "C"
    override val routeChatPlaceholder = "Text chat entry"
    override val routeToolbox = "Tools"
    override val routeToolboxIcon = "T"
    override val routeToolboxPlaceholder = "Local SOS, battery, and emergency tools"
    override val routeGuide = "Guides"
    override val routeGuideIcon = "G"
    override val routeGuidePlaceholder = "Offline guide list"
    override val routeEmergencyCard = "Card"
    override val routeEmergencyCardIcon = "ID"
    override val routeEmergencyCardPlaceholder = "Personal emergency card"
    override val routeSettings = "Settings"
    override val routeSettingsIcon = "S"
    override val routeSettingsPlaceholder = "Language, model mode, and privacy settings"

    override val settingsTitle = "Settings"
    override val languageSectionTitle = "Language"
    override val languageSectionBody = "The interface updates immediately and your choice is saved on this device."
    override val simplifiedChinese = "中文"
    override val english = "English"
    override val currentLanguage = "Current language"

    override val clearCurrentDialogTitle = "Clear current chat?"
    override val clearCurrentDialogBody = "This deletes local messages in the current chat and resets model context. It cannot be undone."
    override val confirmClear = "Clear"
    override val cancel = "Cancel"
    override val deleteConversationTitle = "Delete this chat?"
    override fun deleteConversationBody(title: String) = "This deletes local messages in \"$title\". It cannot be undone."
    override val confirmDelete = "Delete"
    override val history = "History"
    override val newConversation = "New chat"
    override val defaultConversationTitle = "New chat"
    override val clearCurrent = "Clear"
    override val delete = "Delete"
    override val camera = "Camera"
    override val gallery = "Gallery"
    override val processingImage = "Processing image"
    override val chatInputPlaceholder = "Describe your situation, or attach an image"
    override val freeChatInputPlaceholder = "Chat freely, local knowledge base still active"
    override val stop = "Stop"
    override val send = "Send"
    override val image = "Image"
    override val pendingImageContentDescription = "Pending image"
    override val attachedImageContentDescription = "Attached image"
    override val recommendedTools = "Recommended tools"
    override val freeChatModeOn = "Free chat"
    override val freeChatModeOff = "Free chat"

    override val generatingReply = "Generating reply..."
    override val checkingRealModel = "Checking real model files..."
    override val realModelMissing = "Real model unavailable. Mock chat, offline guides, and tools are still available."
    override val checksumFailed = "Model verification failed. Real model loading was blocked; Mock and offline tools remain available."
    override val realModelReady = "Real model is ready. LiteRT-LM engine is loaded."
    override val realModelReadyToLoad = "Real model files are ready. Preparing LiteRT-LM engine."
    override val mockModelLoading = "Initializing Mock model..."
    override val mockModelReady = "Mock model is ready"
    override val mockModelReleased = "Mock model released"
    override val mockModelPending = "Mock model pending initialization"

    override val searchGuides = "Search guides"
    override val noGuideResults = "No matching offline guides found"
    override val backToList = "Back to list"

    override val emergencyCardEditorTitle = "Local personal emergency card"
    override val name = "Name"
    override val bloodType = "Blood type"
    override val allergies = "Allergies"
    override val chronicConditions = "Chronic conditions"
    override val medications = "Medications"
    override val emergencyContact = "Emergency contact"
    override val notes = "Notes"
    override val hideSensitiveFields = "Hide sensitive fields by default"
    override val save = "Save"
    override val showToRescuers = "Show to rescuers"
    override val backToEdit = "Back to edit"
    override val emergencyInformation = "Emergency information"
    override val notFilled = "Not filled"

    override val sosFlashlightTitle = "SOS flashlight"
    override val sosFlashlightBody = "Use the flashlight or send an SOS flash signal."
    override val screenSosTitle = "Screen SOS"
    override val screenSosBody = "Show a full-screen bright SOS and keep the screen awake."
    override val batteryAdviceTitle = "Battery advice"
    override val batteryAdviceBody = "Read battery status and show offline power-saving advice."
    override val emergencyCardToolBody = "Save locally and show it to rescuers."
    override val debugLogTitle = "Export Debug Log"
    override val debugLogBody = "Export local logs. Nothing is uploaded automatically."
    override val flashlightStatus = "Flashlight status"
    override val flashlightOn = "On"
    override val flashlightOff = "Off"
    override val sosFlashStatus = "SOS flash"
    override val running = "Running"
    override val notRunning = "Not running"
    override val turnOn = "Turn on"
    override val turnOff = "Turn off"
    override val startSos = "Start SOS"
    override val stopSos = "Stop SOS"
    override val currentBattery = "Current battery"
    override val unknown = "Unknown"
    override val charging = "Status: charging"
    override val notCharging = "Status: not charging"
    override val refresh = "Refresh"
    override val debugLogNote = "Logs stay on this device. Exported files are written to the app-specific external directory."
    override val recordSnapshot = "Record stability snapshot"
    override val exportTxt = "Export .txt"
    override fun exported(path: String) = "Exported: $path"
    override val backToToolbox = "Back to tools"
    override val exit = "Exit"

    override fun toolName(toolType: ToolType): String = when (toolType) {
        ToolType.SOS_FLASHLIGHT -> sosFlashlightTitle
        ToolType.SCREEN_SOS -> screenSosTitle
        ToolType.BATTERY_SAVER_ADVICE -> batteryAdviceTitle
        ToolType.EMERGENCY_CARD -> emergencyCardEditorTitle
        ToolType.OFFLINE_GUIDE -> routeGuide
        ToolType.DEBUG_LOG_EXPORT -> debugLogTitle
    }

    override fun batteryAdvice(status: BatteryStatus): List<String> {
        val advice = mutableListOf<String>()
        val percent = status.percent
        if (percent == null) {
            advice += "If the battery level cannot be read, treat the phone as low on power."
        } else if (percent <= 15) {
            advice += "Battery is very low. Preserve emergency messaging, guides, and short help requests first."
        } else if (percent <= 30) {
            advice += "Battery is low. Reduce long chats and bright-screen use."
        } else {
            advice += "Battery is usable for now, but prepare a fixed help message early."
        }
        if (!status.isCharging) {
            advice += "Lower screen brightness and disable vibration and unnecessary background apps."
            advice += "Turn off Wi-Fi, Bluetooth, location, or use airplane mode when communication is not needed."
            advice += "Use offline guides first and reduce repeated local model generation."
            advice += "Prepare a short help message: location clues, people count, injuries, and battery level."
            advice += "Use SOS flashlight and bright screen only briefly when needed."
        } else {
            advice += "While charging, save key guides and help messages before you need to unplug."
        }
        return advice
    }
}
