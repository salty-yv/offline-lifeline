package com.example.offlinelifeline.safety

import com.example.offlinelifeline.core.model.RiskDomain

class SafetyConstraintBuilder {
    fun buildSafetyInstruction(
        riskDomains: Set<RiskDomain>,
        hasImageInput: Boolean = false,
        imageInputSupported: Boolean = false
    ): String {
        val activeRules = SafetyRules.forRisks(riskDomains)

        return buildString {
            appendLine("Give conservative, actionable safety advice.")
            appendLine("You are not a doctor, not a rescue dispatcher, and cannot replace professional rescue.")
            appendLine("If information is incomplete, give immediate safe actions first, then ask at most 3 key questions.")
            appendLine("Local tools may only be recommended; do not claim they have been automatically executed.")
            appendLine("If images are involved, do not make a definitive medical diagnosis, species identification, or wild food safety judgment from an image.")
            appendLine("For wound images, give conservative first aid advice and ask about bleeding, pain, sensation, movement, and consciousness.")
            appendLine("For mushroom, plant, berry, or wild food images, never say it is safe to eat.")
            appendLine("For river, flood, storm, or fire images, warn about water flow, cold exposure, contamination, smoke, unstable ground, and evacuation risk as relevant.")

            if (hasImageInput && !imageInputSupported) {
                appendLine("The runtime cannot inspect image pixels. Do not claim to see the image. Ask the user to describe the image in text.")
            }

            if (activeRules.isNotEmpty()) {
                appendLine()
                appendLine("Banned actions:")
                activeRules
                    .flatMap { it.bannedPatterns }
                    .distinct()
                    .forEach { appendLine("- Do not suggest or imply: $it") }

                appendLine()
                appendLine("Required reminders:")
                activeRules
                    .flatMap { it.requiredReminders }
                    .distinct()
                    .forEach { appendLine("- $it") }
            }
        }.trim()
    }
}
