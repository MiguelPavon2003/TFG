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

    private var locked: Boolean = false

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

        // Evitar rebotes
        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.isChecked = j.selected

        // Si está bloqueado, no se puede tocar
        holder.cb.isEnabled = !locked
        holder.itemView.isEnabled = !locked
        holder.itemView.alpha = if (locked) 0.6f else 1f

        holder.cb.setOnCheckedChangeListener { button, isChecked ->
            if (locked) {
                button.isChecked = j.selected
                return@setOnCheckedChangeListener
            }

            val selectedCount = datos.count { it.selected }

            if (isChecked) {
                if (selectedCount >= maxSeleccion) {
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

        // Pulsar fila = toggle checkbox
        holder.itemView.setOnClickListener {
            if (!locked) holder.cb.performClick()
        }
    }

    override fun getItemCount(): Int = datos.size

    fun getSeleccionados(): List<SelJugador> = datos.filter { it.selected }

    /** Llamar cuando se guarde para bloquear cambios */
    fun setLocked(value: Boolean) {
        locked = value
        notifyDataSetChanged()
    }

    fun isLocked(): Boolean = locked

    /** Útil para refrescar lista si recargas desde Firebase */
    fun setItems(nuevos: List<SelJugador>) {
        datos.clear()
        datos.addAll(nuevos)
        notifyDataSetChanged()
        onSelectionChanged(datos.count { it.selected })
    }
}
