package com.hermes.mobile.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HermesThemeTest {
    @Test
    fun lightThemeTokensMatchDesktopConsistentDirection() {
        val scheme = HermesThemeTokens.light()

        assertEquals(HermesColors.Background, scheme.background)
        assertEquals(HermesColors.Surface, scheme.surface)
        assertEquals(HermesColors.Blue, scheme.accent)
        assertTrue(scheme.isLight)
    }

    @Test
    fun sectionHeaderTypographyStaysCompact() {
        assertEquals(10, HermesTypography.SectionLabel.sizeSp)
        assertEquals(600, HermesTypography.SectionLabel.weight)
    }
}
