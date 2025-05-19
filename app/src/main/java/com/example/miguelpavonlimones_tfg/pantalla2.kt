package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class pantalla2 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvNombreEquipo: TextView
    private lateinit var btnRegistrarEquipo: Button
    private lateinit var btnCrearPartido: Button  // Nuevo botón

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla2)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        tvNombreUsuario = findViewById(R.id.tvNombreUsuario)
        tvNombreEquipo = findViewById(R.id.tvNombreEquipo)
        btnRegistrarEquipo = findViewById(R.id.btnRegistrarEquipo)
        btnCrearPartido = findViewById(R.id.btnCrearPartido)

        btnCrearPartido.visibility = View.GONE  // Ocultar por defecto

        val userId = auth.currentUser?.uid

        // Mostrar nombre de usuario desde Firebase
        if (userId != null) {
            val userRef: DatabaseReference = database.getReference("usuarios").child(userId)
            userRef.child("nombre").get().addOnSuccessListener { snapshot ->
                val nombreUsuario = snapshot.value.toString()
                tvNombreUsuario.text = "$nombreUsuario ⌄"
            }.addOnFailureListener {
                Toast.makeText(this, "No se pudo obtener el nombre del usuario", Toast.LENGTH_SHORT).show()
            }
        }

        // Mostrar nombre del equipo si se pasó por Intent
        val nombreEquipoPasado = intent.getStringExtra("nombreEquipo")
        if (!nombreEquipoPasado.isNullOrEmpty()) {
            tvNombreEquipo.text = nombreEquipoPasado
            btnCrearPartido.visibility = View.VISIBLE  // Mostrar botón si hay equipo
        } else {
            tvNombreEquipo.text = "No tienes equipo"
            btnCrearPartido.visibility = View.GONE
        }

        // Acción del botón para registrar un equipo
        btnRegistrarEquipo.setOnClickListener {
            val intent = Intent(this, RegistrarEquipoActivity::class.java)
            startActivity(intent)
        }

        // Acción del botón para crear un partido
        btnCrearPartido.setOnClickListener {
            //val intent = Intent(this, CrearPartidoActivity::class.java)
            startActivity(intent)
        }
    }
}
