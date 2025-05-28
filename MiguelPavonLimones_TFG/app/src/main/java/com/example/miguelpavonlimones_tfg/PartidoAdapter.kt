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
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvRival: TextView = view.findViewById(R.id.tvRival)
        val tvTipo: TextView = view.findViewById(R.id.tvTipo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartidoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_partido, parent, false)
        return PartidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartidoViewHolder, position: Int) {
        val partido = lista[position]
        holder.tvNombreEquipo.text = partido.nombreEquipo ?: "Sin equipo"
        holder.tvFecha.text = partido.fecha
        holder.tvRival.text = partido.rival
        holder.tvTipo.text = partido.tipo

        holder.view.setOnClickListener {
            onItemClick(partido)
        }
    }

    override fun getItemCount(): Int = lista.size
}

