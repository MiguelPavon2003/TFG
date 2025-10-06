package com.example.miguelpavonlimones_tfg

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.*

class Configuracion : AppCompatActivity() {

    // Bottom nav
    private lateinit var bottomNavigation: BottomNavigationView

    // UI
    private lateinit var tvNombreUsu: TextView
    private lateinit var tvGmailUsu: TextView
    private lateinit var btnEditarPerfil: MaterialButton
    private lateinit var btnCambiarPassword: MaterialButton
    private lateinit var btnCerrarSesion: MaterialButton
    private lateinit var btnEliminarCuenta: MaterialButton

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        // Referencias UI
        bottomNavigation = findViewById(R.id.bottomNavigation)
        tvNombreUsu = findViewById(R.id.tvNombreUsu)
        tvGmailUsu = findViewById(R.id.tvGmailUsu)
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil)
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta)

        // BottomNavigation
        bottomNavigation.selectedItemId = R.id.nav_configuracion
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_partidos -> {
                    if (this.javaClass != pantalla2::class.java) {
                        startActivity(Intent(this, pantalla2::class.java))
                        finish()
                    }
                    true
                }

                R.id.nav_jugadores -> {
                    if (this.javaClass != Jugadores::class.java) {
                        startActivity(Intent(this, Jugadores::class.java))
                        finish()
                    }
                    true
                }

                R.id.nav_configuracion -> true
                else -> false
            }
        }

        // Cargar datos de usuario (nombre y email)
        cargarDatosUsuario()

        // Acciones de botones
        btnEditarPerfil.setOnClickListener {
            mostrarInfo("Editar perfil", "Aquí abrirías tu pantalla para editar el perfil.")
        }

        btnCambiarPassword.setOnClickListener {
            val email = auth.currentUser?.email
            if (email.isNullOrBlank()) {
                mostrarInfo("Error", "No hay email asociado a esta cuenta.")
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        mostrarInfo("Listo", "Se ha enviado un correo para restablecer la contraseña a:\n$email")
                    }
                    .addOnFailureListener {
                        mostrarInfo("Error", it.message ?: "No se pudo enviar el correo.")
                    }
            }
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnEliminarCuenta.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Seguro que quieres eliminar tu cuenta y TODOS tus datos? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí") { _, _ -> eliminarCuentaYDatos() }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun cargarDatosUsuario() {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        tvGmailUsu.text = email

        val userRef = db.getReference("usuarios").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario"
                tvNombreUsu.text = nombre
            }

            override fun onCancelled(error: DatabaseError) {
                mostrarInfo("Error", "No se pudo cargar el usuario: ${error.message}")
            }
        })
    }

    /** Elimina todos los datos del usuario en Firebase y luego intenta borrar la cuenta. */
    private fun eliminarCuentaYDatos() {
        val user = auth.currentUser
        val uid = user?.uid

        if (user == null || uid == null) {
            mostrarInfo("Error", "No hay usuario autenticado.")
            return
        }

        setAccionesEnabled(false)

        val equiposRef = db.getReference("equipos")
        val partidosRef = db.getReference("partidos")

        equiposRef.orderByChild("usuarioId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(equiposSnap: DataSnapshot) {
                    val updates = hashMapOf<String, Any?>()

                    updates["usuarios/$uid"] = null
                    updates["jugadores/$uid"] = null

                    // Borrar todos los equipos del usuario
                    for (eq in equiposSnap.children) {
                        val equipoKey = eq.key ?: continue
                        updates["equipos/$equipoKey"] = null
                    }

                    partidosRef.orderByChild("usuarioId").equalTo(uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(partidosSnap: DataSnapshot) {
                                for (p in partidosSnap.children) {
                                    val partidoId = p.key ?: continue
                                    updates["partidos/$partidoId"] = null
                                    updates["estadisticas/$partidoId"] = null
                                    updates["convocados/$partidoId"] = null
                                    updates["partidosFinalizados/$partidoId"] = null
                                }

                                // Ejecutar borrado masivo
                                db.reference.updateChildren(updates).addOnCompleteListener { task ->
                                    if (!task.isSuccessful) {
                                        setAccionesEnabled(true)
                                        mostrarInfo(
                                            "Error",
                                            task.exception?.message ?: "No se pudieron eliminar los datos."
                                        )
                                        return@addOnCompleteListener
                                    }

                                    // Intentar eliminar la cuenta
                                    tryDeleteUser()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                setAccionesEnabled(true)
                                mostrarInfo("Error", "No se pudieron cargar los partidos: ${error.message}")
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    setAccionesEnabled(true)
                    mostrarInfo("Error", "No se pudieron cargar los equipos: ${error.message}")
                }
            })
    }

    /** Intenta borrar la cuenta; si requiere reautenticación, la solicita. */
    private fun tryDeleteUser() {
        val user = auth.currentUser ?: return
        user.delete().addOnCompleteListener { del ->
            setAccionesEnabled(true)
            if (del.isSuccessful) {
                startActivity(Intent(this@Configuracion, MainActivity::class.java))
                finish()
            } else {
                val ex = del.exception
                if (ex is FirebaseAuthRecentLoginRequiredException) {
                    pedirReautenticacionYEliminar()
                } else {
                    mostrarInfo(
                        "Datos borrados",
                        "Se borraron los datos, pero no la cuenta: ${ex?.message ?: ""}\n" +
                                "Inicia sesión de nuevo recientemente e inténtalo otra vez para borrar la cuenta."
                    )
                }
            }
        }
    }

    /** Pide contraseña y reautentica al usuario antes de eliminarlo. */
    private fun pedirReautenticacionYEliminar() {
        val email = auth.currentUser?.email ?: run {
            mostrarInfo("Reautenticación", "No hay email disponible para reautenticar.")
            return
        }

        val input = EditText(this).apply {
            hint = "Contraseña actual"
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar identidad")
            .setMessage("Introduce tu contraseña para borrar la cuenta de $email")
            .setView(input)
            .setPositiveButton("Confirmar") { _, _ ->
                val pwd = input.text?.toString()?.trim().orEmpty()
                if (pwd.isEmpty()) {
                    mostrarInfo("Reautenticación", "La contraseña no puede estar vacía.")
                    return@setPositiveButton
                }
                reautenticarConEmail(email, pwd)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Reautentica con email/contraseña y vuelve a intentar borrar. */
    private fun reautenticarConEmail(email: String, password: String) {
        val user = auth.currentUser ?: return
        val cred = EmailAuthProvider.getCredential(email, password)

        setAccionesEnabled(false)
        user.reauthenticate(cred)
            .addOnSuccessListener {
                tryDeleteUser()
            }
            .addOnFailureListener {
                setAccionesEnabled(true)
                mostrarInfo("Reautenticación fallida", it.message ?: "No se pudo reautenticar.")
            }
    }

    private fun setAccionesEnabled(enabled: Boolean) {
        btnEditarPerfil.isEnabled = enabled
        btnCambiarPassword.isEnabled = enabled
        btnCerrarSesion.isEnabled = enabled
        btnEliminarCuenta.isEnabled = enabled
    }

    private fun mostrarInfo(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }
}
