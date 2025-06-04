package com.example.miguelpavonlimones_tfg

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class BarChartCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var datos: Map<String, Int> = emptyMap()
    private var mostrarDetalles = false
    private var detalles: Map<String, String>? = null
    private var esPorcentaje = false

    private val paintBarra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
    }
    private val paintLinea = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
    }

    fun setDatos(
        nuevosDatos: Map<String, Int>,
        esPorcentaje: Boolean,
        detalles: Map<String, String>? = null
    ) {
        this.datos = nuevosDatos
        this.esPorcentaje = esPorcentaje
        this.mostrarDetalles = detalles != null
        this.detalles = detalles
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (datos.isEmpty()) return

        val margen = 60f
        val altoGrafica = height - 140f
        val anchoTotal = width - 2 * margen
        val espacio = 40f
        val numBarras = datos.size
        val anchoBarra = (anchoTotal - espacio * (numBarras - 1)) / numBarras
        val maxValor = if (esPorcentaje) 100 else 50

        // Líneas horizontales (escala)
        for (i in 0..maxValor step 10) {
            val y = altoGrafica - (i / maxValor.toFloat()) * altoGrafica + margen
            canvas.drawLine(margen, y, width - margen, y, paintLinea)
            canvas.drawText(i.toString(), 10f, y + 10f, paintTexto)
        }

        // Colores por nombre
        val colores = mapOf(
            "Triple" to ContextCompat.getColor(context, R.color.colorTriple),
            "Tiro de 2" to ContextCompat.getColor(context, R.color.colorTiro2),
            "Tiro Libre" to ContextCompat.getColor(context, R.color.colorTiroLibre),
            "Rebotes Of" to ContextCompat.getColor(context, R.color.colorReboteOf),
            "Rebotes Def" to ContextCompat.getColor(context, R.color.colorReboteDef),
            "Rebotes Tot" to ContextCompat.getColor(context, R.color.colorReboteTot),
            "Asistencias" to ContextCompat.getColor(context, R.color.colorAsist),
            "Robos" to ContextCompat.getColor(context, R.color.colorRobos),
            "Tapones" to ContextCompat.getColor(context, R.color.colorTapones),
            "Faltas" to ContextCompat.getColor(context, R.color.colorFaltas),
            "Pérdidas" to ContextCompat.getColor(context, R.color.colorPerdidas)
        )

        datos.entries.forEachIndexed { index, (label, valor) ->
            val left = margen + index * (anchoBarra + espacio)
            val top = altoGrafica - (valor / maxValor.toFloat()) * altoGrafica + margen
            val right = left + anchoBarra
            val bottom = altoGrafica + margen

            paintBarra.color = colores[label] ?: Color.DKGRAY
            canvas.drawRect(left, top, right, bottom, paintBarra)

            // Etiqueta arriba (valor o aciertos/intentos)
            val texto = if (mostrarDetalles) detalles?.get(label) ?: "" else valor.toString()
            canvas.drawText(texto, left + 5f, top - 10f, paintTexto)

            // Etiqueta abajo (nombre)
            canvas.drawText(label, left, bottom + 35f, paintTexto)
        }
    }
}
