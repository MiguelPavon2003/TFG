package com.example.miguelpavonlimones_tfg

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class RegistrarPartidoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_partido)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://miguelpavonlimones-tfg-default-rtdb.europe-west1.firebasedatabase.app/")

        val etFecha = findViewById<EditText>(R.id.etFecha)
        val etRival = findViewById<EditText>(R.id.etRival)
        val spinnerTipo = findViewById<Spinner>(R.id.spinnerTipo)
        val etJornada = findViewById<EditText>(R.id.etJornada)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupLocalVisitante)
        val radioLocal = findViewById<RadioButton>(R.id.radioLocal)
        val radioVisitante = findViewById<RadioButton>(R.id.radioVisitante)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPartido)

        val tipos = listOf("Pretemporada", "Liga", "Cuartos", "Semifinales", "Final")
        spinnerTipo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tipos)


        spinnerTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val tipoSeleccionado = tipos[position]
                etJornada.visibility = if (tipoSeleccionado == "Liga") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        etFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    etFecha.setText(String.format("%02d/%02d/%d", day, month + 1, year))
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnGuardar.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val fecha = etFecha.text.toString().trim()
            val rival = etRival.text.toString().trim()
            val tipo = spinnerTipo.selectedItem.toString()
            val equipo = intent.getStringExtra("nombreEquipo") ?: "Equipo desconocido"
            val esLocal = radioLocal.isChecked
            val jornada = etJornada.text.toString().trim()

            if (fecha.isEmpty() || rival.isEmpty()) {
                mostrarAlerta("Campos incompletos", "Completa todos los campos.")
                return@setOnClickListener
            }

            val partido = mutableMapOf<String, Any>(
                "usuarioId" to userId,
                "nombreEquipo" to equipo,
                "fecha" to fecha,
                "rival" to rival,
                "tipo" to tipo,
                "local" to esLocal
            )

            if (tipo == "Liga" && jornada.isNotEmpty()) {
                partido["jornada"] = jornada
            }

            db.getReference("partidos").push().setValue(partido)
                .addOnSuccessListener {
                    mostrarAlertaConAccion("Éxito", "Partido guardado correctamente.") {
                        val intent = Intent(this, pantalla2::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener {
                    mostrarAlerta("Error", "Error al guardar partido: ${it.message}")
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

    private fun mostrarAlertaConAccion(titulo: String, mensaje: String, accionOk: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> accionOk() }
            .show()
    }
}
