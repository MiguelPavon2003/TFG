package com.example.miguelpavonlimones_tfg

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PantallaDeEstadisticaActivity : AppCompatActivity() {

    private lateinit var tvPuntosEquipo: TextView
    private lateinit var tvEquipo: TextView
    private lateinit var tvRival: TextView
    private lateinit var tvJornada: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvTipo: TextView

    private lateinit var btnEmpezar: Button
    private lateinit var btnAcabar: Button
    private lateinit var btnPausa: Button
    private lateinit var btnExportarPDF: Button
    private lateinit var btnVerGrafica: Button
    private lateinit var btnSeleccionarJugadores: Button

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
    private var partidoFinalizado = false

    // Estado del partido
    private var partidoEmpezado = false
    private var estadisticasPausadas = false

    private lateinit var estadisticaButtons: List<Button>

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(
        "https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/"
    )

    private lateinit var refEstadisticas: DatabaseReference
    private lateinit var refFinalizado: DatabaseReference
    private lateinit var refConvocados: DatabaseReference
    private lateinit var refStatsJugador: DatabaseReference

    // Convocados
    data class Convocado(
        val key: String,
        val nombre: String,
        val apellido: String,
        val dorsal: Int
    )

    private val convocados = mutableListOf<Convocado>()
    private var convocatoriaBloqueada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_de_estadistica)

        tvPuntosEquipo = findViewById(R.id.tvPuntosEquipo)
        tvEquipo = findViewById(R.id.tvEquipo)
        tvRival = findViewById(R.id.tvRival)
        tvJornada = findViewById(R.id.tvJornada)
        tvFecha = findViewById(R.id.tvFecha)
        tvTipo = findViewById(R.id.tvTipo)

        btnEmpezar = findViewById(R.id.btnEmpezarPartido)
        btnAcabar = findViewById(R.id.btnAcabarPartido)
        btnPausa = findViewById(R.id.btnPausaPartido)
        btnExportarPDF = findViewById(R.id.btnExportarPDF)
        btnVerGrafica = findViewById(R.id.btnVerGraficas)
        btnSeleccionarJugadores = findViewById(R.id.btnSeleccionarJugadores)

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
        val tipoPartido = intent.getStringExtra("tipo") ?: "tipo"

        tvEquipo.text = "Equipo: $nombreEquipo"
        tvRival.text = "Rival: $rival"
        tvFecha.text = "Fecha: $fecha"
        tvTipo.text = "Tipo: $tipoPartido"

        if (tipoPartido.equals("Liga", ignoreCase = true) && jornada.isNotBlank()) {
            tvJornada.text = "Jornada: $jornada"
            tvJornada.visibility = View.VISIBLE
        } else {
            tvJornada.visibility = View.GONE
        }

        refEstadisticas = database.getReference("estadisticas").child(partidoId).child(nombreEquipo)
        refFinalizado = database.getReference("partidosFinalizados").child(partidoId)
        refConvocados = database.getReference("convocados").child(partidoId)
        refStatsJugador = database.getReference("estadisticasJugador").child(partidoId)

        // Abrir selector
        btnSeleccionarJugadores.setOnClickListener {
            val intent = Intent(this, SelectJugadoresActivity::class.java)
            intent.putExtra("partidoId", partidoId)
            intent.putExtra("nombreEquipo", nombreEquipo)
            startActivity(intent)
        }

        // ¿Finalizado?
        refFinalizado.get().addOnSuccessListener { snapshot ->
            partidoFinalizado = snapshot.getValue(Boolean::class.java) == true
            actualizarUI(partidoFinalizado)
        }

        // Cargar stats + estado
        refEstadisticas.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                puntosEquipo = snapshot.child("puntosEquipo").getValue(Int::class.java) ?: 0
                tiros2Intentados = snapshot.child("tiros2Intentados").getValue(Int::class.java) ?: 0
                tiros2Anotados = snapshot.child("tiros2Anotados").getValue(Int::class.java) ?: 0
                tiros3Intentados = snapshot.child("tiros3Intentados").getValue(Int::class.java) ?: 0
                tiros3Anotados = snapshot.child("tiros3Anotados").getValue(Int::class.java) ?: 0
                tirosLibresIntentados = snapshot.child("tirosLibresIntentados").getValue(Int::class.java) ?: 0
                tirosLibresAnotados = snapshot.child("tirosLibresAnotados").getValue(Int::class.java) ?: 0
                rebotes = snapshot.child("rebotes").getValue(Int::class.java) ?: 0
                reboteOfensivo = snapshot.child("reboteOfensivo").getValue(Int::class.java) ?: 0
                reboteDefensivo = snapshot.child("reboteDefensivo").getValue(Int::class.java) ?: 0
                asistencias = snapshot.child("asistencias").getValue(Int::class.java) ?: 0
                robos = snapshot.child("robos").getValue(Int::class.java) ?: 0
                tapones = snapshot.child("tapones").getValue(Int::class.java) ?: 0
                faltas = snapshot.child("faltas").getValue(Int::class.java) ?: 0
                perdidas = snapshot.child("perdidas").getValue(Int::class.java) ?: 0

                partidoEmpezado = snapshot.child("partidoEmpezado").getValue(Boolean::class.java) ?: false
                estadisticasPausadas = snapshot.child("estadisticasPausadas").getValue(Boolean::class.java) ?: false

                tvPuntosEquipo.text = "Puntos: $puntosEquipo"

                if (!partidoFinalizado) {
                    if (!partidoEmpezado) {
                        estadisticaButtons.forEach { it.isEnabled = false }
                        btnEmpezar.visibility = View.VISIBLE
                        btnAcabar.visibility = View.GONE
                        btnPausa.visibility = View.GONE
                    } else {
                        btnEmpezar.visibility = View.GONE
                        btnAcabar.visibility = View.VISIBLE
                        btnPausa.visibility = View.VISIBLE

                        if (estadisticasPausadas) {
                            estadisticaButtons.forEach { it.isEnabled = false }
                            btnPausa.text = "Reanudar"
                        } else {
                            estadisticaButtons.forEach { it.isEnabled = true }
                            btnPausa.text = "Pausar"
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        fun guardarEstadisticasEquipo() {
            val ref = refEstadisticas
            ref.child("puntosEquipo").setValue(puntosEquipo)
            ref.child("tiros2Intentados").setValue(tiros2Intentados)
            ref.child("tiros2Anotados").setValue(tiros2Anotados)
            ref.child("tiros3Intentados").setValue(tiros3Intentados)
            ref.child("tiros3Anotados").setValue(tiros3Anotados)
            ref.child("tirosLibresIntentados").setValue(tirosLibresIntentados)
            ref.child("tirosLibresAnotados").setValue(tirosLibresAnotados)
            ref.child("rebotes").setValue(rebotes)
            ref.child("reboteOfensivo").setValue(reboteOfensivo)
            ref.child("reboteDefensivo").setValue(reboteDefensivo)
            ref.child("asistencias").setValue(asistencias)
            ref.child("robos").setValue(robos)
            ref.child("tapones").setValue(tapones)
            ref.child("faltas").setValue(faltas)
            ref.child("perdidas").setValue(perdidas)
            ref.child("partidoEmpezado").setValue(partidoEmpezado)
            ref.child("estadisticasPausadas").setValue(estadisticasPausadas)
        }

        // -------------------------
        // CLICKs con selección jugador
        // -------------------------

        btnAsistencias.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién dio la asistencia?") { c ->
                asistencias++
                guardarEstadisticasEquipo()
                incJugador(c, "asistencias", 1)
            }
        }

        btnRobos.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién hizo el robo?") { c ->
                robos++
                guardarEstadisticasEquipo()
                incJugador(c, "robos", 1)
            }
        }

        btnTapones.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién hizo el tapón?") { c ->
                tapones++
                guardarEstadisticasEquipo()
                incJugador(c, "tapones", 1)
            }
        }

        btnFaltas.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién cometió la falta?") { c ->
                faltas++
                guardarEstadisticasEquipo()
                incJugador(c, "faltas", 1)
            }
        }

        btnPerdidas.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién perdió el balón?") { c ->
                perdidas++
                guardarEstadisticasEquipo()
                incJugador(c, "perdidas", 1)
            }
        }

        btnRebotes.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            val opciones = arrayOf("Ofensivo", "Defensivo")
            AlertDialog.Builder(this)
                .setTitle("Tipo de rebote")
                .setItems(opciones) { _, which ->
                    elegirConvocado("¿Quién cogió el rebote?") { c ->
                        rebotes++
                        if (which == 0) reboteOfensivo++ else reboteDefensivo++
                        guardarEstadisticasEquipo()

                        incJugador(c, "rebotes", 1)
                        if (which == 0) incJugador(c, "rebOf", 1) else incJugador(c, "rebDef", 1)
                    }
                }.show()
        }

        btnTiroMetido.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién anotó?") { c ->
                val opciones = arrayOf("Tiro libre (1p)", "Tiro de 2 (2p)", "Triple (3p)")
                AlertDialog.Builder(this)
                    .setTitle("Tipo de tiro metido")
                    .setItems(opciones) { _, which ->
                        when (which) {
                            0 -> {
                                puntosEquipo += 1
                                tirosLibresAnotados++; tirosLibresIntentados++
                                guardarEstadisticasEquipo()

                                incJugador(c, "puntos", 1)
                                incJugador(c, "tlAno", 1)
                                incJugador(c, "tlInt", 1)
                            }
                            1 -> {
                                puntosEquipo += 2
                                tiros2Anotados++; tiros2Intentados++
                                guardarEstadisticasEquipo()

                                incJugador(c, "puntos", 2)
                                incJugador(c, "t2Ano", 1)
                                incJugador(c, "t2Int", 1)
                            }
                            2 -> {
                                puntosEquipo += 3
                                tiros3Anotados++; tiros3Intentados++
                                guardarEstadisticasEquipo()

                                incJugador(c, "puntos", 3)
                                incJugador(c, "t3Ano", 1)
                                incJugador(c, "t3Int", 1)
                            }
                        }
                        tvPuntosEquipo.text = "Puntos: $puntosEquipo"
                    }.show()
            }
        }

        btnTiroFallado.setOnClickListener {
            if (estadisticasPausadas) return@setOnClickListener
            elegirConvocado("¿Quién tiró?") { c ->
                val opciones = arrayOf("Tiro libre fallado", "Tiro de 2 fallado", "Triple fallado")
                AlertDialog.Builder(this)
                    .setTitle("Tipo de tiro fallado")
                    .setItems(opciones) { _, which ->
                        when (which) {
                            0 -> {
                                tirosLibresIntentados++
                                guardarEstadisticasEquipo()
                                incJugador(c, "tlInt", 1)
                            }
                            1 -> {
                                tiros2Intentados++
                                guardarEstadisticasEquipo()
                                incJugador(c, "t2Int", 1)
                            }
                            2 -> {
                                tiros3Intentados++
                                guardarEstadisticasEquipo()
                                incJugador(c, "t3Int", 1)
                            }
                        }
                    }.show()
            }
        }

        // Exportar PDF (ahora con tabla de jugadores)
        btnExportarPDF.setOnClickListener { exportarPDF(rival, fecha) }

        btnVerGrafica.setOnClickListener {
            val intent = Intent(this, VerGraficas::class.java)
            intent.putExtra("partidoId", partidoId)
            intent.putExtra("nombreEquipo", nombreEquipo)
            startActivity(intent)
        }

        btnEmpezar.setOnClickListener {
            partidoEmpezado = true
            estadisticasPausadas = false

            estadisticaButtons.forEach { it.isEnabled = true }
            btnEmpezar.visibility = View.GONE
            btnAcabar.visibility = View.VISIBLE

            btnPausa.visibility = View.VISIBLE
            btnPausa.text = "Pausar"

            refEstadisticas.child("partidoEmpezado").setValue(true)
            refEstadisticas.child("estadisticasPausadas").setValue(false)
        }

        btnPausa.setOnClickListener {
            if (!partidoEmpezado) return@setOnClickListener

            estadisticasPausadas = !estadisticasPausadas

            if (estadisticasPausadas) {
                estadisticaButtons.forEach { it.isEnabled = false }
                btnPausa.text = "Reanudar"
                Toast.makeText(this, "Estadísticas en pausa", Toast.LENGTH_SHORT).show()
            } else {
                estadisticaButtons.forEach { it.isEnabled = true }
                btnPausa.text = "Pausar"
                Toast.makeText(this, "Estadísticas reanudadas", Toast.LENGTH_SHORT).show()
            }

            refEstadisticas.child("estadisticasPausadas").setValue(estadisticasPausadas)
        }

        btnAcabar.setOnClickListener {
            estadisticaButtons.forEach { it.isEnabled = false }
            btnAcabar.visibility = View.GONE
            btnPausa.visibility = View.GONE
            database.getReference("partidosFinalizados").child(partidoId).setValue(true)
            Toast.makeText(this, "Partido finalizado", Toast.LENGTH_SHORT).show()
        }

        estadisticaButtons.forEach { it.isEnabled = false }
        btnAcabar.visibility = View.GONE
        btnPausa.visibility = View.GONE

        // Cargar convocatoria para elegir jugadores y actualizar texto botón
        cargarConvocadosYActualizarBoton()
    }

    override fun onResume() {
        super.onResume()
        cargarConvocadosYActualizarBoton()
    }

    private fun actualizarUI(finalizado: Boolean) {
        if (finalizado) {
            estadisticaButtons.forEach { it.isEnabled = false }
            btnEmpezar.visibility = View.GONE
            btnAcabar.visibility = View.GONE
            btnPausa.visibility = View.GONE
            Toast.makeText(this, "Este partido ya ha sido finalizado", Toast.LENGTH_LONG).show()
        }
    }

    // -------------------------
    // CONVOCADOS + BOTÓN
    // -------------------------

    private fun cargarConvocadosYActualizarBoton() {
        val uid = auth.currentUser?.uid ?: return

        refConvocados.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(convSnap: DataSnapshot) {

                convocatoriaBloqueada = convSnap.child("_locked").getValue(Boolean::class.java) == true

                var count = 0
                for (child in convSnap.children) {
                    val key = child.key ?: continue
                    if (key == "_equipo" || key == "_locked") continue
                    val v = child.value
                    if (v is Boolean && v) count++
                }

                btnSeleccionarJugadores.text = "Selec. Juga."

                if (!convocatoriaBloqueada) {
                    convocados.clear()
                    return
                }

                val keys = convSnap.children
                    .filter { it.key != "_locked" && it.key != "_equipo" }
                    .filter { it.value is Boolean && it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                    .toList()

                if (keys.isEmpty()) {
                    convocados.clear()
                    return
                }

                val refJugEquipo = database.getReference("jugadores").child(uid).child(nombreEquipo)
                refJugEquipo.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(jugSnap: DataSnapshot) {
                        convocados.clear()

                        for (k in keys) {
                            val nodo = jugSnap.child(k)
                            if (!nodo.exists()) continue

                            val nombre = nodo.child("nombre").getValue(String::class.java) ?: ""
                            val apellido = nodo.child("apellido").getValue(String::class.java) ?: ""
                            val dorsalInt = nodo.child("dorsal").getValue(Int::class.java)
                            val dorsalLong = nodo.child("dorsal").getValue(Long::class.java)
                            val dorsal = dorsalInt ?: dorsalLong?.toInt() ?: 0

                            val c = Convocado(k, nombre, apellido, dorsal)
                            convocados.add(c)
                            asegurarJugadorStatsBase(c)
                        }

                        convocados.sortBy { it.dorsal }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun elegirConvocado(titulo: String, onPick: (Convocado) -> Unit) {
        if (!convocatoriaBloqueada || convocados.isEmpty()) {
            Toast.makeText(this, "Primero guarda la convocatoria", Toast.LENGTH_SHORT).show()
            return
        }

        val items = convocados.map { "#${it.dorsal} ${it.nombre} ${it.apellido}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setItems(items) { _, which -> onPick(convocados[which]) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun asegurarJugadorStatsBase(c: Convocado) {
        val base = mapOf(
            "nombre" to c.nombre,
            "apellido" to c.apellido,
            "dorsal" to c.dorsal
        )
        refStatsJugador.child(c.key).updateChildren(base)
    }

    private fun incJugador(c: Convocado, campo: String, cantidad: Long) {
        asegurarJugadorStatsBase(c)
        refStatsJugador.child(c.key).child(campo).setValue(ServerValue.increment(cantidad))
    }

    // -------------------------
    // PDF con tabla
    // -------------------------

    private data class JugStats(
        val dorsal: Int,
        val nombre: String,
        val apellido: String,
        val puntos: Int,
        val ast: Int,
        val reb: Int,
        val rob: Int,
        val tap: Int,
        val fal: Int,
        val per: Int,
        val t2: String,
        val t3: String,
        val tl: String
    )

    private fun exportarPDF(rival: String, fecha: String) {
        // Leer estadísticas por jugador primero
        refStatsJugador.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {

                val jugadoresStats = mutableListOf<JugStats>()

                for (child in snap.children) {
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    val apellido = child.child("apellido").getValue(String::class.java) ?: ""

                    val dorsalInt = child.child("dorsal").getValue(Int::class.java)
                    val dorsalLong = child.child("dorsal").getValue(Long::class.java)
                    val dorsal = dorsalInt ?: dorsalLong?.toInt() ?: 0

                    fun gi(k: String): Int {
                        val i = child.child(k).getValue(Int::class.java)
                        val l = child.child(k).getValue(Long::class.java)
                        return i ?: l?.toInt() ?: 0
                    }

                    val t2Int = gi("t2Int")
                    val t2Ano = gi("t2Ano")
                    val t3Int = gi("t3Int")
                    val t3Ano = gi("t3Ano")
                    val tlInt = gi("tlInt")
                    val tlAno = gi("tlAno")

                    jugadoresStats.add(
                        JugStats(
                            dorsal = dorsal,
                            nombre = nombre,
                            apellido = apellido,
                            puntos = gi("puntos"),
                            ast = gi("asistencias"),
                            reb = gi("rebotes"),
                            rob = gi("robos"),
                            tap = gi("tapones"),
                            fal = gi("faltas"),
                            per = gi("perdidas"),
                            t2 = "$t2Ano/$t2Int",
                            t3 = "$t3Ano/$t3Int",
                            tl = "$tlAno/$tlInt"
                        )
                    )
                }

                jugadoresStats.sortBy { it.dorsal }

                // Crear PDF
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val paintTitle = Paint().apply { textSize = 18f; isFakeBoldText = true }
                val paintText = Paint().apply { textSize = 12f }
                val paintHeader = Paint().apply { textSize = 12f; isFakeBoldText = true }
                val paintLine = Paint().apply { strokeWidth = 1f }

                var y = 40

                canvas.drawText("Estadísticas del partido", 180f, y.toFloat(), paintTitle)
                y += 22
                canvas.drawText("Equipo: $nombreEquipo   Rival: $rival   Fecha: $fecha", 40f, y.toFloat(), paintText)
                y += 22

                // Resumen equipo
                canvas.drawText("Puntos: $puntosEquipo", 40f, y.toFloat(), paintText); y += 16
                canvas.drawText("T2: $tiros2Anotados/$tiros2Intentados   T3: $tiros3Anotados/$tiros3Intentados   TL: $tirosLibresAnotados/$tirosLibresIntentados", 40f, y.toFloat(), paintText)
                y += 18
                canvas.drawText("Reb: $rebotes (Of $reboteOfensivo / Def $reboteDefensivo)  Ast: $asistencias  Rob: $robos  Tap: $tapones  Fal: $faltas  Per: $perdidas", 40f, y.toFloat(), paintText)
                y += 22

                // Título tabla
                canvas.drawText("Tabla de jugadores", 40f, y.toFloat(), paintHeader)
                y += 10

                // Tabla: columnas
                val startX = 40f
                val tableW = 515f
                val rowH = 18f

                val cols = listOf(
                    "D" to 30f,
                    "Jugador" to 160f,
                    "PTS" to 35f,
                    "AST" to 35f,
                    "REB" to 35f,
                    "ROB" to 35f,
                    "TAP" to 35f,
                    "FAL" to 35f,
                    "PER" to 35f,
                    "T2" to 45f,
                    "T3" to 45f,
                    "TL" to 45f
                )

                fun drawRowBorder(top: Float) {
                    canvas.drawLine(startX, top, startX + tableW, top, paintLine)
                }

                // Borde superior
                drawRowBorder(y.toFloat())

                // Cabecera
                y += rowH.toInt()
                var x = startX
                for (c in cols) {
                    canvas.drawText(c.first, x + 2f, y - 4f, paintHeader)
                    x += c.second
                }

                // Borde debajo cabecera
                drawRowBorder(y.toFloat())

                // Filas
                for (js in jugadoresStats) {
                    // salto de página si hace falta
                    if (y + rowH + 30 > 842) break

                    y += rowH.toInt()
                    x = startX

                    val jugadorTxt = "${js.nombre} ${js.apellido}".take(18)

                    val values = listOf(
                        js.dorsal.toString(),
                        jugadorTxt,
                        js.puntos.toString(),
                        js.ast.toString(),
                        js.reb.toString(),
                        js.rob.toString(),
                        js.tap.toString(),
                        js.fal.toString(),
                        js.per.toString(),
                        js.t2,
                        js.t3,
                        js.tl
                    )

                    for (i in values.indices) {
                        canvas.drawText(values[i], x + 2f, y - 4f, paintText)
                        x += cols[i].second
                    }

                    drawRowBorder(y.toFloat())
                }

                // Cerrar página
                pdfDocument.finishPage(page)

                val EquipoUsuario = nombreEquipo.replace(" ", "_")
                val EquipoRival = rival.replace(" ", "_")
                val FechaPartido = fecha.replace("/", "-")
                val fileName = "${EquipoUsuario}_vs_${EquipoRival}_${FechaPartido}.pdf"

                val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsPath, fileName)

                try {
                    pdfDocument.writeTo(FileOutputStream(file))
                    Toast.makeText(this@PantallaDeEstadisticaActivity, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                } catch (e: IOException) {
                    Toast.makeText(this@PantallaDeEstadisticaActivity, "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }

                pdfDocument.close()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PantallaDeEstadisticaActivity, "Error leyendo stats jugador: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
