package com.example.miguelpavonlimones_tfg

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SelectJugadoresActivity : AppCompatActivity() {

    private lateinit var tvContador: TextView
    private lateinit var rv: RecyclerView
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    private lateinit var refJugadores: DatabaseReference
    private lateinit var refConvocados: DatabaseReference

    private val lista = mutableListOf<SelJugador>()
    private lateinit var adapter: SelectJugadorAdapter

    private var partidoId: String = ""
    private var nombreEquipo: String = ""
    private val maxSeleccion = 12

    private var convocatoriaBloqueada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_jugadores)

        tvContador  = findViewById(R.id.tvContador)
        rv          = findViewById(R.id.rvSelectJugadores)
        btnGuardar  = findViewById(R.id.btnGuardarConvocados)
        btnCancelar = findViewById(R.id.btnCancelar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        partidoId    = intent.getStringExtra("partidoId") ?: ""
        nombreEquipo = intent.getStringExtra("nombreEquipo") ?: ""

        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty() || partidoId.isEmpty() || nombreEquipo.isEmpty()) {
            Toast.makeText(this, "Faltan datos para cargar jugadores", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        refJugadores  = db.getReference("jugadores").child(uid).child(nombreEquipo)
        refConvocados = db.getReference("convocados").child(partidoId)

        adapter = SelectJugadorAdapter(
            datos = lista,
            maxSeleccion = maxSeleccion
        ) { count ->
            tvContador.text = "($count/$maxSeleccion)"
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.setHasFixedSize(true)
        rv.adapter = adapter

        btnGuardar.setOnClickListener { guardarSeleccion() }
        btnCancelar.setOnClickListener { finish() }

        cargarJugadoresYConvocados()
    }

    /** Carga jugadores, convocados y comprueba si ya está bloqueado */
    private fun cargarJugadoresYConvocados() {
        refConvocados.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(convocadosSnap: DataSnapshot) {

                // 1) Leer metadatos de forma segura
                convocatoriaBloqueada = (convocadosSnap.child("_locked").value as? Boolean) == true
                val equipoGuardado = convocadosSnap.child("_equipo").getValue(String::class.java)

                // Si ya hay equipo guardado y no coincide con el actual -> bloquear (evita mezclar)
                if (!equipoGuardado.isNullOrBlank() && equipoGuardado != nombreEquipo) {
                    convocatoriaBloqueada = true
                    Toast.makeText(
                        this@SelectJugadoresActivity,
                        "La convocatoria pertenece al equipo \"$equipoGuardado\" y no se puede cambiar.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // 2) Sacar claves convocadas TRUE (solo boolean true)
                val yaConvocados = mutableSetOf<String>()
                for (child in convocadosSnap.children) {
                    val key = child.key ?: continue
                    if (key == "_locked" || key == "_equipo") continue

                    val v = child.value
                    if (v is Boolean && v) {
                        yaConvocados.add(key)
                    }
                }

                // 3) Leer jugadores del equipo
                refJugadores.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(jugSnap: DataSnapshot) {
                        lista.clear()

                        for (child in jugSnap.children) {
                            val key = child.key ?: continue
                            val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                            val apellido = child.child("apellido").getValue(String::class.java) ?: ""

                            val dorsalInt = child.child("dorsal").getValue(Int::class.java)
                            val dorsalLong = child.child("dorsal").getValue(Long::class.java)
                            val dorsal = dorsalInt ?: dorsalLong?.toInt() ?: 0

                            lista.add(
                                SelJugador(
                                    key = key,
                                    nombre = nombre,
                                    apellido = apellido,
                                    dorsal = dorsal,
                                    selected = yaConvocados.contains(key)
                                )
                            )
                        }

                        // Orden por dorsal
                        lista.sortBy { it.dorsal }
                        adapter.notifyDataSetChanged()

                        val count = lista.count { it.selected }
                        tvContador.text = "($count/$maxSeleccion)"

                        aplicarBloqueoUI()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@SelectJugadoresActivity,
                            "Error al cargar jugadores: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@SelectJugadoresActivity,
                    "Error al leer convocados: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    /** Si está bloqueado: no se puede modificar ni guardar */
    private fun aplicarBloqueoUI() {
        if (convocatoriaBloqueada) {
            btnGuardar.isEnabled = false
            btnGuardar.text = "Convocados guardados"
            adapter.setLocked(true)
        } else {
            btnGuardar.isEnabled = true
            btnGuardar.text = "Guardar"
            adapter.setLocked(false)
        }
    }

    /** Guarda selección en /convocados/{partidoId} y bloquea */
    private fun guardarSeleccion() {
        if (convocatoriaBloqueada) {
            Toast.makeText(this, "La convocatoria ya está cerrada", Toast.LENGTH_SHORT).show()
            return
        }

        val seleccionados = adapter.getSeleccionados()

        if (seleccionados.size > maxSeleccion) {
            Toast.makeText(this, "Máximo $maxSeleccion jugadores", Toast.LENGTH_LONG).show()
            return
        }

        // Reescritura completa (simple y limpia)
        val nuevoNodo = hashMapOf<String, Any?>()

        for (s in seleccionados) {
            nuevoNodo[s.key] = true
        }

        // Metadatos
        nuevoNodo["_equipo"] = nombreEquipo
        nuevoNodo["_locked"] = true

        refConvocados.setValue(nuevoNodo).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Convocados guardados", Toast.LENGTH_SHORT).show()
                convocatoriaBloqueada = true
                aplicarBloqueoUI()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Error al guardar: ${task.exception?.message ?: ""}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
