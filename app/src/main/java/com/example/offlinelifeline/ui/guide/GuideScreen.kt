package com.example.offlinelifeline.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.offlinelifeline.data.db.GuideEntity
import com.example.offlinelifeline.ui.components.LifelineCard
import com.example.offlinelifeline.ui.components.LifelineTopBar
import com.example.offlinelifeline.ui.i18n.LocalAppStrings

@Composable
fun GuideScreen(
    viewModel: GuideViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val selectedGuide = uiState.selectedGuide?.localizedFor(strings.languageTag)
    BackHandler(enabled = selectedGuide != null) {
        viewModel.closeDetail()
    }

    if (selectedGuide != null) {
        GuideDetailScreen(
            guide = selectedGuide,
            onBack = viewModel::closeDetail,
            modifier = modifier
        )
    } else {
        GuideListScreen(
            uiState = uiState,
            onQueryChanged = viewModel::onQueryChanged,
            onGuideSelected = viewModel::selectGuide,
            modifier = modifier
        )
    }
}

@Composable
private fun GuideListScreen(
    uiState: GuideUiState,
    onQueryChanged: (String) -> Unit,
    onGuideSelected: (GuideEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val visibleGuides = remember(
        uiState.guides,
        uiState.visibleGuides,
        uiState.query,
        strings.languageTag
    ) {
        if (strings.languageTag.startsWith("en")) {
            val query = uiState.query.trim()
            uiState.guides
                .map { it.localizedFor(strings.languageTag) }
                .filter { guide ->
                    query.isBlank() ||
                        guide.title.contains(query, ignoreCase = true) ||
                        guide.summary.contains(query, ignoreCase = true) ||
                        guide.tags.contains(query, ignoreCase = true) ||
                        guide.body.contains(query, ignoreCase = true)
                }
        } else {
            uiState.visibleGuides
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LifelineTopBar(title = strings.routeGuide)
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text(strings.searchGuides) },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.weight(1f))
            uiState.errorMessage != null -> MessageState(
                text = uiState.errorMessage,
                modifier = Modifier.weight(1f)
            )
            visibleGuides.isEmpty() -> MessageState(
                text = strings.noGuideResults,
                modifier = Modifier.weight(1f)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(visibleGuides.chunked(2)) { rowGuides ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowGuides.forEach { guide ->
                            GuideListItem(
                                guide = guide,
                                onClick = { onGuideSelected(guide) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowGuides.size == 1) {
                            Column(modifier = Modifier.weight(1f)) {}
                        }
                    }
                }
            }
        }
    }
}

private fun GuideEntity.localizedFor(languageTag: String): GuideEntity {
    if (!languageTag.startsWith("en")) return this
    val text = EnglishGuideText[id] ?: return this
    return copy(
        title = text.title,
        summary = text.summary,
        body = text.body,
        tags = text.tags
    )
}

private data class GuideText(
    val title: String,
    val summary: String,
    val body: String,
    val tags: String
)

