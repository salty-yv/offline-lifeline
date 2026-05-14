package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.RiskDomain
import com.example.offlinelifeline.core.model.ToolRecommendation
import com.example.offlinelifeline.core.model.ToolType

class ToolRouter {
    fun recommendTools(
        riskDomains: Set<RiskDomain>,
        context: AgentContext
    ): List<ToolRecommendation> {
        val recommendations = mutableListOf<ToolRecommendation>()

        if (RiskDomain.LOW_BATTERY in riskDomains || (context.batteryPercent != null && context.batteryPercent <= 20)) {
            recommendations += ToolRecommendation(
                toolType = ToolType.BATTERY_SAVER_ADVICE,
                reason = "电量偏低，先保护关键通信和离线使用能力。",
                priority = 100
            )
        }

        if (RiskDomain.LOST in riskDomains && RiskDomain.NIGHT in riskDomains) {
            recommendations += ToolRecommendation(
                toolType = ToolType.SCREEN_SOS,
                reason = "夜间迷路时可用高亮屏幕作为求救信号。",
                priority = 90
            )
            recommendations += ToolRecommendation(
                toolType = ToolType.SOS_FLASHLIGHT,
                reason = "如果环境允许，可用闪光灯发出 SOS 信号。",
                priority = 80
            )
        }

        if (RiskDomain.INJURY in riskDomains || RiskDomain.BLEEDING in riskDomains || RiskDomain.FRACTURE in riskDomains) {
            recommendations += ToolRecommendation(
                toolType = ToolType.EMERGENCY_CARD,
                reason = "受伤时可快速展示个人应急信息。",
                priority = 70
            )
            recommendations += ToolRecommendation(
                toolType = ToolType.OFFLINE_GUIDE,
                reason = "查看离线急救指南，减少对模型连续生成的依赖。",
                priority = 60
            )
        }

        if (riskDomains.any { it in DisasterRisks }) {
            recommendations += ToolRecommendation(
                toolType = ToolType.OFFLINE_GUIDE,
                reason = "灾害场景优先查看离线指南。",
                priority = 60
            )
        }

        if (RiskDomain.LOST in riskDomains && recommendations.none { it.toolType == ToolType.OFFLINE_GUIDE }) {
            recommendations += ToolRecommendation(
                toolType = ToolType.OFFLINE_GUIDE,
                reason = "迷路场景可查看离线指南中的求救信号和电量保护内容。",
                priority = 50
            )
        }

        return recommendations
            .distinctBy { it.toolType }
            .sortedByDescending { it.priority }
    }

    private companion object {
        val DisasterRisks = setOf(RiskDomain.FLOOD, RiskDomain.FIRE, RiskDomain.EARTHQUAKE, RiskDomain.THUNDERSTORM)
    }
}
