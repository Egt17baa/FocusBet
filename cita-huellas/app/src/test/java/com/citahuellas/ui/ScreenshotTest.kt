package com.citahuellas.ui

import android.webkit.WebView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import com.citahuellas.search.SearchUiState
import com.citahuellas.search.StatusColor
import com.citahuellas.ui.theme.CitaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Capturas para revisar el aspecto (no comprueban lógica). Salida: app/build/outputs/roborazzi/ */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val people = listOf(
        Profile("1", "Ana Pérez Quispe", "X1234567L", "PERU", "600111222", "ana@example.com", "Renovación, caduca en marzo"),
        Profile("2", "Luis Martínez", "Y7654321B", "COLOMBIA", "600333444", "luis@example.com"),
    )
    private val cb = AppCallbacks({}, {}, {}, { _, _ -> }, {}, {}, {}, {}, { _, _ -> }, {}, {})

    private fun shot(name: String, dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { CitaTheme(dark = dark) { content() } }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun web() = WebView(ApplicationProvider.getApplicationContext())

    @Test fun people() = shot("1-personas") {
        App(people, AppSettings(myPhone = "611", myEmail = "yo@x.es"), SearchUiState(), "", web(), cb, startTab = Tab.PEOPLE)
    }

    @Test fun searching() = shot("2-buscando") {
        App(people, AppSettings(myPhone = "611", myEmail = "yo@x.es"),
            SearchUiState("No hay citas. Reintento 3 de 10", 3, 10, 42, StatusColor.SEARCHING), "", web(), cb, startTab = Tab.SEARCH)
    }

    @Test fun found() = shot("3-cita-encontrada") {
        App(people, AppSettings(myPhone = "611", myEmail = "yo@x.es"),
            SearchUiState("Elige el hueco y pon el código SMS. Confirma tú.", 4, 10, null, StatusColor.FOUND, smsStep = true), "4829", web(), cb, startTab = Tab.SEARCH)
    }

    @Test fun settingsDark() = shot("4-ajustes-oscuro", dark = true) {
        App(people, AppSettings(myPhone = "611222333", myEmail = "yo@example.com"), SearchUiState(), "", web(), cb, startTab = Tab.SETTINGS)
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onRoot() =
    this.onNode(androidx.compose.ui.test.isRoot())
