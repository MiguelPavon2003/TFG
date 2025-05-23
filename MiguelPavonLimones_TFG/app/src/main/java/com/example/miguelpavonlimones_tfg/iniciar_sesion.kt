package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class iniciar_sesion : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_iniciar_sesion)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail1)
        val etContraseña = findViewById<EditText>(R.id.etContraseña1)
        val btnInicioSesion = findViewById<Button>(R.id.btnIniciarSesion)

        btnInicioSesion.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etContraseña.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                mostrarAlerta("Campos incompletos", "Introduce correo y contraseña.")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, pantalla2::class.java))
                        finish()
                    } else {
                        mostrarAlerta("Error", task.exception?.message ?: "No se pudo iniciar sesión.")
                    }
                }
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
}
