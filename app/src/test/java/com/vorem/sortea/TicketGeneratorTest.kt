package com.vorem.sortea

import com.vorem.sortea.data.MAX_TICKETS
import com.vorem.sortea.data.SorteaConfig
import com.vorem.sortea.data.TicketGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TicketGeneratorTest {
    private fun config(quantity: Int) = SorteaConfig(
        quantity = quantity,
        prizes = List(10) { "Suerte ${it + 1}" },
        drawDate = LocalDate.of(2026, 8, 31)
    )

    @Test fun generatesOneTicket() {
        val tickets = TicketGenerator.generate(config(1))
        assertEquals(1, tickets.size)
        assertInRangeAndUnique(tickets)
    }

    @Test fun generatesMultipleTicketsWithoutRepeats() {
        val tickets = TicketGenerator.generate(config(50))
        assertEquals(50, tickets.size)
        assertInRangeAndUnique(tickets)
    }

    @Test fun generatesMaximum499TicketsWithoutRepeats() {
        val tickets = TicketGenerator.generate(config(MAX_TICKETS))
        assertEquals(MAX_TICKETS, tickets.size)
        assertInRangeAndUnique(tickets)
        assertEquals(998, tickets.flatMap { listOf(it.number1, it.number2) }.distinct().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMoreThanMaximum() {
        TicketGenerator.generate(config(MAX_TICKETS + 1))
    }

    @Test fun eachTicketHasDifferentNumbers() {
        val tickets = TicketGenerator.generate(config(100))
        tickets.forEach { assertNotEquals(it.number1, it.number2) }
    }

    private fun assertInRangeAndUnique(tickets: List<com.vorem.sortea.data.SorteaTicket>) {
        val numbers = tickets.flatMap { listOf(it.number1, it.number2) }
        assertTrue(numbers.all { it in 1..999 })
        assertEquals(numbers.size, numbers.distinct().size)
    }
}

class PdfLayoutTest {
    @Test fun defaultSizeFitsEightBySix() {
        val grid = com.vorem.sortea.data.PdfLayout.grid(2.5, 3.8, 1.0)
        assertEquals(8, grid.columns)
        assertEquals(6, grid.rows)
        assertEquals(48, grid.perPage)
    }

    @Test fun maximum499ProducesElevenPdfFilesAtDefaultSize() {
        val grid = com.vorem.sortea.data.PdfLayout.grid(2.5, 3.8, 1.0)
        assertEquals(11, kotlin.math.ceil(499.0 / grid.perPage).toInt())
        assertEquals(19, 499 % grid.perPage)
    }
}
