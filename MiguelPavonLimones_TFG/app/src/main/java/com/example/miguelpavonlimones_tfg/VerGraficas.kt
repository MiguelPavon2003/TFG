package com.example.miguelpavonlimones_tfg

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class VerGraficas : AppCompatActivity() {

    private lateinit var graficaTiro: BarChartCanvasView
    private lateinit var graficaOtras: BarChartCanvasView

    private lateinit var tableJugadores: TableLayout

    private lateinit var database: FirebaseDatabase
    private lateinit var refEstadisticas: DatabaseReference
    private lateinit var refStatsJugador: DatabaseReference

    private var partidoId = ""
    private var nombreEquipo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_graficas)

        graficaTiro = findViewById(R.id.graficaTiro)
        graficaOtras = findViewById(R.id.graficaOtras)
        tableJugadores = findViewById(R.id.tableJugadores)

        partidoId = intent.getStringExtra("partidoId") ?: ""
        nombreEquipo = intent.getStringExtra("nombreEquipo") ?: ""

        database = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")
        refEstadisticas = database.getReference("estadisticas").child(partidoId).child(nombreEquipo)

        // ✅ stats por jugador (las que guardas al elegir jugador en PantallaDeEstadistica)
        refStatsJugador = database.getReference("estadisticasJugador").child(partidoId)

        escucharCambiosEquipo()
        escucharCambiosTablaJugadores()
    }

    private fun escucharCambiosEquipo() {
        refEstadisticas.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val intentos2 = getInt(snapshot, "tiros2Intentados")
                val anotados2 = getInt(snapshot, "tiros2Anotados")
                val intentos3 = getInt(snapshot, "tiros3Intentados")
                val anotados3 = getInt(snapshot, "tiros3Anotados")
                val intentosTL = getInt(snapshot, "tirosLibresIntentados")
                val anotadosTL = getInt(snapshot, "tirosLibresAnotados")

                val datosTiro = mutableMapOf<String, Int>()
                datosTiro["Triple"] = calcularPorcentaje(anotados3, intentos3)
                datosTiro["Tiro de 2"] = calcularPorcentaje(anotados2, intentos2)
                datosTiro["Tiro Libre"] = calcularPorcentaje(anotadosTL, intentosTL)

                val detallesTiro = mutableMapOf<String, String>()
                detallesTiro["Triple"] = "$anotados3/$intentos3"
                detallesTiro["Tiro de 2"] = "$anotados2/$intentos2"
                detallesTiro["Tiro Libre"] = "$anotadosTL/$intentosTL"

                graficaTiro.setDatos(datosTiro, true, detallesTiro)

                val rebOf = getInt(snapshot, "reboteOfensivo")
                val rebDef = getInt(snapshot, "reboteDefensivo")
                val rebTot = getInt(snapshot, "rebotes")

                val otrasStats = mutableMapOf<String, Int>()
                otrasStats["Reb Of"] = rebOf
                otrasStats["Reb Def"] = rebDef
                otrasStats["Reb Tot"] = rebTot
                otrasStats["Asist"] = getInt(snapshot, "asistencias")
                otrasStats["Robos"] = getInt(snapshot, "robos")
                otrasStats["Tapones"] = getInt(snapshot, "tapones")
                otrasStats["Faltas"] = getInt(snapshot, "faltas")
                otrasStats["Pérdidas"] = getInt(snapshot, "perdidas")

                graficaOtras.setDatos(otrasStats, false)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ✅ Tabla tipo PDF debajo de gráficas
    private fun escucharCambiosTablaJugadores() {
        refStatsJugador.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Limpiar tabla
                tableJugadores.removeAllViews()

                // Header
                addHeaderRow()

                // Si no hay datos
                if (!snapshot.exists()) {
                    addEmptyRow("No hay estadísticas de jugadores todavía.")
                    return
                }

                val filas = mutableListOf<JugRow>()

                for (child in snapshot.children) {
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    val apellido = child.child("apellido").getValue(String::class.java) ?: ""

                    val dorsal = getInt(child, "dorsal")
                    val pts = getInt(child, "puntos")
                    val ast = getInt(child, "asistencias")
                    val reb = getInt(child, "rebotes")
                    val rob = getInt(child, "robos")
                    val tap = getInt(child, "tapones")
                    val fal = getInt(child, "faltas")
                    val per = getInt(child, "perdidas")

                    val t2Ano = getInt(child, "t2Ano")
                    val t2Int = getInt(child, "t2Int")
                    val t3Ano = getInt(child, "t3Ano")
                    val t3Int = getInt(child, "t3Int")
                    val tlAno = getInt(child, "tlAno")
                    val tlInt = getInt(child, "tlInt")

                    filas.add(
                        JugRow(
                            dorsal = dorsal,
                            jugador = "${nombre} ${apellido}".trim(),
                            pts = pts,
                            ast = ast,
                            reb = reb,
                            rob = rob,
                            tap = tap,
                            fal = fal,
                            per = per,
                            t2 = "$t2Ano/$t2Int",
                            t3 = "$t3Ano/$t3Int",
                            tl = "$tlAno/$tlInt"
                        )
                    )
                }

                filas.sortBy { it.dorsal }

                for (r in filas) {
                    addDataRow(r)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private data class JugRow(
        val dorsal: Int,
        val jugador: String,
        val pts: Int,
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

    // ---------------- UI Tabla ----------------

    private fun addHeaderRow() {
        val row = TableRow(this)
        row.addView(cell("D", 36, true))
        row.addView(cell("Jugador", 160, true))
        row.addView(cell("PTS", 44, true))
        row.addView(cell("AST", 44, true))
        row.addView(cell("REB", 44, true))
        row.addView(cell("ROB", 44, true))
        row.addView(cell("TAP", 44, true))
        row.addView(cell("FAL", 44, true))
        row.addView(cell("PER", 44, true))
        row.addView(cell("T2", 54, true))
        row.addView(cell("T3", 54, true))
        row.addView(cell("TL", 54, true))
        tableJugadores.addView(row)
    }

    private fun addDataRow(r: JugRow) {
        val row = TableRow(this)
        row.addView(cell(r.dorsal.toString(), 36, false))
        row.addView(cell(r.jugador.take(22), 160, false))
        row.addView(cell(r.pts.toString(), 44, false))
        row.addView(cell(r.ast.toString(), 44, false))
        row.addView(cell(r.reb.toString(), 44, false))
        row.addView(cell(r.rob.toString(), 44, false))
        row.addView(cell(r.tap.toString(), 44, false))
        row.addView(cell(r.fal.toString(), 44, false))
        row.addView(cell(r.per.toString(), 44, false))
        row.addView(cell(r.t2, 54, false))
        row.addView(cell(r.t3, 54, false))
        row.addView(cell(r.tl, 54, false))
        tableJugadores.addView(row)
    }

    private fun addEmptyRow(msg: String) {
        val row = TableRow(this)
        val tv = cell(msg, 500, false)
        tv.gravity = Gravity.START
        row.addView(tv)
        tableJugadores.addView(row)
    }

    private fun cell(text: String, widthDp: Int, bold: Boolean): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(dp(6), dp(6), dp(6), dp(6))
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        tv.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        tv.gravity = Gravity.CENTER

        val params = TableRow.LayoutParams(dp(widthDp), TableRow.LayoutParams.WRAP_CONTENT)
        tv.layoutParams = params
        return tv
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    // ---------------- Util Firebase ----------------

    private fun getInt(snapshot: DataSnapshot, key: String): Int {
        val i = snapshot.child(key).getValue(Int::class.java)
        val l = snapshot.child(key).getValue(Long::class.java)
        return i ?: l?.toInt() ?: 0
    }

    private fun calcularPorcentaje(aciertos: Int, intentos: Int): Int {
        return if (intentos == 0) 0 else (100 * aciertos / intentos)
    }
}
