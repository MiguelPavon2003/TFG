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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_jugadores)

        tvContador  = findViewById(R.id.tvContador)
        rv          = findViewById(R.id.rvSelectJugadores)
        btnGuardar  = findViewById(R.id.btnGuardarConvocados)
        btnCancelar = findViewById(R.id.btnCancelar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        partidoId   = intent.getStringExtra("partidoId") ?: ""
        nombreEquipo= intent.getStringExtra("nombreEquipo") ?: ""

        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty() || partidoId.isEmpty() || nombreEquipo.isEmpty()) {
            Toast.makeText(this, "Faltan datos para cargar jugadores", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        refJugadores  = db.getReference("jugadores").child(uid).child(nombreEquipo)
        refConvocados = db.getReference("convocados").child(partidoId)

        adapter = SelectJugadorAdapter(lista, maxSeleccion) { count ->
            tvContador.text = "($count/$maxSeleccion)"
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        cargarJugadoresYConvocados()

        btnGuardar.setOnClickListener { guardarSeleccion() }
        btnCancelar.setOnClickListener { finish() }
    }

    /** Carga jugadores y marca los ya convocados */
    private fun cargarJugadoresYConvocados() {
        // Primero leemos convocados para marcar luego
        refConvocados.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(convocadosSnap: DataSnapshot) {
                val yaConvocados = convocadosSnap.children
                    .filter { it.getValue(Boolean::class.java) == true }
                    .mapNotNull { it.key }
                    .toSet()

                // Ahora leemos jugadores
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

                        // Actualizar contador inicial
                        tvContador.text = "(${lista.count { it.selected }}/$maxSeleccion)"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SelectJugadoresActivity, "Error al cargar jugadores: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SelectJugadoresActivity, "Error al leer convocados: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    /** Guarda selección en /convocados/{partidoId}/{key} = true/false */
    private fun guardarSeleccion() {
        val seleccionados = adapter.getSeleccionados()
        val updates = hashMapOf<String, Any?>()

        // Primero, marcar todos a false (o eliminarlos) para dejar solo los actuales
        // Opción A (mantener historial): ponemos false a los no seleccionados existentes
        // Cargamos las claves actuales y decidimos:
        refConvocados.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Poner false a todos los que existan
                for (child in snapshot.children) {
                    updates[child.key!!] = false
                }
                // Poner true a los seleccionados
                for (s in seleccionados) {
                    updates[s.key] = true
                }

                refConvocados.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@SelectJugadoresActivity, "Convocados guardados", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@SelectJugadoresActivity, "Error al guardar", Toast.LENGTH_LONG).show()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SelectJugadoresActivity, "Error al actualizar: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