private val EnglishGuideText = mapOf(
    "lost" to GuideText(
        "Lost",
        "Stop wandering, save strength and battery, and create a visible help signal.",
        """
        Do these 3 things first:
        1. Stop moving blindly.
        2. Choose a safer place with shelter and low fall risk.
        3. Lower brightness and prepare one help message with location clues, injuries, and battery level.

        Next few minutes:
        - Recall the last certain landmark, junction, stream, sign, or building.
        - Mark your position with clothing, stones, screen SOS, or flashlight.
        - If it is getting dark or you are injured, prioritize warmth and waiting for rescue.

        Do not:
        - Do not keep trying unknown routes.
        - Do not waste battery on repeated high-power actions.
        - Do not stay near cliffs, steep slopes, or water edges.
        """.trimIndent(),
        "lost,mountain,night,help,battery"
    ),
    "bleeding" to GuideText(
        "Bleeding",
        "Apply direct pressure, raise the wound if possible, and seek help for severe bleeding.",
        """
        Do these 3 things first:
        1. Press directly on the bleeding point with clean cloth.
        2. Raise the injured area above the heart if you can.
        3. Seek help quickly for heavy, spurting, or uncontrolled bleeding.

        Next few minutes:
        - Keep pressure steady and avoid checking too often.
        - Keep the person warm and watch for shock.
        - Note the time and any change in bleeding.

        Do not:
        - Do not pack the wound with dirt, herbs, or dirty material.
        - Do not cut the wound or try to drain blood.
        """.trimIndent(),
        "bleeding,wound,pressure,first aid"
    ),
    "sprain_fracture" to GuideText(
        "Sprain or Suspected Fracture",
        "Stop weight-bearing, stabilize the area, and watch for deformity, numbness, or severe pain.",
        """
        Do these 3 things first:
        1. Stop walking or loading the injured part.
        2. Stabilize it with clothing, sticks, or backpack straps.
        3. Treat deformity, numbness, severe pain, or inability to move as possible fracture.

        Next few minutes:
        - Keep the area still and reduce movement.
        - Cool it if possible, but avoid freezing the skin.
        - Consider night, cold, and getting-lost risk before moving.

        Do not:
        - Do not force the bone or joint back into place.
        - Do not keep walking through strong pain.
        """.trimIndent(),
        "sprain,fracture,ankle,stabilize,pain"
    ),
    "burn" to GuideText(
        "Burns",
        "Cool with clean running water, cover lightly, and seek help for serious burns.",
        """
        Do these 3 things first:
        1. Move away from the heat source.
        2. Cool the burn with clean running water.
        3. Cover it lightly with clean dressing or cloth.

        Next few minutes:
        - Remove rings, watches, or tight items nearby.
        - Seek help for face, hand, groin, large, or deep burns.

        Do not:
        - Do not apply toothpaste, grease, or unknown ointments.
        - Do not pop blisters.
        """.trimIndent(),
        "burn,scald,blister,cooling"
    ),
    "hypothermia" to GuideText(
        "Hypothermia",
        "Get out of wind and rain, change wet clothes, warm slowly, and avoid alcohol.",
        """
        Do these 3 things first:
        1. Leave wind, rain, snow, and cold ground.
        2. Remove wet clothes and wrap the head, neck, and trunk.
        3. If awake and able to swallow, sip warm drinks.

        Next few minutes:
        - Share warmth with companions if safe.
        - Confusion, clumsiness, or stopped shivering means high risk.

        Do not:
        - Do not drink alcohol for warmth.
        - Do not place very hot objects directly on skin.
        """.trimIndent(),
        "hypothermia,cold,shivering,wet,warmth"
    ),
    "heatstroke" to GuideText(
        "Heat Illness",
        "Move to shade, cool down, sip fluids if awake, and seek help for confusion or collapse.",
        """
        Do these 3 things first:
        1. Stop activity and move to shade or ventilation.
        2. Loosen tight clothing and cool the neck, armpits, and groin.
        3. If awake, sip water and electrolytes.

        Next few minutes:
        - Watch for confusion, seizure, very high temperature, or inability to drink.
        - Seek help quickly for severe symptoms.

        Do not:
        - Do not continue in direct heat.
        - Do not force water into an unconscious person.
        """.trimIndent(),
        "heat,heatstroke,sun,dizziness,cooling"
    ),
    "dehydration" to GuideText(
        "Dehydration",
        "Reduce activity, drink small amounts often, and avoid drinking too much at once.",
        """
        Do these 3 things first:
        1. Stop unnecessary activity and move to shade.
        2. Drink small amounts repeatedly.
        3. Add salt or electrolytes if available.

        Next few minutes:
        - Watch urine, dizziness, heartbeat, and mental clarity.
        - If water is unsafe, do not risk contaminated sources.

        Do not:
        - Do not drink a very large amount at once.
        - Do not drink alcohol or lots of caffeine.
        """.trimIndent(),
        "dehydration,thirst,no water,hydration"
    ),
    "snake_bite" to GuideText(
        "Snake or Insect Bite",
        "Move less, immobilize the limb, and do not suck, cut, or bleed the wound.",
        """
        Do these 3 things first:
        1. Move away from the animal; remember its appearance but do not chase it.
        2. Reduce movement and immobilize the limb.
        3. Contact rescue or medical care as soon as possible.

        Next few minutes:
        - Remove rings, watches, or tight items.
        - Record bite time and symptom changes.
        - Trouble breathing, widespread rash, or face/lip swelling may mean severe allergy.

        Do not:
        - Do not suck venom.
        - Do not cut the wound or bleed it.
        - Do not ice it or drink alcohol.
        """.trimIndent(),
        "snake bite,insect bite,venom,allergy,immobilize"
    ),
    "poisoning" to GuideText(
        "Accidental Ingestion",
        "Stop eating, keep a sample, and do not assume wild food is safe.",
        """
        Do these 3 things first:
        1. Stop eating immediately.
        2. Keep the remaining food, packaging, or a photo.
        3. Seek help for vomiting, abdominal pain, confusion, or breathing trouble.

        Next few minutes:
        - Record time eaten, amount, and symptoms.
        - If awake, sip water while waiting for professional guidance.

        Do not:
        - Do not assume unknown mushrooms, berries, or plants are safe.
        - Do not induce vomiting unless a professional tells you to.
        """.trimIndent(),
        "poisoning,mushroom,berries,plants,toxic"
    ),
    "thunderstorm" to GuideText(
        "Thunderstorm",
        "Avoid isolated high objects, water, and metal; lower your body position.",
        """
        Do these 3 things first:
        1. Leave peaks, open ground, water edges, and isolated trees.
        2. Spread out from companions and lower your body.
        3. Turn off unnecessary electronics and save battery.

        Next few minutes:
        - Look for a lower area that is not flooding.
        - Move again only after thunder has clearly passed farther away.

        Do not:
        - Do not shelter under an isolated tree.
        - Do not stand in water or beside metal railings.
        """.trimIndent(),
        "lightning,thunderstorm,storm,shelter"
    ),
    "flood" to GuideText(
        "Flood",
        "Stay away from moving water, move higher if safe, and do not force a crossing.",
        """
        Do these 3 things first:
        1. Leave river channels, low areas, and underpasses.
        2. Move to higher ground if you can do it safely.
        3. Save battery and prepare a visible help signal.

        Next few minutes:
        - Watch water level and escape routes.
        - Keep companions visible to each other.

        Do not:
        - Do not force a water crossing.
        - Do not stay near water to retrieve items or take photos.
        """.trimIndent(),
        "flood,rising water,river,crossing,evacuation"
    ),
    "fire" to GuideText(
        "Fire",
        "Avoid smoke and flames, move low, and do not return for belongings.",
        """
        Do these 3 things first:
        1. Check smoke and flame direction and move toward a safe exit or upwind.
        2. Cover nose and mouth with damp cloth if possible and move low.
        3. Call for help or signal after reaching a safer area.

        Next few minutes:
        - Stay away from fuel, smoke channels, and enclosed spaces.
        - If trapped, block door gaps and signal from a window or open area.

        Do not:
        - Do not return for belongings.
        - Do not use elevators.
        """.trimIndent(),
        "fire,smoke,burn,evacuation"
    ),
    "earthquake" to GuideText(
        "Earthquake",
        "Protect head and neck, avoid glass and tall objects, and leave unsafe buildings after shaking stops.",
        """
        Do these 3 things first:
        1. Take cover nearby and protect head and neck.
        2. Stay away from glass, hanging objects, and tall furniture.
        3. Evacuate calmly after shaking stops.

        Next few minutes:
        - Watch for aftershocks and falling debris.
        - If injured or trapped, save strength and signal by knocking or using the screen.

        Do not:
        - Do not enter damaged buildings.
        - Do not use flame to check for gas leaks.
        """.trimIndent(),
        "earthquake,aftershock,building,collapse"
    ),
    "rescue_signal" to GuideText(
        "Help Signal",
        "Make signals short, repeated, and visible; prioritize location and injuries.",
        """
        Do these 3 things first:
        1. Prepare one fixed help message: where you are, what happened, people count, injuries, and battery.
        2. Use screen SOS, flashlight, or ground marks as repeated signals.
        3. Send or display signals at intervals to avoid draining the battery.

        Next few minutes:
        - Use open areas, reflective items, or bright clothing to improve visibility.
        - At night, prefer bright screen or intermittent flashing.

        Do not:
        - Do not keep the screen bright until the battery dies.
        - Do not move far away from a place where you already left signals.
        """.trimIndent(),
        "SOS,help,signal,location,screen"
    ),
    "battery" to GuideText(
        "Battery Protection",
        "Lower brightness, disable high-drain features, and reduce repeated model chats.",
        """
        Do these 3 things first:
        1. Lower brightness and disable vibration and unnecessary background apps.
        2. Turn off Wi-Fi, Bluetooth, and location, or use airplane mode.
        3. Check offline guides first and reduce repeated model generation.

        Next few minutes:
        - Prepare one fixed help message and send it at intervals.
        - Use screen SOS or flashlight only when necessary.

        Do not:
        - Do not run long repeated chats.
        - Do not continuously record video or use bright lighting.
        """.trimIndent(),
        "low battery,power saving,airplane mode,brightness"
    )
)

@Composable
private fun GuideListItem(
    guide: GuideEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = guide.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = guide.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GuideDetailScreen(
    guide: GuideEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            LifelineTopBar(
                title = guide.title,
                navigationIcon = Icons.Default.ArrowBack,
                navigationContentDescription = strings.backToList,
                onNavigationClick = onBack
            )
        }

        item {
            LifelineCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = guide.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = guide.body,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageState(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.widthIn(max = 320.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
