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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class pantalla2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var btnDropdownEquipo: MaterialButton
    private lateinit var btnRegistrarEquipo: MaterialButton
    private lateinit var btnCrearPartido: MaterialButton
    private lateinit var recyclerViewPartidos: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView

    private var nombreEquipoActual: String = ""
    private val listaPartidos = mutableListOf<Partido>()
    private lateinit var partidoAdapter: PartidoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla2)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        btnDropdownEquipo = findViewById(R.id.btnDropdownEquipo)
        btnRegistrarEquipo = findViewById(R.id.btnRegistrarEquipo)
        btnCrearPartido = findViewById(R.id.btnCrearPartido)
        recyclerViewPartidos = findViewById(R.id.recyclerViewPartidos)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        btnCrearPartido.visibility = View.GONE

        recyclerViewPartidos.layoutManager = LinearLayoutManager(this)
        partidoAdapter = PartidoAdapter(
            listaPartidos,
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

        // Verificación simple
        database.getReference("usuarios").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        auth.signOut()
                        startActivity(Intent(this@pantalla2, MainActivity::class.java))
                        finish()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Cargar equipos, decidir equipo activo y cargar partidos de ese equipo
        cargarEquipoActivoYCargarPartidos(userId)

        btnDropdownEquipo.setOnClickListener {
            val popup = PopupMenu(this, btnDropdownEquipo, Gravity.START)
            val borrarEquipo = popup.menu.add("Borrar equipo")
            val cambiarEquipo = popup.menu.add("Cambiar equipo")

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
                                    mostrarAlerta("Éxito", "Equipo(s) eliminado(s) correctamente")
                                    btnRegistrarEquipo.visibility = View.VISIBLE
                                    btnRegistrarEquipo.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    btnRegistrarEquipo.requestLayout()
                                    btnCrearPartido.visibility = View.GONE
                                    nombreEquipoActual = "Sin equipo"
                                    btnDropdownEquipo.text = nombreEquipoActual
                                    guardarEquipoEnPrefs(null)
                                    listaPartidos.clear()
                                    partidoAdapter.notifyDataSetChanged()
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

            cambiarEquipo.setOnMenuItemClickListener {
                mostrarSelectorEquipos() // <<< DIÁLOGO con nombres + categoría
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

        bottomNavigation.selectedItemId = R.id.nav_partidos
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_partidos -> true
                R.id.nav_jugadores -> {
                    if (this.javaClass != Jugadores::class.java) {
                        startActivity(Intent(this, Jugadores::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_configuracion -> {
                    if (this.javaClass != Configuracion::class.java) {
                        startActivity(Intent(this, Configuracion::class.java))
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /** Carga equipos del usuario, decide equipo activo (prefs o primero) y carga partidos de ese equipo */
    private fun cargarEquipoActivoYCargarPartidos(userId: String) {
        val prefsEquipo = leerEquipoDePrefs()
        val ref = database.getReference("equipos")
        ref.orderByChild("usuarioId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        nombreEquipoActual = "Sin equipo"
                        btnDropdownEquipo.text = nombreEquipoActual
                        btnCrearPartido.visibility = View.GONE
                        guardarEquipoEnPrefs(null)
                        listaPartidos.clear()
                        partidoAdapter.notifyDataSetChanged()
                        return
                    }

                    // Equipos del usuario
                    val equipos = snapshot.children.mapNotNull {
                        val nombre = it.child("nombreEquipo").getValue(String::class.java)
                        val cat    = it.child("categoria").getValue(String::class.java) ?: ""
                        if (!nombre.isNullOrBlank()) Pair(nombre, cat) else null
                    }

                    // Elegir equipo activo: el de prefs si existe en la lista, si no, el primero
                    val candidato = equipos.firstOrNull { it.first == prefsEquipo }?.first
                        ?: equipos.first().first

                    nombreEquipoActual = candidato
                    btnDropdownEquipo.text = candidato
                    btnCrearPartido.visibility = View.VISIBLE

                    guardarEquipoEnPrefs(candidato)
                    cargarPartidosParaEquipo(userId, candidato)
                }

                override fun onCancelled(error: DatabaseError) {
                    mostrarAlerta("Error", "Error al cargar equipo: ${error.message}")
                }
            })
    }

    /** Diálogo de selección simple con “Nombre (Categoría)” y aplica selección */
    private fun mostrarSelectorEquipos() {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.getReference("equipos")
        ref.orderByChild("usuarioId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        mostrarAlerta("Sin equipos", "No tienes equipos registrados.")
                        return
                    }

                    val nombres   = mutableListOf<String>()
                    val categorias= mutableListOf<String>()
                    for (child in snapshot.children) {
                        val n = child.child("nombreEquipo").getValue(String::class.java) ?: continue
                        val c = child.child("categoria").getValue(String::class.java) ?: ""
                        nombres.add(n); categorias.add(c)
                    }

                    val items = nombres.mapIndexed { i, n ->
                        if (categorias[i].isNotBlank()) "$n (${categorias[i]})" else n
                    }.toTypedArray()

                    AlertDialog.Builder(this@pantalla2)
                        .setTitle("Elige equipo")
                        .setItems(items) { _, which ->
                            val nuevo = nombres[which]
                            if (nuevo != nombreEquipoActual) {
                                nombreEquipoActual = nuevo
                                btnDropdownEquipo.text = nuevo
                                guardarEquipoEnPrefs(nuevo)
                                cargarPartidosParaEquipo(uid, nuevo)
                            }
                        }
                        .show()
                }
                override fun onCancelled(error: DatabaseError) {
                    mostrarAlerta("Error", "No se pudieron cargar los equipos: ${error.message}")
                }
            })
    }

    /** Carga los partidos del usuario filtrando por el equipo seleccionado */
    private fun cargarPartidosParaEquipo(userId: String, equipo: String) {
        val partidosRef = database.getReference("partidos")
        partidosRef.orderByChild("usuarioId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaPartidos.clear()
                    for (snap in snapshot.children) {
                        val partido = snap.getValue(Partido::class.java)
                        partido?.let {
                            it.id = snap.key
                            // Asegura el nombreEquipo
                            if (it.nombreEquipo.isNullOrBlank()) it.nombreEquipo = equipo
                            // Filtrado por equipo activo
                            if (it.nombreEquipo == equipo) {
                                listaPartidos.add(it)
                            }
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
        AlertDialog.Builder(this)
            .setTitle("Eliminar partido")
            .setMessage("¿Estás seguro de que quieres eliminar este partido?")
            .setPositiveButton("Sí") { _, _ ->
                partido.id?.let { id ->
                    database.getReference("partidos").child(id).removeValue()
                        .addOnSuccessListener {
                            listaPartidos.remove(partido)
                            partidoAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener {
                            mostrarAlerta("Error", "No se pudo eliminar el partido: ${it.message}")
                        }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    // ===== SharedPreferences helpers =====
    private fun guardarEquipoEnPrefs(nombre: String?) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
        if (nombre.isNullOrBlank()) prefs.remove("equipoActualNombre")
        else prefs.putString("equipoActualNombre", nombre)
        prefs.apply()
    }

    private fun leerEquipoDePrefs(): String? {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("equipoActualNombre", null)
    }
}
