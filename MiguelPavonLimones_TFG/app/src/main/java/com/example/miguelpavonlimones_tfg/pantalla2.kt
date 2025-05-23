package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class pantalla2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var btnDropdownUsuario: MaterialButton
    private lateinit var btnDropdownEquipo: MaterialButton
    private lateinit var btnRegistrarEquipo: Button
    private lateinit var btnCrearPartido: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla2)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        btnDropdownUsuario = findViewById(R.id.btnDropdownUsuario)
        btnDropdownEquipo = findViewById(R.id.btnDropdownEquipo)
        btnRegistrarEquipo = findViewById(R.id.btnRegistrarEquipo)
        btnCrearPartido = findViewById(R.id.btnCrearPartido)

        btnCrearPartido.visibility = View.GONE

        val userId = auth.currentUser?.uid

        // Si el usuario no está autenticado, volver al login
        if (userId == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val userRef: DatabaseReference = database.getReference("usuarios").child(userId)

        // Verificar que el nodo del usuario exista
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
                Toast.makeText(this@pantalla2, "Error al verificar usuario", Toast.LENGTH_SHORT).show()
            }
        })

        // Cargar equipo asociado al usuario
        val equipoRef = database.getReference("equipos")
        equipoRef.orderByChild("usuarioId").equalTo(userId).limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (equipoSnap in snapshot.children) {
                            val nombreEquipo = equipoSnap.child("nombreEquipo").value?.toString() ?: "Sin equipo"
                            btnDropdownEquipo.text = "$nombreEquipo "
                            btnCrearPartido.visibility = View.VISIBLE
                        }
                    } else {
                        btnDropdownEquipo.text = "Sin equipo "
                        btnCrearPartido.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@pantalla2, "Error al cargar el equipo", Toast.LENGTH_SHORT).show()
                }
            })

        // Menú usuario
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

        // Menú equipo
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
            Toast.makeText(this, "Aquí iría la lógica para crear partido", Toast.LENGTH_SHORT).show()
        }
    }
}
