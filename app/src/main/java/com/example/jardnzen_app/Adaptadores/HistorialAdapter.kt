package com.example.jardnzen_app.Adaptadores


// Importaciones necesarias para inflar vistas y manejar el RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.jardnzen_app.Modelos.RegistroHistorial
import com.locochones.jardnzen_app.R


// Adaptador del RecyclerView que mostrará los registros del historial
class HistorialAdapter(private val registros: List<RegistroHistorial>) :
    RecyclerView.Adapter<HistorialAdapter.HistorialViewHolder>() {

    // Clase interna que representa cada elemento (item) del RecyclerView
    class HistorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Referencias a los TextView del layout 'registro_historial.xml'
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
        val txtTemperatura: TextView = itemView.findViewById(R.id.txtTemperatura)
        val txtHumedad: TextView = itemView.findViewById(R.id.txtHumedad)
        val txtLuz: TextView = itemView.findViewById(R.id.txtLuz)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.registro_historial, parent, false)
        return HistorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
        val registro = registros[position]

        // Asigna los valores de fecha, temperatura, humedad y luz a los TextViews

        holder.txtFecha.text = registro.fecha
        holder.txtTemperatura.text = "${registro.temperatura}°C"
        holder.txtHumedad.text = "${registro.humedad}%"
        holder.txtLuz.text = "${registro.luz}%"
    }
    // Devuelve la cantidad total de elementos en la lista (número de registros)

    override fun getItemCount() = registros.size
}