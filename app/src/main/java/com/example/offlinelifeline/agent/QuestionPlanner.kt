package com.example.offlinelifeline.agent

import com.example.offlinelifeline.core.model.AgentContext
import com.example.offlinelifeline.core.model.RiskDomain

class QuestionPlanner {
    fun planQuestions(context: AgentContext): List<String> {
        val questions = mutableListOf<String>()
        val risks = context.riskDomains

        if (RiskDomain.BLEEDING in risks || RiskDomain.FRACTURE in risks || RiskDomain.SNAKE_BITE in risks) {
            questions += "你现在有没有大量出血、麻木、变形、意识模糊或呼吸困难？"
        }

        if (context.batteryPercent == null) {
            questions += "手机还剩多少电？"
        }

        if (context.userCanMove == null && (RiskDomain.LOST in risks || RiskDomain.INJURY in risks)) {
            questions += "你现在还能缓慢移动吗，还是不能负重？"
        }

        if ((context.hasWater == null || context.hasWarmClothes == null) && (RiskDomain.LOST in risks || RiskDomain.NIGHT in risks)) {
            questions += "你身边有没有水、保暖衣物或可以避风的地方？"
        }

        return questions.distinct().take(MAX_QUESTIONS)
    }

    private companion object {
        const val MAX_QUESTIONS = 3
    }
}
