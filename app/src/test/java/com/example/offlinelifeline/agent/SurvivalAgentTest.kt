package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.RiskDomain
import com.example.offlinelifeline.core.model.ToolType
import org.junit.Assert.assertTrue
import org.junit.Test

class SurvivalAgentTest {
    private val contextManager = ContextManager()
    private val riskClassifier = RiskClassifier()
    private val toolRouter = ToolRouter()

    @Test
    fun classifyLostLowBatteryNightAndRecommendTools() {
        val input = "我迷路了，手机只有 12% 电，天快黑了"
        val context = contextManager.buildContext(emptyList())
        val risks = riskClassifier.classify(input, context.copy(batteryPercent = 12))
        val tools = toolRouter.recommendTools(risks, context.copy(riskDomains = risks, batteryPercent = 12))
            .map { it.toolType }

        assertTrue(RiskDomain.LOST in risks)
        assertTrue(RiskDomain.LOW_BATTERY in risks)
        assertTrue(RiskDomain.NIGHT in risks)
        assertTrue(ToolType.BATTERY_SAVER_ADVICE in tools)
        assertTrue(ToolType.SCREEN_SOS in tools)
        assertTrue(ToolType.OFFLINE_GUIDE in tools)
    }
}
