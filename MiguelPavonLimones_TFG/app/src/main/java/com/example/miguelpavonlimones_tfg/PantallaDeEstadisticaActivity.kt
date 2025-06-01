package com.example.miguelpavonlimones_tfg

import android.app.AlertDialog
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PantallaDeEstadisticaActivity : AppCompatActivity() {

    private lateinit var tvPuntosEquipo: TextView
    private lateinit var tvEquipo: TextView
    private lateinit var tvRival: TextView
    private lateinit var tvJornada: TextView
    private lateinit var tvFecha: TextView

    private lateinit var btnEmpezar: Button
    private lateinit var btnAcabar: Button
    private lateinit var btnExportarPDF: Button

    private var puntosEquipo = 0
    private var tiros2Intentados = 0
    private var tiros2Anotados = 0
    private var tiros3Intentados = 0
    private var tiros3Anotados = 0
    private var tirosLibresIntentados = 0
    private var tirosLibresAnotados = 0
    private var rebotes = 0
    private var reboteOfensivo = 0
    private var reboteDefensivo = 0
    private var asistencias = 0
    private var robos = 0
    private var tapones = 0
    private var faltas = 0
    private var perdidas = 0

    private var partidoId = ""
    private var nombreEquipo = ""

    private lateinit var estadisticaButtons: List<Button>

    private val database = FirebaseDatabase.getInstance(
        "https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_de_estadistica)

        tvPuntosEquipo = findViewById(R.id.tvPuntosEquipo)
        tvEquipo = findViewById(R.id.tvEquipo)
        tvRival = findViewById(R.id.tvRival)
        tvJornada = findViewById(R.id.tvJornada)
        tvFecha = findViewById(R.id.tvFecha)

        btnEmpezar = findViewById(R.id.btnEmpezarPartido)
        btnAcabar = findViewById(R.id.btnAcabarPartido)
        btnExportarPDF = findViewById(R.id.btnExportarPDF)

        val btnTiroMetido: Button = findViewById(R.id.btnTiroMetido)
        val btnTiroFallado: Button = findViewById(R.id.btnTiroFallado)
        val btnRebotes: Button = findViewById(R.id.btnRebotes)
        val btnAsistencias: Button = findViewById(R.id.btnAsistencias)
        val btnRobos: Button = findViewById(R.id.btnRobos)
        val btnTapones: Button = findViewById(R.id.btnTapones)
        val btnFaltas: Button = findViewById(R.id.btnFaltas)
        val btnPerdidas: Button = findViewById(R.id.btnPerdidas)

        estadisticaButtons = listOf(
            btnTiroMetido, btnTiroFallado, btnRebotes, btnAsistencias,
            btnRobos, btnTapones, btnFaltas, btnPerdidas
        )

        partidoId = intent.getStringExtra("partidoId") ?: ""
        nombreEquipo = intent.getStringExtra("nombreEquipo") ?: ""
        val rival = intent.getStringExtra("rival") ?: "Rival"
        val jornada = intent.getStringExtra("jornada") ?: ""
        val fecha = intent.getStringExtra("fecha") ?: "fecha"

        tvEquipo.text = "Equipo: $nombreEquipo"
        tvRival.text = "Rival: $rival"
        tvJornada.text = "Jornada: $jornada"
        tvFecha.text = "Fecha: $fecha"

        btnTiroMetido.setOnClickListener {
            val opciones = arrayOf("Tiro libre (1p)", "Tiro de 2 (2p)", "Triple (3p)")
            AlertDialog.Builder(this)
                .setTitle("Tipo de tiro metido")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> { puntosEquipo += 1; tirosLibresAnotados++; tirosLibresIntentados++ }
                        1 -> { puntosEquipo += 2; tiros2Anotados++; tiros2Intentados++ }
                        2 -> { puntosEquipo += 3; tiros3Anotados++; tiros3Intentados++ }
                    }
                    tvPuntosEquipo.text = "Puntos: $puntosEquipo"
                }.show()
        }

        btnTiroFallado.setOnClickListener {
            val opciones = arrayOf("Tiro libre fallado", "Tiro de 2 fallado", "Triple fallado")
            AlertDialog.Builder(this)
                .setTitle("Tipo de tiro fallado")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> tirosLibresIntentados++
                        1 -> tiros2Intentados++
                        2 -> tiros3Intentados++
                    }
                }.show()
        }

        btnRebotes.setOnClickListener {
            val opciones = arrayOf("Ofensivo", "Defensivo")
            AlertDialog.Builder(this)
                .setTitle("Tipo de rebote")
                .setItems(opciones) { _, which ->
                    rebotes++
                    if (which == 0) reboteOfensivo++ else reboteDefensivo++
                }.show()
        }

        btnAsistencias.setOnClickListener { asistencias++ }
        btnRobos.setOnClickListener { robos++ }
        btnTapones.setOnClickListener { tapones++ }
        btnFaltas.setOnClickListener { faltas++ }
        btnPerdidas.setOnClickListener { perdidas++ }

        btnExportarPDF.setOnClickListener { exportarPDF(rival, fecha) }

        estadisticaButtons.forEach { it.isEnabled = false }
        btnAcabar.visibility = View.GONE

        btnEmpezar.setOnClickListener {
            estadisticaButtons.forEach { it.isEnabled = true }
            btnEmpezar.visibility = View.GONE
            btnAcabar.visibility = View.VISIBLE
        }

        btnAcabar.setOnClickListener {
            estadisticaButtons.forEach { it.isEnabled = false }
            btnAcabar.visibility = View.GONE
        }
    }

    private fun exportarPDF(rival: String, fecha: String) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.textSize = 16f

        var y = 50
        canvas.drawText("Estadísticas del partido", 220f, y.toFloat(), paint)
        y += 30

        canvas.drawText("Puntos: $puntosEquipo", 50f, y.toFloat(), paint); y += 25
        canvas.drawText("Tiros de 2: $tiros2Anotados/$tiros2Intentados", 50f, y.toFloat(), paint); y += 25
        canvas.drawText("Tiros de 3: $tiros3Anotados/$tiros3Intentados", 50f, y.toFloat(), paint); y += 25
        canvas.drawText("Tiros libres: $tirosLibresAnotados/$tirosLibresIntentados", 50f, y.toFloat(), paint); y += 25

        val totalCampoIntentos = tiros2Intentados + tiros3Intentados
        val totalCampoAciertos = tiros2Anotados + tiros3Anotados
        canvas.drawText("Tiros de campo: $totalCampoAciertos/$totalCampoIntentos", 50f, y.toFloat(), paint); y += 25

        fun porcentaje(aciertos: Int, intentos: Int): String = if (intentos == 0) "0%" else "${(100 * aciertos / intentos)}%"

        canvas.drawText("% Tiro 2: ${porcentaje(tiros2Anotados, tiros2Intentados)}", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("% Triple: ${porcentaje(tiros3Anotados, tiros3Intentados)}", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("% Tiro libre: ${porcentaje(tirosLibresAnotados, tirosLibresIntentados)}", 50f, y.toFloat(), paint); y += 30

        canvas.drawText("Rebotes: $rebotes (Of: $reboteOfensivo / Def: $reboteDefensivo)", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("Asistencias: $asistencias", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("Robos: $robos", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("Tapones: $tapones", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("Faltas: $faltas", 50f, y.toFloat(), paint); y += 20
        canvas.drawText("Pérdidas: $perdidas", 50f, y.toFloat(), paint)

        pdfDocument.finishPage(page)

        val EquipoUsuario = nombreEquipo.replace(" ", "_")
        val EquipoRival = rival.replace(" ", "_")
        val FechaPartido = fecha.replace("/", "-")
        val fileName = "${EquipoUsuario}_vs_${EquipoRival}_${FechaPartido}.pdf"

        val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsPath, fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }

        pdfDocument.close()
    }
}
