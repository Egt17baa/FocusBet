package com.citahuellas.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test fun clampedLimits() {
        assertEquals(AppSettings(5, 20), AppSettings(2, 99).clamped())
        assertEquals(AppSettings(300, 1), AppSettings(999, 0).clamped())
        assertEquals(AppSettings(), AppSettings().clamped())
    }
}
