package com.example.miguelpavonlimones_tfg

import PartidoAdapter
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
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

        btnDropdownUsuario = findViewById(R.id.btnDropdownUsuario)
        btnDropdownEquipo = findViewById(R.id.btnDropdownEquipo)
        btnRegistrarEquipo = findViewById(R.id.btnRegistrarEquipo)
        btnCrearPartido = findViewById(R.id.btnCrearPartido)
        recyclerViewPartidos = findViewById(R.id.recyclerViewPartidos)

        btnCrearPartido.visibility = View.GONE

        recyclerViewPartidos.layoutManager = LinearLayoutManager(this)
        partidoAdapter = PartidoAdapter(listaPartidos,
            onItemClick = { partidoSeleccionado ->
                val intent = Intent(this, PantallaDeEstadisticaActivity::class.java)
                intent.putExtra("partidoId", partidoSeleccionado.id)
                intent.putExtra("fecha", partidoSeleccionado.fecha)
                intent.putExtra("rival", partidoSeleccionado.rival)
                intent.putExtra("jornada", partidoSeleccionado.jornada.toString())
                intent.putExtra("tipo", partidoSeleccionado.tipo)
                intent.putExtra("nombreEquipo", partidoSeleccionado.nombreEquipo)
                startActivity(intent)
            },
            onDeleteClick = { partido ->
                mostrarConfirmacionEliminacion(partido)
            }
        )

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

        btnDropdownUsuario.setOnClickListener {
            val popup = PopupMenu(this, btnDropdownUsuario, Gravity.END)
            val eliminarUsuario = popup.menu.add("Eliminar usuario")
            val cerrarSesion = popup.menu.add("Cerrar sesión")
            cerrarSesion.setOnMenuItemClickListener {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            eliminarUsuario.setOnMenuItemClickListener {
                val usuario = auth.currentUser
                val uid = usuario?.uid

                if (usuario != null && uid != null) {
                    // Eliminar datos relacionados
                    val databaseRef = database.reference
                    val updates = hashMapOf<String, Any?>()
                    updates["usuarios/$uid"] = null
                    updates["partidos"] = null
                    updates["equipos"] = null

                    databaseRef.updateChildren(updates).addOnCompleteListener {
                        usuario.delete().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                mostrarAlerta("Éxito", "Usuario eliminado completamente")
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            } else {
                                mostrarAlerta("Error", "No se pudo eliminar el usuario: ${task.exception?.message}")
                            }
                        }
                    }
                } else {
                    mostrarAlerta("Error", "No se pudo obtener el usuario actual")
                }
                true
            }
            popup.show()
        }

        btnDropdownEquipo.setOnClickListener {
            val popup = PopupMenu(this, btnDropdownEquipo, Gravity.START)
            val borrarEquipo = popup.menu.add("Borrar equipo")

            borrarEquipo.setOnMenuItemClickListener {
                val uid = auth.currentUser?.uid

                if (uid != null) {
                    val ref = database.getReference("equipos")
                    ref.orderByChild("usuarioId").equalTo(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    for (equipoSnap in snapshot.children) {
                                        equipoSnap.ref.removeValue()
                                    }
                                    mostrarAlerta("Éxito", "Equipo eliminado correctamente")

                                    btnRegistrarEquipo.visibility = View.VISIBLE
                                    btnRegistrarEquipo.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    btnRegistrarEquipo.requestLayout()
                                    btnCrearPartido.visibility = View.GONE
                                    btnDropdownEquipo.text = "Sin equipo"
                                } else {
                                    mostrarAlerta("Error", "No se encontró equipo para eliminar")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                mostrarAlerta("Error", "Error al eliminar equipo: ${error.message}")
                            }
                        })
                } else {
                    mostrarAlerta("Error", "Usuario no autenticado")
                }
                true
            }
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
                            it.id = snap.key
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

    private fun mostrarConfirmacionEliminacion(partido: Partido) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar partido")
        builder.setMessage("¿Estás seguro de que quieres eliminar este partido?")
        builder.setPositiveButton("Sí") { _, _ ->
            partido.id?.let { id ->
                val ref = database.getReference("partidos").child(id)
                ref.removeValue().addOnSuccessListener {
                    listaPartidos.remove(partido)
                    partidoAdapter.notifyDataSetChanged()
                }.addOnFailureListener {
                    mostrarAlerta("Error", "No se pudo eliminar el partido: ${it.message}")
                }
            }
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
}
