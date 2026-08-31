package com.vorem.sortea.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.graphics.pdf.PdfDocument
import com.vorem.sortea.R
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.min

object PdfLayout {
    private const val PAGE_WIDTH_PT = 612f
    private const val PAGE_HEIGHT_PT = 792f
    private const val CM_TO_PT = 72f / 2.54f
    private const val MM_TO_PT = 72f / 25.4f
    private const val MAX_TICKETS_PER_PAGE = 48
    data class Grid(val columns: Int, val rows: Int, val perPage: Int)

    fun grid(widthCm: Double, heightCm: Double, spacingMm: Double): Grid {
        require(widthCm > 0 && heightCm > 0 && spacingMm >= 0)
        val width = (widthCm * CM_TO_PT).toFloat()
        val height = (heightCm * CM_TO_PT).toFloat()
        val spacing = (spacingMm * MM_TO_PT).toFloat()
        val columns = floor((PAGE_WIDTH_PT + spacing) / (width + spacing)).toInt().coerceAtLeast(1)
        val rows = floor((PAGE_HEIGHT_PT + spacing) / (height + spacing)).toInt().coerceAtLeast(1).coerceAtMost(6)
        return Grid(columns, rows, min(MAX_TICKETS_PER_PAGE, columns * rows))
    }
}

/**
 * Carpeta de salida de los PDF: una carpeta propia "Sortea" en la raiz del
 * almacenamiento (no dentro de Descargas). Requiere acceso a todos los
 * archivos en Android 11+ (ver [SorteaStorage.hasFullAccess]) o el permiso
 * clasico de almacenamiento en versiones anteriores.
 */
object SorteaStorage {
    private const val FOLDER_NAME = "Sortea"

    fun outputDir(): File =
        File(Environment.getExternalStorageDirectory(), FOLDER_NAME).apply { mkdirs() }

    fun hasFullAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

    /** Elimina los PDF generados en tandas anteriores antes de crear los nuevos. */
    fun clearPreviousPdfs() {
        outputDir().listFiles { file -> file.isFile && file.extension.equals("pdf", ignoreCase = true) }
            ?.forEach { it.delete() }
    }

    fun scanForFileManagers(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        MediaScannerConnection.scanFile(context, files.map { it.absolutePath }.toTypedArray(), null, null)
    }
}

/**
 * PDF renderer that uses the user's supplied GANADIARIO artwork as the actual
 * ticket template. We do not redraw the artwork; only the variable fields are
 * placed over the existing blank areas.
 */
object PdfGenerator {
    private const val PAGE_WIDTH_PT = 612f
    private const val PAGE_HEIGHT_PT = 792f

    // Coordinates are percentages of the supplied template.
    private val NUMBER_1 = RectF(0.16f, 0.215f, 0.465f, 0.274f)
    private val NUMBER_2 = RectF(0.515f, 0.215f, 0.95f, 0.274f)
    private val PRIZE_BOXES = listOf(
        RectF(0.022f, 0.366f, 0.202f, 0.493f), RectF(0.218f, 0.366f, 0.397f, 0.493f),
        RectF(0.414f, 0.366f, 0.593f, 0.493f), RectF(0.610f, 0.366f, 0.789f, 0.493f),
        RectF(0.806f, 0.366f, 0.985f, 0.493f), RectF(0.022f, 0.581f, 0.202f, 0.708f),
        RectF(0.218f, 0.581f, 0.397f, 0.708f), RectF(0.414f, 0.581f, 0.593f, 0.708f),
        RectF(0.610f, 0.581f, 0.789f, 0.708f), RectF(0.806f, 0.581f, 0.985f, 0.708f)
    )
    private val DATE_BOX = RectF(0.48f, 0.758f, 0.93f, 0.825f)

