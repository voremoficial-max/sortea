package com.vorem.sortea

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * Estado compartido entre pantallas de la app (Generar boletas y Ajustes).
 * Se crea una sola vez en [SorteaApp] con `remember` y se pasa a cada
 * pantalla, asi los valores no se pierden al navegar entre ellas.
 *
 * La cantidad es opcional: si el usuario la deja vacia se usan 499
 * boletas por defecto (ver [com.vorem.sortea.data.DEFAULT_TICKETS]).
 */
class SorteaUiState {
    var quantityText by mutableStateOf("")
    var prizes by mutableStateOf(List(10) { "" })
    var date by mutableStateOf(LocalDate.now())
    var widthText by mutableStateOf("2.5")
    var heightText by mutableStateOf("3.8")
    var spacingText by mutableStateOf("1.0")
}
