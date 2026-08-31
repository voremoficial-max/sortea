package com.vorem.sortea.data

import java.time.LocalDate

const val MIN_NUMBER = 1
const val MAX_NUMBER = 999
const val MAX_TICKETS = 499

/** Cantidad usada cuando el usuario deja el campo de cantidad vacio. */
const val DEFAULT_TICKETS = 499

/** Configuration entered by the user in Phase 1. */
data class SorteaConfig(
    val quantity: Int = 1,
    val prizes: List<String> = List(10) { "" },
    val drawDate: LocalDate = LocalDate.now(),
    val widthCm: Double = 2.5,
    val heightCm: Double = 3.8,
    val spacingMm: Double = 1.0
)

data class SorteaTicket(
    val sequence: Int,
    val number1: Int,
    val number2: Int,
    val drawDate: LocalDate,
    val prizes: List<String>
) {
    init {
        require(number1 in MIN_NUMBER..MAX_NUMBER)
        require(number2 in MIN_NUMBER..MAX_NUMBER)
        require(number1 != number2)
        require(prizes.size == 10)
    }

    fun number1Formatted(): String = "%03d".format(number1)
    fun number2Formatted(): String = "%03d".format(number2)
}
