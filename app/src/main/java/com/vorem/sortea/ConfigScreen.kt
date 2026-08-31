package com.vorem.sortea

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vorem.sortea.data.DEFAULT_TICKETS
import com.vorem.sortea.data.MAX_TICKETS
import com.vorem.sortea.data.SorteaConfig
import com.vorem.sortea.data.SorteaTicket
import com.vorem.sortea.data.TicketGenerator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    state: SorteaUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onGenerate: () -> Unit
) {
    var message by remember { mutableStateOf<String?>(null) }
    var previewTicket by remember { mutableStateOf<SorteaTicket?>(null) }
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE dd 'de' MMMM 'de' yyyy", Locale("es", "CO")) }

    fun currentSizeOrDefault(): Triple<Double, Double, Double> {
        val width = state.widthText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 2.5
        val height = state.heightText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 3.8
        val spacing = state.spacingText.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } ?: 1.0
        return Triple(width, height, spacing)
    }

    fun readConfig(): SorteaConfig? {
        val quantity = if (state.quantityText.isBlank()) DEFAULT_TICKETS else state.quantityText.toIntOrNull()
        if (quantity == null || quantity !in 1..MAX_TICKETS) {
            message = "Revisa la cantidad: debe estar entre 1 y $MAX_TICKETS."
            return null
        }
        val (width, height, spacing) = currentSizeOrDefault()
        return SorteaConfig(quantity, state.prizes, state.date, width, height, spacing)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Generar boletas") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Atrás") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Cantidad de boletas (opcional, $DEFAULT_TICKETS por defecto)", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                state.quantityText,
                { state.quantityText = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Cantidad") },
                placeholder = { Text("$DEFAULT_TICKETS") }
            )

            Text("10 suertes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.prizes.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> state.prizes = state.prizes.toMutableList().also { it[index] = input }.toList() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Suerte ${index + 1}") }
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Fecha del sorteo", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.date.format(dateFormatter).uppercase(Locale("es", "CO")),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> state.date = LocalDate.of(year, month + 1, day) },
                            state.date.year, state.date.monthValue - 1, state.date.dayOfMonth
                        ).show()
                    }) { Text("Seleccionar fecha") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tamaño de boleta", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${state.widthText} × ${state.heightText} cm · separación ${state.spacingText} mm")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenSettings) { Text("Cambiar en Ajustes") }
                }
            }

            Text("Vista previa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TicketPreview(ticket = previewTicket ?: TicketGenerator.generate(SorteaConfig(prizes = state.prizes, drawDate = state.date)).first())

            OutlinedButton(
                onClick = {
                    readConfig()?.let { config ->
                        previewTicket = TicketGenerator.generate(config).first()
                        message = "Vista previa actualizada."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Actualizar vista previa") }

            Button(
                onClick = {
                    if (readConfig() != null) onGenerate()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continuar a generar boletas") }

            message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TicketPreview(ticket: SorteaTicket) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(width = 256.dp, height = 384.dp)
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.vorem.sortea.R.drawable.boleta_template),
                contentDescription = "Plantilla de boleta GANADIARIO",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                fun box(l: Float, t: Float, r: Float, b: Float) = androidx.compose.ui.geometry.Rect(w*l, h*t, w*r, h*b)
                fun textIn(rect: androidx.compose.ui.geometry.Rect, text: String, textSize: Float, letterSpacing: Float = 0f) {
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(17,17,17)
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            this.textSize = textSize
                            this.letterSpacing = letterSpacing
                        }
                        val y = rect.center.y - (p.ascent() + p.descent()) / 2f
                        drawText(text, rect.center.x, y, p)
                    }
                }
                textIn(box(0.16f,0.215f,0.465f,0.274f), ticket.number1Formatted(), h*0.050f)
                textIn(box(0.515f,0.215f,0.95f,0.274f), ticket.number2Formatted(), h*0.050f)
                val prizeBoxes = listOf(
                    box(0.022f,0.366f,0.202f,0.493f), box(0.218f,0.366f,0.397f,0.493f), box(0.414f,0.366f,0.593f,0.493f), box(0.610f,0.366f,0.789f,0.493f), box(0.806f,0.366f,0.985f,0.493f),
                    box(0.022f,0.581f,0.202f,0.708f), box(0.218f,0.581f,0.397f,0.708f), box(0.414f,0.581f,0.593f,0.708f), box(0.610f,0.581f,0.789f,0.708f), box(0.806f,0.581f,0.985f,0.708f)
                )
                prizeBoxes.forEachIndexed { i, r ->
                    val text = ticket.prizes.getOrNull(i).orEmpty().ifBlank { "TU SUERTE" }
                    textIn(r, text, h*0.025f)
                }
                textIn(
                    box(0.48f,0.758f,0.93f,0.825f),
                    ticket.drawDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "CO"))),
                    h*0.030f,
                    letterSpacing = 0.10f
                )
            }
        }
    }
}
