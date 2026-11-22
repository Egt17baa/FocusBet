package com.citahuellas.ui

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import com.citahuellas.search.SearchUiState
import com.citahuellas.search.StatusColor
import com.citahuellas.ui.theme.GlassShape
import com.citahuellas.ui.theme.StatusFound
import com.citahuellas.ui.theme.StatusSearching
import com.citahuellas.ui.theme.StatusStopped
import com.citahuellas.ui.theme.glass
import com.citahuellas.ui.theme.pill

data class Contact(val phone: String, val email: String)

fun contactFor(p: Profile, useMine: Boolean, s: AppSettings) =
    if (useMine) Contact(s.myPhone, s.myEmail.ifBlank { p.email }) else Contact(p.phone, p.email.ifBlank { s.myEmail })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    people: List<Profile>,
    settings: AppSettings,
    state: SearchUiState,
    webView: WebView,
    padding: PaddingValues,
    smsCode: String,
    onSmsCode: (String) -> Unit,
    selectedId: String?,
    onSelect: (String) -> Unit,
    useMine: Boolean,
    onUseMine: (Boolean) -> Unit,
    onStart: (Profile, Boolean) -> Unit,
    onCancel: () -> Unit,
    onStep: (Profile, Boolean) -> Unit,
    onFillSms: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val person = people.firstOrNull { it.id == selectedId } ?: people.firstOrNull()
    val contact = person?.let { contactFor(it, useMine, settings) }
    val contactOk = contact != null && contact.phone.isNotBlank() && contact.email.isNotBlank()

    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!state.active) GlassCard {
            if (people.isEmpty()) {
                Text("Añade primero a una persona en la pestaña Personas.")
            } else {
                ExposedDropdownMenuBox(open, { open = it }) {
                    OutlinedTextField(
                        person?.fullName ?: "", {}, readOnly = true, label = { Text("Persona") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(open, { open = false }) {
                        people.forEach { p -> DropdownMenuItem(text = { Text(p.fullName) }, onClick = { onSelect(p.id); open = false }) }
                    }
                }
                Spacer(Modifier.size(10.dp))
                Text("¿A qué teléfono llega el SMS?", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    SegmentedButton(useMine, { onUseMine(true) }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Mi número") }
                    SegmentedButton(!useMine, { onUseMine(false) }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Su número") }
                }
                if (!contactOk) Text(
                    if (useMine) "Pon tu teléfono y correo en Ajustes." else "Esta persona no tiene teléfono o correo guardado.",
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(Modifier.size(12.dp))
                Button({ person?.let { onStart(it, useMine) } }, enabled = person != null && contactOk, shape = CircleShape, modifier = Modifier.fillMaxWidth()) {
                    Text("Buscar cita · cada ${settings.intervalSec} s, hasta ${settings.maxAttempts} veces")
                }
            }
        }

        StatusPill(state, person?.fullName)

        if (state.active) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onCancel, shape = CircleShape, modifier = Modifier.weight(1f)) { Text("Parar") }
            if (person != null) FilledTonalButton({ onStep(person, useMine) }, shape = CircleShape, modifier = Modifier.weight(1f)) { Text("Hacer este paso") }
        }

        if (state.smsStep) GlassCard(strong = true) {
            Text("Código SMS", style = MaterialTheme.typography.titleMedium)
            Text(
                if (useMine) "Si Android te pregunta, pulsa Permitir y se pondrá solo. O escríbelo aquí:" else "Pídeselo a la persona y escríbelo aquí:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    smsCode, { onSmsCode(it.filter(Char::isDigit).take(8)) }, singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = glassFieldColors(), modifier = Modifier.weight(1f),
                )
                Button({ onFillSms(smsCode) }, enabled = smsCode.length >= 4, shape = CircleShape) { Text("Poner") }
            }
            Text("Después revisa todo y pulsa Confirmar en la web.", modifier = Modifier.padding(top = 6.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth().glass(strong = true).clip(GlassShape).padding(4.dp)) {
            AndroidView(
                factory = { (webView.parent as? ViewGroup)?.removeView(webView); webView },
                modifier = Modifier.fillMaxSize().clip(GlassShape),
            )
        }
    }
}

@Composable
private fun StatusPill(state: SearchUiState, name: String?) {
    val color = when (state.color) {
        StatusColor.SEARCHING -> StatusSearching
        StatusColor.FOUND -> StatusFound
        StatusColor.STOPPED -> StatusStopped
        StatusColor.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth().glass().pill(color, 24.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).pill(color).padding(6.dp))
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            if (state.active && name != null) Text(name, style = MaterialTheme.typography.labelLarge)
            Text(state.status, style = MaterialTheme.typography.bodyLarge)
        }
        state.secondsLeft?.let { Text("${it}s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    }
}
