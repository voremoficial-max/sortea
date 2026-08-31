package com.vorem.sortea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vorem.sortea.data.PdfLayout

/**
 * Ajustes de tamaño de la boleta, separados de la Configuración principal
 * (cantidad, suertes y fecha viven en ConfigScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SorteaUiState, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ajustes de tamaño") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tamaño de la boleta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    state.widthText,
                    { state.widthText = it },
                    Modifier.weight(1f),
                    label = { Text("Ancho cm") },
                    singleLine = true
                )
                OutlinedTextField(
                    state.heightText,
                    { state.heightText = it },
                    Modifier.weight(1f),
                    label = { Text("Alto cm") },
                    singleLine = true
                )
            }
            OutlinedTextField(
                state.spacingText,
                { state.spacingText = it },
                Modifier.fillMaxWidth(),
                label = { Text("Separación mm") },
                singleLine = true
            )

            Spacer(Modifier.height(4.dp))

            val width = state.widthText.replace(',', '.').toDoubleOrNull()
            val height = state.heightText.replace(',', '.').toDoubleOrNull()
            val spacing = state.spacingText.replace(',', '.').toDoubleOrNull()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (width != null && width > 0 && height != null && height > 0 && spacing != null && spacing >= 0) {
                        val grid = PdfLayout.grid(width, height, spacing)
                        Text(
                            "Distribución en hoja carta: ${grid.columns} × ${grid.rows} = ${grid.perPage} boletas por página",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("Ingresa medidas válidas para ver la distribución por hoja.")
                    }
                }
            }
        }
    }
}
