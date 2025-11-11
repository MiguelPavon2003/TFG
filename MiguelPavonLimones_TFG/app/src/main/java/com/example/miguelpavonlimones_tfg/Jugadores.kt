package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Jugadores : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private var uid: String? = null

    // Equipo actual (desde preferencias)
    private var nombreEquipoActual: String? = null
    private var nombreEquipoCargado: String? = null
    private var refJugadores: DatabaseReference? = null
    private var listenerJugadores: ValueEventListener? = null

    // Bottom nav
    private lateinit var bottomNavigation: BottomNavigationView

    // Formulario
    private lateinit var etNomJugador: EditText
    private lateinit var etApellJugador: EditText
    private lateinit var etNumJugador: EditText
    private lateinit var btnGuardarJugador: Button

    // UI lista y contador
    private lateinit var tvFichas: TextView
    private lateinit var recyclerViewJugadores: RecyclerView

    // Datos en memoria
    private val jugadores = mutableListOf<Jugador>()
    private val clavesJugadores = mutableListOf<String>()
    val nombresJugadores = mutableListOf<String>()  // para usar en otras pantallas

    // Adaptador
    private val adapter by lazy { JugadorAdapter(jugadores) { pos -> eliminarJugador(pos) } }

    // Límite de plantilla
    private val maxFichas = 16

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jugadores)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db   = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")
        uid  = auth.currentUser?.uid

        // Referencias UI
        bottomNavigation    = findViewById(R.id.bottomNavigation)
        etNomJugador        = findViewById(R.id.etNomJugador)
        etApellJugador      = findViewById(R.id.etApellJugador)
        etNumJugador        = findViewById(R.id.etNumJugador)
        btnGuardarJugador   = findViewById(R.id.btnGuardarJugador)
        tvFichas            = findViewById(R.id.tvFichas)
        recyclerViewJugadores = findViewById(R.id.recyclerViewJugadores)

        // RecyclerView
        recyclerViewJugadores.layoutManager = LinearLayoutManager(this)
        recyclerViewJugadores.setHasFixedSize(true)
        recyclerViewJugadores.adapter = adapter

        // Contador inicial
        actualizarFichas()

        // Guardar
        btnGuardarJugador.setOnClickListener { onGuardarJugador() }

        // Enter en dorsal => guardar
        etNumJugador.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onGuardarJugador()
                true
            } else false
        }

        // Bottom navigation
        bottomNavigation.selectedItemId = R.id.nav_jugadores
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_partidos -> {
                    if (this.javaClass != pantalla2::class.java) {
                        startActivity(Intent(this, pantalla2::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_jugadores -> true
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

        // Cargar equipo seleccionado y configurar ruta
        if (uid == null) {
            mostrarAlerta("Error", "Usuario no autenticado.")
            btnGuardarJugador.isEnabled = false
        } else {
            nombreEquipoActual = leerEquipoDesdePrefs()
            if (nombreEquipoActual.isNullOrBlank()) {
                // Fallback: si no hay selección guardada, tomamos el primero del usuario
                cargarPrimerEquipoYConfigurarRuta(uid!!)
            } else {
                configurarRutaJugadores(uid!!, nombreEquipoActual!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Por si el usuario cambió de equipo en pantalla2
        val equipoPrefs = leerEquipoDesdePrefs()
        if (uid != null && !equipoPrefs.isNullOrBlank() && equipoPrefs != nombreEquipoCargado) {
            configurarRutaJugadores(uid!!, equipoPrefs!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpia listener para evitar fugas
        quitarListener()
    }

    /** Lee el nombre del equipo seleccionado en SharedPreferences (lo guarda pantalla2 al elegir equipo) */
    private fun leerEquipoDesdePrefs(): String? {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getString("equipoActualNombre", null)
    }

    /** Fallback: si no hay equipo en prefs, coger el primero del usuario y guardarlo en prefs */
    private fun cargarPrimerEquipoYConfigurarRuta(userId: String) {
        val equipoRef = db.getReference("equipos")
        equipoRef.orderByChild("usuarioId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        mostrarAlerta("Aviso", "No tienes equipo. Crea uno para registrar jugadores.")
                        btnGuardarJugador.isEnabled = false
                        return
                    }
                    val equipoSnap = snapshot.children.first()
                    val nombre = equipoSnap.child("nombreEquipo").getValue(String::class.java)
                    if (nombre.isNullOrBlank()) {
                        mostrarAlerta("Error", "Nombre de equipo inválido.")
                        btnGuardarJugador.isEnabled = false
                        return
                    }
                    // Guardar en prefs para coherencia con pantalla2
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("equipoActualNombre", nombre)
                        .apply()

                    configurarRutaJugadores(userId, nombre)
                }
                override fun onCancelled(error: DatabaseError) {
                    mostrarAlerta("Error", "No se pudo cargar el equipo: ${error.message}")
                }
            })
    }

    /** Configura la ruta jugadores/{uid}/{equipo} y engancha listener */
    private fun configurarRutaJugadores(userId: String, nombreEquipo: String) {
        // Quitar listener anterior si hubiera
        quitarListener()

        nombreEquipoActual  = nombreEquipo
        nombreEquipoCargado = nombreEquipo
        refJugadores = db.getReference("jugadores").child(userId).child(nombreEquipo)

        // Escuchar lista de jugadores en tiempo real
        listenerJugadores = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                jugadores.clear()
                clavesJugadores.clear()
                nombresJugadores.clear()

                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val nombre = child.child("nombre").getValue(String::class.java) ?: ""
                    val apellido = child.child("apellido").getValue(String::class.java) ?: ""

                    // dorsal puede venir como Int o Long
                    val dorsalInt = child.child("dorsal").getValue(Int::class.java)
                    val dorsalLong = child.child("dorsal").getValue(Long::class.java)
                    val dorsal = dorsalInt ?: dorsalLong?.toInt() ?: 0

                    jugadores.add(Jugador(nombre, apellido, dorsal))
                    clavesJugadores.add(key)
                    nombresJugadores.add("$nombre $apellido")
                }

                // Orden por dorsal y reorden de claves
                val orden = jugadores.withIndex().sortedBy { it.value.dorsal }
                val jugadoresOrdenados = orden.map { it.value }
                val clavesOrdenadas = orden.map { clavesJugadores[it.index] }

                jugadores.clear(); jugadores.addAll(jugadoresOrdenados)
                clavesJugadores.clear(); clavesJugadores.addAll(clavesOrdenadas)

                adapter.notifyDataSetChanged()
                actualizarFichas()
            }

            override fun onCancelled(error: DatabaseError) {
                mostrarAlerta("Error", "No se pudieron cargar los jugadores: ${error.message}")
            }
        }

        refJugadores?.addValueEventListener(listenerJugadores as ValueEventListener)
        // Habilitar guardar si hay ruta
        btnGuardarJugador.isEnabled = true
    }

    private fun quitarListener() {
        listenerJugadores?.let { lst ->
            refJugadores?.removeEventListener(lst)
        }
        listenerJugadores = null
    }

    private fun onGuardarJugador() {
        val nombre = etNomJugador.text.toString().trim()
        val apellido = etApellJugador.text.toString().trim()
        val dorsalTexto = etNumJugador.text.toString().trim()

        if (nombre.isEmpty() || apellido.isEmpty() || dorsalTexto.isEmpty()) {
            mostrarAlerta("Campos incompletos", "Rellena nombre, apellido y dorsal.")
            return
        }

        val dorsal = dorsalTexto.toIntOrNull()
        if (dorsal == null || dorsal < 0) {
            mostrarAlerta("Dorsal inválido", "Introduce un dorsal numérico válido.")
            return
        }

        if (jugadores.size >= maxFichas) {
            mostrarAlerta("Límite alcanzado", "Ya has registrado $maxFichas fichas.")
            return
        }

        if (jugadores.any { it.dorsal == dorsal }) {
            mostrarAlerta("Dorsal repetido", "Ya existe un jugador con el dorsal $dorsal.")
            return
        }

        val ruta = refJugadores ?: run {
            mostrarAlerta("Error", "No hay ruta de equipo configurada.")
            return
        }

        val data = mapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "dorsal" to dorsal
        )

        ruta.push().setValue(data)
            .addOnSuccessListener {
                etNomJugador.text?.clear()
                etApellJugador.text?.clear()
                etNumJugador.text?.clear()
                etNomJugador.requestFocus()
                // La lista y el contador se actualizan por el listener
            }
            .addOnFailureListener {
                mostrarAlerta("Error", "No se pudo guardar el jugador: ${it.message}")
            }
    }

    private fun eliminarJugador(position: Int) {
        if (position !in jugadores.indices || position !in clavesJugadores.indices) return
        val key = clavesJugadores[position]
        val ruta = refJugadores ?: return

        ruta.child(key).removeValue()
            .addOnFailureListener {
                mostrarAlerta("Error", "No se pudo eliminar el jugador: ${it.message}")
            }
    }

    private fun actualizarFichas() {
        tvFichas.text = "Fichas (${jugadores.size}/$maxFichas)"
        btnGuardarJugador.isEnabled = jugadores.size < maxFichas
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
}
