package com.mahavtaar.panchanga.feature.swara

import androidx.compose.runtime.Immutable
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.SwaraTier

enum class SwaraTabType {
    LIVE,
    GUIDANCE,
    TATTVAS,
    ALIGNMENT
}

@Immutable
data class SwaraVisibilityConfig(
    val tier: SwaraTier,
    val includeLunarWindows: Boolean,
    val showHeroTattvaAndDetails: Boolean,
    val showSlokaCard: Boolean,
    val showBoundarySwaras: Boolean,
    val showGuidanceWorks: Boolean,
    val showTithiRulesTable: Boolean,
    val showTattvaExplorer: Boolean,
    val showNakshatraAlignment: Boolean,
    val showMasterTables: Boolean,
    val visibleTabs: List<SwaraTabType>
)

fun resolveSwaraVisibility(policy: AccessPolicy): SwaraVisibilityConfig {
    val tier = when {
        policy.hasFeature(AppFeatureKey.SWARA_TIER_TAPASVI) -> SwaraTier.TAPASVI
        policy.hasFeature(AppFeatureKey.SWARA_TIER_YOGI) -> SwaraTier.YOGI
        else -> SwaraTier.SADHAKA
    }

    val includeLunarWindows = tier == SwaraTier.YOGI || tier == SwaraTier.TAPASVI
    val isTapasvi = tier == SwaraTier.TAPASVI

    val showGuidanceWorks = policy.hasFeature(AppFeatureKey.SWARA_GUIDANCE_WORKS)
    val showTithiRulesTable = policy.hasFeature(AppFeatureKey.SWARA_TITHI_RULES_TABLE)
    val showTattvaExplorer = policy.hasFeature(AppFeatureKey.SWARA_TATTVA_EXPLORER)
    val showNakshatraAlignment = policy.hasFeature(AppFeatureKey.SWARA_NAKSHATRA_ALIGNMENT)
    val showMasterTables = policy.hasFeature(AppFeatureKey.SWARA_MASTER_TABLES)

    val visibleTabs = buildList {
        add(SwaraTabType.LIVE)
        if (showGuidanceWorks || showTithiRulesTable) add(SwaraTabType.GUIDANCE)
        if (showTattvaExplorer) add(SwaraTabType.TATTVAS)
        if (showNakshatraAlignment || showMasterTables) add(SwaraTabType.ALIGNMENT)
    }

    return SwaraVisibilityConfig(
        tier = tier,
        includeLunarWindows = includeLunarWindows,
        showHeroTattvaAndDetails = isTapasvi,
        showSlokaCard = isTapasvi,
        showBoundarySwaras = isTapasvi,
        showGuidanceWorks = showGuidanceWorks,
        showTithiRulesTable = showTithiRulesTable,
        showTattvaExplorer = showTattvaExplorer,
        showNakshatraAlignment = showNakshatraAlignment,
        showMasterTables = showMasterTables,
        visibleTabs = visibleTabs
    )
}

internal fun SwaraTier.localizedLabel(lang: AppLanguage): String = when (this) {
    SwaraTier.SADHAKA -> if (lang == AppLanguage.HINDI) "साधक" else "Sādhaka"
    SwaraTier.YOGI -> if (lang == AppLanguage.HINDI) "योगी" else "Yogī"
    SwaraTier.TAPASVI -> if (lang == AppLanguage.HINDI) "तपस्वी" else "Tapasvī"
}
