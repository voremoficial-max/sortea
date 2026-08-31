package com.vorem.sortea.data

import kotlin.random.Random

object TicketGenerator {
    /**
     * Generates unique pairs from 001..999. Each number is consumed at most once.
     * The same generated ticket list is deterministic after creation and can be
     * persisted by a later phase alongside the PDF.
     */
    fun generate(config: SorteaConfig, random: Random = Random.Default): List<SorteaTicket> {
        require(config.quantity in 1..MAX_TICKETS) {
            "La cantidad debe estar entre 1 y $MAX_TICKETS."
        }
        require(config.prizes.size == 10) { "Deben existir exactamente 10 suertes." }

        val pool = (MIN_NUMBER..MAX_NUMBER).toMutableList()
        pool.shuffle(random)

        return List(config.quantity) { index ->
            val offset = index * 2
            SorteaTicket(
                sequence = index + 1,
                number1 = pool[offset],
                number2 = pool[offset + 1],
                drawDate = config.drawDate,
                prizes = config.prizes.toList()
            )
        }
    }
}
