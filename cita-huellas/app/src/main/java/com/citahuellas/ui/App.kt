package com.citahuellas.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import com.citahuellas.search.SearchUiState
import com.citahuellas.ui.theme.GlassBackground
import com.citahuellas.ui.theme.glass

enum class Tab(val label: String, val icon: ImageVector) {
    PEOPLE("Personas", Icons.Rounded.People), SEARCH("Buscar", Icons.Rounded.Search), SETTINGS("Ajustes", Icons.Rounded.Settings)
}

class AppCallbacks(
    val savePerson: (Profile) -> Unit,
    val deletePerson: (String) -> Unit,
    val saveSettings: (AppSettings) -> Unit,
    val start: (Profile, Boolean) -> Unit,
    val cancel: () -> Unit,
    val step: (Profile, Boolean) -> Unit,
    val fillSms: (String) -> Unit,
    val setSmsCode: (String) -> Unit,
)

@Composable
fun App(people: List<Profile>, settings: AppSettings, search: SearchUiState, smsCode: String, webView: WebView, cb: AppCallbacks, startTab: Tab? = null) {
    var tab by remember { mutableStateOf(startTab ?: if (people.isEmpty()) Tab.PEOPLE else Tab.SEARCH) }
    var editing by remember { mutableStateOf<Profile?>(null) }
    var creating by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var useMine by rememberSaveable { mutableStateOf(true) }

    GlassBackground {
        val dir = LocalLayoutDirection.current
        val sys = WindowInsets.statusBars.asPaddingValues()
        val nav = WindowInsets.navigationBars.asPaddingValues()
        val padding = PaddingValues(
            top = sys.calculateTopPadding(),
            bottom = nav.calculateBottomPadding() + 84.dp,
            end = nav.calculateEndPadding(dir),
        )
        Box(Modifier.fillMaxSize()) {
            when {
                editing != null || creating -> ProfileEditScreen(
                    initial = editing, padding = padding,
                    onSave = { cb.savePerson(it); editing = null; creating = false },
                    onDelete = { cb.deletePerson(it); editing = null },
                    onBack = { editing = null; creating = false },
                )
                tab == Tab.PEOPLE -> PeopleScreen(people, padding) { p -> if (p == null) creating = true else editing = p }
                tab == Tab.SEARCH -> SearchScreen(
                    people, settings, search, webView, padding, smsCode, cb.setSmsCode,
                    selectedId, { selectedId = it }, useMine, { useMine = it },
                    onStart = cb.start, onCancel = cb.cancel, onStep = cb.step, onFillSms = cb.fillSms,
                )
                else -> SettingsScreen(settings, padding, cb.saveSettings)
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .glass(strong = true)
                    .padding(horizontal = 4.dp),
            ) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t && editing == null && !creating,
                        onClick = { tab = t; editing = null; creating = false },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
            }
        }
    }
}