    // Separacion extra (en unidades "em") solo para la fecha, para que los
    // caracteres no se vean pegados entre si.
    private const val DATE_LETTER_SPACING = 0.10f

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "CO"))

    // Paints reutilizados en vez de crear uno nuevo por cada campo de cada
    // boleta: con 499 boletas x 12 campos evita miles de asignaciones.
    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    data class Result(val files: List<GeneratedPdf>)
    data class GeneratedPdf(val name: String, val uri: Uri?)

    fun generate(context: Context, config: SorteaConfig, tickets: List<SorteaTicket>): Result {
        require(tickets.isNotEmpty()) { "No hay boletas para generar." }
        val grid = PdfLayout.grid(config.widthCm, config.heightCm, config.spacingMm)
        val ticketWidth = (config.widthCm * 72f / 2.54f).toFloat()
        val ticketHeight = (config.heightCm * 72f / 2.54f).toFloat()
        val spacing = (config.spacingMm * 72f / 25.4f).toFloat()
        val template = BitmapFactory.decodeResource(context.resources, R.drawable.boleta_template)
            ?: error("No se pudo cargar la plantilla de la boleta.")

        // Cada tanda nueva reemplaza a la anterior: se limpian los PDF viejos primero.
        SorteaStorage.clearPreviousPdfs()
        val outputDir = SorteaStorage.outputDir()
        val files = mutableListOf<GeneratedPdf>()
        val savedFiles = mutableListOf<File>()

        try {
            tickets.chunked(grid.perPage).forEachIndexed { index, pageTickets ->
                val name = "Sortea_${(index + 1).toString().padStart(3, '0')}.pdf"
                val document = PdfDocument()
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT.toInt(), PAGE_HEIGHT_PT.toInt(), 1).create()
                )
                drawPage(page.canvas, pageTickets, ticketWidth, ticketHeight, spacing, grid.columns, grid.rows, template)
                document.finishPage(page)
                val file = File(outputDir, name)
                file.outputStream().use { document.writeTo(it) }
                document.close()
                savedFiles += file
                files += GeneratedPdf(name, Uri.fromFile(file))
            }
        } finally {
            template.recycle()
        }
        SorteaStorage.scanForFileManagers(context, savedFiles)
        return Result(files)
    }

    private fun drawPage(
        canvas: Canvas,
        tickets: List<SorteaTicket>,
        ticketWidth: Float,
        ticketHeight: Float,
        spacing: Float,
        columns: Int,
        rows: Int,
        template: android.graphics.Bitmap
    ) {
        canvas.drawColor(android.graphics.Color.WHITE)
        val totalWidth = columns * ticketWidth + (columns - 1) * spacing
        val totalHeight = rows * ticketHeight + (rows - 1) * spacing
        val startX = (PAGE_WIDTH_PT - totalWidth) / 2f
        val startY = (PAGE_HEIGHT_PT - totalHeight) / 2f
        tickets.forEachIndexed { index, ticket ->
            val col = index % columns
            val row = index / columns
            val left = startX + col * (ticketWidth + spacing)
            val top = startY + row * (ticketHeight + spacing)
            drawTicket(canvas, RectF(left, top, left + ticketWidth, top + ticketHeight), ticket, template)
        }
    }

    private fun drawTicket(canvas: Canvas, rect: RectF, ticket: SorteaTicket, template: android.graphics.Bitmap) {
        canvas.drawBitmap(template, null, rect, bitmapPaint)

        drawCenteredText(canvas, rectFor(rect, NUMBER_1), ticket.number1Formatted(), 0xFF111111.toInt(), maxFraction = 0.62f, minSize = 3.0f, startFraction = 0.80f)
        drawCenteredText(canvas, rectFor(rect, NUMBER_2), ticket.number2Formatted(), 0xFF111111.toInt(), maxFraction = 0.62f, minSize = 3.0f, startFraction = 0.80f)

        ticket.prizes.take(10).forEachIndexed { index, prize ->
            val text = prize.ifBlank { "TU SUERTE" }
            drawCenteredText(canvas, rectFor(rect, PRIZE_BOXES[index]), text, 0xFF151515.toInt(), maxFraction = 0.80f, minSize = 1.8f)
        }

        val dateText = ticket.drawDate.format(DATE_FORMATTER)
        drawCenteredText(canvas, rectFor(rect, DATE_BOX), dateText, 0xFF111111.toInt(), maxFraction = 0.82f, minSize = 1.7f, letterSpacing = DATE_LETTER_SPACING)
    }

    private fun rectFor(ticket: RectF, normalized: RectF): RectF = RectF(
        ticket.left + ticket.width() * normalized.left,
        ticket.top + ticket.height() * normalized.top,
        ticket.left + ticket.width() * normalized.right,
        ticket.top + ticket.height() * normalized.bottom
    )

    private fun drawCenteredText(
        canvas: Canvas,
        box: RectF,
        text: String,
        color: Int,
        maxFraction: Float,
        minSize: Float,
        startFraction: Float = 0.62f,
        letterSpacing: Float = 0f
    ) {
        val paint = boldPaint
        paint.color = color
        paint.letterSpacing = letterSpacing
        var size = box.height() * startFraction
        while (size > minSize) {
            paint.textSize = size
            if (paint.measureText(text) <= box.width() * maxFraction) break
            size -= 0.15f
        }
        paint.textSize = maxOf(size, minSize)
        val baseline = box.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.save()
        canvas.clipRect(box)
        canvas.drawText(text, box.centerX(), baseline, paint)
        canvas.restore()
        paint.letterSpacing = 0f
    }
}
