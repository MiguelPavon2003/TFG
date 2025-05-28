package com.example.miguelpavonlimones_tfg

import PartidoAdapter
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class pantalla2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var btnDropdownUsuario: MaterialButton
    private lateinit var btnDropdownEquipo: MaterialButton
    private lateinit var btnRegistrarEquipo: MaterialButton
    private lateinit var btnCrearPartido: MaterialButton
    private lateinit var recyclerViewPartidos: RecyclerView

    private var nombreEquipoActual: String = ""
    private val listaPartidos = mutableListOf<Partido>()
    private lateinit var partidoAdapter: PartidoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla2)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        // Views
        btnDropdownUsuario = findViewById(R.id.btnDropdownUsuario)
        btnDropdownEquipo = findViewById(R.id.btnDropdownEquipo)
        btnRegistrarEquipo = findViewById(R.id.btnRegistrarEquipo)
        btnCrearPartido = findViewById(R.id.btnCrearPartido)
        recyclerViewPartidos = findViewById(R.id.recyclerViewPartidos)

        btnCrearPartido.visibility = View.GONE

        // Setup RecyclerView
        recyclerViewPartidos.layoutManager = LinearLayoutManager(this)
        partidoAdapter = PartidoAdapter(listaPartidos) { partidoSeleccionado ->
            val intent = Intent(this, PantallaDeEstadisticaActivity::class.java)
            intent.putExtra("fecha", partidoSeleccionado.fecha)
            intent.putExtra("rival", partidoSeleccionado.rival)
            intent.putExtra("tipo", partidoSeleccionado.tipo)
            intent.putExtra("equipo", partidoSeleccionado.nombreEquipo)
            startActivity(intent)
        }
        recyclerViewPartidos.adapter = partidoAdapter

        val userId = auth.currentUser?.uid

        if (userId == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val userRef = database.getReference("usuarios").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    auth.signOut()
                    startActivity(Intent(this@pantalla2, MainActivity::class.java))
                    finish()
                } else {
                    val nombreUsuario = snapshot.child("nombre").value?.toString() ?: "Usuario"
                    btnDropdownUsuario.text = "$nombreUsuario "
                }
            }

            override fun onCancelled(error: DatabaseError) {
                mostrarAlerta("Error", "Error al verificar usuario")
            }
        })

        val equipoRef = database.getReference("equipos")
        equipoRef.orderByChild("usuarioId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val equipoSnap = snapshot.children.first()
                        nombreEquipoActual = equipoSnap.child("nombreEquipo").value?.toString() ?: "Sin equipo"
                        btnDropdownEquipo.text = "$nombreEquipoActual "
                        btnCrearPartido.visibility = View.VISIBLE

                        // Solo cargamos partidos después de tener el nombre de equipo
                        cargarPartidos(userId)
                    } else {
                        nombreEquipoActual = "Sin equipo"
                        btnDropdownEquipo.text = nombreEquipoActual
                        btnCrearPartido.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    mostrarAlerta("Error", "Error al cargar equipo")
                }
            })

        // Menús
        btnDropdownUsuario.setOnClickListener {
            val popup = PopupMenu(this, btnDropdownUsuario, Gravity.END)
            popup.menu.add("Perfil")
            popup.menu.add("Configuración")
            val cerrarSesion = popup.menu.add("Cerrar sesión")
            cerrarSesion.setOnMenuItemClickListener {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            popup.show()
        }

        btnDropdownEquipo.setOnClickListener {
            val popup = PopupMenu(this, btnDropdownEquipo, Gravity.START)
            popup.menu.add("Ver equipo")
            popup.menu.add("Editar equipo")
            popup.menu.add("Borrar equipo")
            popup.show()
        }

        btnRegistrarEquipo.setOnClickListener {
            startActivity(Intent(this, RegistrarEquipoActivity::class.java))
        }

        btnCrearPartido.setOnClickListener {
            val intent = Intent(this, RegistrarPartidoActivity::class.java)
            intent.putExtra("nombreEquipo", nombreEquipoActual)
            startActivity(intent)
        }
    }

    private fun cargarPartidos(userId: String) {
        val partidosRef = database.getReference("partidos")
        partidosRef.orderByChild("usuarioId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaPartidos.clear()
                    for (snap in snapshot.children) {
                        val partido = snap.getValue(Partido::class.java)
                        partido?.let {
                            if (it.nombreEquipo.isNullOrBlank()) {
                                it.nombreEquipo = nombreEquipoActual
                            }
                            listaPartidos.add(it)
                        }
                    }
                    listaPartidos.sortByDescending { it.fecha }
                    partidoAdapter.notifyDataSetChanged()

                }

                override fun onCancelled(error: DatabaseError) {
                    mostrarAlerta("Error", "No se pudieron cargar los partidos: ${error.message}")
                }
            })
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
}
