package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegistrarEquipoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_equipo)

        auth = FirebaseAuth.getInstance()

        val etNombreEquipo = findViewById<EditText>(R.id.etNombreEquipo)
        val spinnerCategoria = findViewById<Spinner>(R.id.menuCategoria)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarEquipo)


        val categorias = listOf(
            "Senior Masc.", "Senior Fem.", "Juvenil Masc.", "Junior Fem.",
            "Cadete Masc.", "Cadete Fem.", "Infantil Masc.", "Infantil Fem.",
            "Alevin Masc.", "Alevin Fem.", "Benjamin Masc.", "Benjamin Fem."
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)
        spinnerCategoria.adapter = adapter

        btnGuardar.setOnClickListener {
            val nombreEquipo = etNombreEquipo.text.toString().trim()
            val categoria = spinnerCategoria.selectedItem.toString()

            if (nombreEquipo.isEmpty()) {
                Toast.makeText(this, "Introduce el nombre del equipo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val equipo = mapOf(
                "nombreEquipo" to nombreEquipo,
                "categoria" to categoria,
                "usuarioId" to userId
            )

            val db = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")
            db.getReference("equipos").push().setValue(equipo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Equipo guardado correctamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, pantalla2::class.java)
                    intent.putExtra("nombreEquipo", nombreEquipo)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar equipo: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
