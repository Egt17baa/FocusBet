package com.citahuellas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.citahuellas.data.Profile
import com.citahuellas.engine.normalizeText
import java.util.UUID

private val NIE = Regex("^[XYZ]\\d{7}[A-Z]$")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(initial: Profile?, padding: PaddingValues, onSave: (Profile) -> Unit, onDelete: (String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(initial?.fullName ?: "") }
    var nie by remember { mutableStateOf(initial?.nie ?: "") }
    var country by remember { mutableStateOf(initial?.country ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var countryOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val nieOk = nie.isBlank() || NIE.matches(nie.trim().uppercase())
    val emailOk = email.isBlank() || "@" in email
    val canSave = name.isNotBlank() && nie.isNotBlank() && country.isNotBlank()

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(if (initial == null) "Nueva persona" else "Editar persona")
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre y apellidos") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nie, { nie = it.uppercase().trim() }, label = { Text("NIE") }, singleLine = true,
                    isError = !nieOk,
                    supportingText = { if (!nieOk) Text("Formato habitual: X1234567L (puedes guardarlo igualmente)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(countryOpen, { countryOpen = it }) {
                    OutlinedTextField(country, { country = it; countryOpen = true }, label = { Text("País de nacionalidad") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(countryOpen) },
                        supportingText = { Text("Como aparece en la web, p. ej. PERU o REPUBLICA DOMINICANA") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = glassFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable))
                    val matches = COUNTRIES.filter { normalizeText(it).contains(normalizeText(country)) }
                    if (matches.isNotEmpty()) ExposedDropdownMenu(countryOpen, { countryOpen = false }) {
                        matches.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { country = c; countryOpen = false }) }
                    }
                }
                OutlinedTextField(phone, { phone = it }, label = { Text("Su teléfono (opcional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(email, { email = it.trim() }, label = { Text("Su correo (opcional)") }, singleLine = true,
                    isError = !emailOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Nota") }, placeholder = { Text("p. ej. renovación, caduca en marzo") },
                    colors = glassFieldColors(), modifier = Modifier.fillMaxWidth())
            }
        }
        Text("🔒 Se guarda cifrado solo en este móvil.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onBack, shape = CircleShape, modifier = Modifier.weight(1f)) { Text("Volver") }
            Button(
                onClick = {
                    onSave(Profile(initial?.id ?: UUID.randomUUID().toString(), name.trim(), nie.trim().uppercase(), country.trim(), phone.trim(), email.trim(), note.trim()))
                },
                enabled = canSave, shape = CircleShape, modifier = Modifier.weight(1f),
            ) { Text("Guardar") }
        }
        if (initial != null) TextButton({ confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Borrar a esta persona", color = MaterialTheme.colorScheme.error)
        }
    }
    if (confirmDelete && initial != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("¿Borrar a ${initial.fullName}?") },
        text = { Text("Se borran sus datos de este móvil.") },
        confirmButton = { TextButton({ confirmDelete = false; onDelete(initial.id) }) { Text("Borrar") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancelar") } },
    )
}
