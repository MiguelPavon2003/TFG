package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val etNombre = findViewById<EditText>(R.id.etNomUsu)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etContraseña = findViewById<EditText>(R.id.etContraseña)
        val tvIniciarSesion = findViewById<TextView>(R.id.tvIniciarSesion)
        btnRegistrar = findViewById(R.id.btnRegistrar)


        tvIniciarSesion.setOnClickListener {
            startActivity(Intent(this, iniciar_sesion::class.java))
        }

        // Registro de usuario
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etContraseña.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
                mostrarAlerta("Campos incompletos", "Por favor, completa todos los campos.")
                return@setOnClickListener
            }

            if (password.length < 6) {
                mostrarAlerta("Contraseña inválida", "La contraseña debe tener al menos 6 caracteres.")
                return@setOnClickListener
            }

            btnRegistrar.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val datosUsuario = mapOf(
                            "nombre" to nombre,
                            "email" to email
                        )

                        val db = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")
                        db.getReference("usuarios").child(userId!!)
                            .setValue(datosUsuario)
                            .addOnSuccessListener {
                                mostrarAlertaConAccion(
                                    "Registro exitoso",
                                    "El usuario se ha registrado correctamente."
                                ) {
                                    val intent = Intent(this, pantalla2::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                btnRegistrar.isEnabled = true
                                mostrarAlerta("Error en Firebase", "No se pudieron guardar los datos: ${it.message}")
                            }
                    } else {
                        btnRegistrar.isEnabled = true
                        mostrarAlerta("Error de autenticación", task.exception?.message ?: "Error desconocido.")
                    }
                }
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK") { _, _ ->
                btnRegistrar.isEnabled = true
            }
            .show()
    }

    private fun mostrarAlertaConAccion(titulo: String, mensaje: String, accionOk: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                accionOk()
            }
            .show()
    }
}
