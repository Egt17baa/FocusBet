package com.citahuellas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.citahuellas.data.AppSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(current: AppSettings, padding: PaddingValues, onSave: (AppSettings) -> Unit) {
    var interval by remember(current) { mutableFloatStateOf(current.intervalSec.toFloat()) }
    var attempts by remember(current) { mutableFloatStateOf(current.maxAttempts.toFloat()) }
    var phone by remember(current) { mutableStateOf(current.myPhone) }
    var email by remember(current) { mutableStateOf(current.myEmail) }
    var saved by remember { mutableStateOf(false) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Ajustes")
        GlassCard {
            Text("Ritmo de búsqueda", style = MaterialTheme.typography.titleMedium)
            Text("Cada ${interval.roundToInt()} segundos", modifier = Modifier.padding(top = 8.dp))
            Slider(interval, { interval = (it / 5).roundToInt() * 5f; saved = false }, valueRange = 5f..300f)
            Text("Hasta ${attempts.roundToInt()} intentos")
            Slider(attempts, { attempts = it.roundToInt().toFloat(); saved = false }, valueRange = 1f..20f, steps = 18)
            Text(
                "Cuanto más rápido, más riesgo de que la web te corte un rato. Si pasa, la app se para y te avisa.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GlassCard {
            Text("Mis datos de contacto", style = MaterialTheme.typography.titleMedium)
            Text("Se usan cuando eliges \"Mi número\" al buscar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(phone, { phone = it; saved = false }, label = { Text("Mi teléfono") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = glassFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(email, { email = it.trim(); saved = false }, label = { Text("Mi correo") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = glassFieldColors(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        Button(
            onClick = { onSave(AppSettings(interval.roundToInt(), attempts.roundToInt(), phone.trim(), email.trim())); saved = true },
            shape = CircleShape, modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saved) "Guardado ✓" else "Guardar") }
    }
}
