import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.miguelpavonlimones_tfg.Partido
import com.example.miguelpavonlimones_tfg.R

class PartidoAdapter(
    private val lista: List<Partido>,
    private val onItemClick: (Partido) -> Unit
) : RecyclerView.Adapter<PartidoAdapter.PartidoViewHolder>() {

    class PartidoViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreEquipo: TextView = view.findViewById(R.id.tvNombreEquipo)
        val tvRival: TextView = view.findViewById(R.id.tvRival)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
        val tvLocalVisitante: TextView = view.findViewById(R.id.tvLocalVisitante)
        val tvJornada: TextView = view.findViewById(R.id.tvJornada)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartidoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_partido, parent, false)
        return PartidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartidoViewHolder, position: Int) {
        val partido = lista[position]

        holder.tvNombreEquipo.text = partido.nombreEquipo ?: "Equipo"
        holder.tvRival.text = "Rival: ${partido.rival}"
        holder.tvFecha.text = "Fecha: ${partido.fecha}"
        holder.tvTipo.text = "Tipo: ${partido.tipo}"
        holder.tvLocalVisitante.text = if (partido.local) "Juega como: Local" else "Juega como: Visitante"

        if (partido.tipo == "Liga" && !partido.jornada.isNullOrBlank()) {
            holder.tvJornada.visibility = View.VISIBLE
            holder.tvJornada.text = "Jornada: ${partido.jornada}"
        } else {
            holder.tvJornada.visibility = View.GONE
        }

        holder.view.setOnClickListener {
            onItemClick(partido)
        }
    }

    override fun getItemCount(): Int = lista.size
}


