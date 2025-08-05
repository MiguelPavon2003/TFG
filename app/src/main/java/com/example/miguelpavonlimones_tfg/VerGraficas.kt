package com.example.miguelpavonlimones_tfg

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.example.miguelpavonlimones_tfg.BarChartCanvasView

class VerGraficas : AppCompatActivity() {

    private lateinit var graficaTiro: BarChartCanvasView
    private lateinit var graficaOtras: BarChartCanvasView

    private lateinit var database: FirebaseDatabase
    private lateinit var refEstadisticas: DatabaseReference

    private var partidoId = ""
    private var nombreEquipo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_graficas)

        graficaTiro = findViewById(R.id.graficaTiro)
        graficaOtras = findViewById(R.id.graficaOtras)

        partidoId = intent.getStringExtra("partidoId") ?: ""
        nombreEquipo = intent.getStringExtra("nombreEquipo") ?: ""

        database = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")
        refEstadisticas = database.getReference("estadisticas").child(partidoId).child(nombreEquipo)

        escucharCambios()
    }

    private fun escucharCambios() {
        refEstadisticas.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val intentos2 = snapshot.child("tiros2Intentados").getValue(Int::class.java) ?: 0
                val anotados2 = snapshot.child("tiros2Anotados").getValue(Int::class.java) ?: 0
                val intentos3 = snapshot.child("tiros3Intentados").getValue(Int::class.java) ?: 0
                val anotados3 = snapshot.child("tiros3Anotados").getValue(Int::class.java) ?: 0
                val intentosTL = snapshot.child("tirosLibresIntentados").getValue(Int::class.java) ?: 0
                val anotadosTL = snapshot.child("tirosLibresAnotados").getValue(Int::class.java) ?: 0

                val datosTiro = mutableMapOf<String, Int>()
                datosTiro["Triple"] = calcularPorcentaje(anotados3, intentos3)
                datosTiro["Tiro de 2"] = calcularPorcentaje(anotados2, intentos2)
                datosTiro["Tiro Libre"] = calcularPorcentaje(anotadosTL, intentosTL)

                val detallesTiro = mutableMapOf<String, String>()
                detallesTiro["Triple"] = "$anotados3/$intentos3"
                detallesTiro["Tiro de 2"] = "$anotados2/$intentos2"
                detallesTiro["Tiro Libre"] = "$anotadosTL/$intentosTL"

                graficaTiro.setDatos(datosTiro, true, detallesTiro)

                val rebOf = snapshot.child("reboteOfensivo").getValue(Int::class.java) ?: 0
                val rebDef = snapshot.child("reboteDefensivo").getValue(Int::class.java) ?: 0
                val rebTot = snapshot.child("rebotes").getValue(Int::class.java) ?: 0

                val otrasStats = mutableMapOf<String, Int>()
                otrasStats["Reb Of"] = rebOf
                otrasStats["Reb Def"] = rebDef
                otrasStats["Reb Tot"] = rebTot
                otrasStats["Asist"] = snapshot.child("asistencias").getValue(Int::class.java) ?: 0
                otrasStats["Robos"] = snapshot.child("robos").getValue(Int::class.java) ?: 0
                otrasStats["Tapones"] = snapshot.child("tapones").getValue(Int::class.java) ?: 0
                otrasStats["Faltas"] = snapshot.child("faltas").getValue(Int::class.java) ?: 0
                otrasStats["Pérdidas"] = snapshot.child("perdidas").getValue(Int::class.java) ?: 0

                graficaOtras.setDatos(otrasStats, false)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun calcularPorcentaje(aciertos: Int, intentos: Int): Int {
        return if (intentos == 0) 0 else (100 * aciertos / intentos)
    }
}
