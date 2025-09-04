package com.example.miguelpavonlimones_tfg

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class JugadorAdapter(
    private val datos: List<Jugador>,
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<JugadorAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDorsal: TextView = itemView.findViewById(R.id.tvDorsal)
        val btnBorrar: ImageView = itemView.findViewById(R.id.btnBorrar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jugador, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val j = datos[position]
        holder.tvNombre.text = "${j.nombre} ${j.apellido}"
        holder.tvDorsal.text = "Dorsal: ${j.dorsal}"

        holder.btnBorrar.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClick(pos)
            }

        }
    }

    override fun getItemCount(): Int = datos.size
}
