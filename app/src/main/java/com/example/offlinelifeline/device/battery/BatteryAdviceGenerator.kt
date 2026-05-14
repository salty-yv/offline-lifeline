package com.example.offlinelifeline.device.battery

class BatteryAdviceGenerator {
    fun buildAdvice(status: BatteryStatus): List<String> {
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
