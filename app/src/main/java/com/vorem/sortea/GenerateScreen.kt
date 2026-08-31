package com.vorem.sortea

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vorem.sortea.data.DEFAULT_TICKETS
import com.vorem.sortea.data.MAX_TICKETS
import com.vorem.sortea.data.PdfGenerator
import com.vorem.sortea.data.SorteaConfig
import com.vorem.sortea.data.SorteaStorage
import com.vorem.sortea.data.TicketGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Paso final: aqui se dispara la generacion real del PDF a partir de lo
 * definido en la pantalla de Generar boletas y en Ajustes. Los PDF se
 * guardan en una carpeta propia "Sortea" en la raiz del almacenamiento, y
 * cada tanda nueva borra los PDF de la tanda anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(state: SorteaUiState, onBack: () -> Unit) {
    var message by remember { mutableStateOf<String?>(null) }
    var generatedCount by remember { mutableStateOf(0) }
    var generatedFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var hasAccess by remember { mutableStateOf(SorteaStorage.hasFullAccess()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAccess = granted }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasAccess = SorteaStorage.hasFullAccess() }

    fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            allFilesLauncher.launch(intent)
        } else {
            legacyPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun readConfig(): SorteaConfig? {
        val quantity = if (state.quantityText.isBlank()) DEFAULT_TICKETS else state.quantityText.toIntOrNull()
        val width = state.widthText.replace(',', '.').toDoubleOrNull()
        val height = state.heightText.replace(',', '.').toDoubleOrNull()
        val spacing = state.spacingText.replace(',', '.').toDoubleOrNull()
        if (quantity == null || quantity !in 1..MAX_TICKETS || width == null || width <= 0 || height == null || height <= 0 || spacing == null || spacing < 0) {
            message = "Revisa la cantidad (1-$MAX_TICKETS) y las medidas en Ajustes antes de generar."
            return null
        }
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
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Cantidad de boletas: ${state.quantityText.ifBlank { "$DEFAULT_TICKETS (por defecto)" }}")
                    Text("Tamaño: ${state.widthText} × ${state.heightText} cm · separación ${state.spacingText} mm")
                    Text("Fecha del sorteo: ${state.date}")
                }
            }

            if (!hasAccess) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permiso de almacenamiento", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Para guardar los PDF en la carpeta Sortea (en la raíz del almacenamiento) se necesita autorizar el acceso.")
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { requestAccess() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Conceder permiso")
                        }
                    }
                }
            }

            Button(
                enabled = !busy && hasAccess,
                onClick = {
                    readConfig()?.let { config ->
                        busy = true
                        message = null
                        generatedFiles = emptyList()
                        scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val tickets = TicketGenerator.generate(config)
                                    PdfGenerator.generate(context, config, tickets)
                                }
                                generatedCount = config.quantity
                                generatedFiles = result.files.map { it.name }
                                message = "Boletas generadas correctamente: ${result.files.size} PDF(s)."
                            } catch (e: Exception) {
                                message = "No se pudo generar el PDF: ${e.message ?: "error desconocido"}"
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Generando PDF…" else "Generar PDF") }

            AnimatedVisibility(visible = busy, enter = fadeIn(), exit = fadeOut()) {
                GeneratingPdfAnimation()
            }

            message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            if (!busy && generatedCount > 0) {
                Text("${generatedCount} boleta(s) creadas. Archivos guardados en la carpeta \"Sortea\" en el almacenamiento del dispositivo.")
                generatedFiles.forEach { Text("• $it") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GeneratingPdfAnimation() {
    val transition = rememberInfiniteTransition(label = "pdf-generation")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "rotation"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Creando tus boletas…", style = MaterialTheme.typography.bodyMedium)
    }
}
