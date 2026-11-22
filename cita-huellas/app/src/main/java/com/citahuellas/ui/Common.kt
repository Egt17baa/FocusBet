package com.citahuellas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.citahuellas.ui.theme.glass

@Composable
fun GlassCard(modifier: Modifier = Modifier, strong: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().glass(strong = strong).padding(18.dp), content = content)
}

@Composable
fun ScreenTitle(text: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 6.dp, top = 8.dp, bottom = 12.dp)) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun glassFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.25f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
)
