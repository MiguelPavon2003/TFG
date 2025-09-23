package com.example.miguelpavonlimones_tfg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

data class SelJugador(
    val key: String,
    val nombre: String,
    val apellido: String,
    val dorsal: Int,
    var selected: Boolean = false
)

class SelectJugadorAdapter(
    private val datos: MutableList<SelJugador>,
    private val maxSeleccion: Int,
    private val onSelectionChanged: (selectedCount: Int) -> Unit
) : RecyclerView.Adapter<SelectJugadorAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cb: CheckBox = v.findViewById(R.id.cbSeleccion)
        val tvNombre: TextView = v.findViewById(R.id.tvNombre)
        val tvDorsal: TextView = v.findViewById(R.id.tvDorsal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_jugador, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val j = datos[position]
        holder.tvNombre.text = "${j.nombre} ${j.apellido}"
        holder.tvDorsal.text = "#${j.dorsal}"
        // Evitar “rebotes” de listener
        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.isChecked = j.selected

        holder.cb.setOnCheckedChangeListener { button, isChecked ->
            val selectedCount = datos.count { it.selected }
            if (isChecked) {
                // Intento marcar uno más
                if (selectedCount >= maxSeleccion) {
                    // Límite alcanzado
                    button.isChecked = false
                    Toast.makeText(
                        holder.itemView.context,
                        "Máximo $maxSeleccion convocados.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnCheckedChangeListener
                }
                j.selected = true
            } else {
                j.selected = false
            }
            onSelectionChanged(datos.count { it.selected })
        }

        // También permite pulsar toda la fila
        holder.itemView.setOnClickListener {
            holder.cb.performClick()
        }
    }

    override fun getItemCount(): Int = datos.size

    fun getSeleccionados(): List<SelJugador> = datos.filter { it.selected }
}
