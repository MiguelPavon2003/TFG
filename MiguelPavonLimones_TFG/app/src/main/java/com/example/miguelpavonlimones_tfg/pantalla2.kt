package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
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
        database = FirebaseDatabase.getInstance()

        btnDropdownUsuario = findViewById(R.id.btnDropdownUsuario)
        btnDropdownEquipo = findViewById(R.id.btnDropdownEquipo)
        btnRegistrarEquipo = findViewById(R.id.btnRegistrarEquipo)
        btnCrearPartido = findViewById(R.id.btnCrearPartido)

        btnCrearPartido.visibility = View.GONE

        val userId = auth.currentUser?.uid


        if (userId != null) {
            val userRef: DatabaseReference = database.getReference("usuarios").child(userId)
            userRef.child("nombre").get().addOnSuccessListener { snapshot ->
                val nombreUsuario = snapshot.value.toString()
                btnDropdownUsuario.text = "$nombreUsuario ⌄"
            }
        }


        val nombreEquipo = intent.getStringExtra("nombreEquipo")
        if (!nombreEquipo.isNullOrEmpty()) {
            btnDropdownEquipo.text = "$nombreEquipo ⌄"
            btnCrearPartido.visibility = View.VISIBLE
        } else {
            btnDropdownEquipo.text = "Sin equipo ⌄"
        }

        // Menú usuario
        btnDropdownUsuario.setOnClickListener {
            val popup = PopupMenu(this, btnDropdownUsuario, Gravity.END)
            popup.menu.add("Perfil")
            popup.menu.add("Configuración")
            val cerrarSesion = popup.menu.add("Cerrar sesión")
            cerrarSesion.setTitleCondensed("Cerrar sesión")
            cerrarSesion.setOnMenuItemClickListener {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                true
            }
            // Poner en rojo "Cerrar sesión"
            popup.setOnMenuItemClickListener { item ->
                if (item.title == "Cerrar sesión") {
                    item.title = "Cerrar sesión"
                }
                item.setTitle(item.title)
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
