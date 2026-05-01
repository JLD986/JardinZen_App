package com.example.jardnzen_app.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jardnzen_app.Modelos.PlantaAlmanaque
import com.locochones.jardnzen_app.R

class AlmanaqueAdapter(
    private var listaOriginal: List<PlantaAlmanaque>,
    private val onPlantaClick: (PlantaAlmanaque) -> Unit
) : RecyclerView.Adapter<AlmanaqueAdapter.AlmanaqueViewHolder>() {

    private var listaFiltrada = listaOriginal.toList()

    inner class AlmanaqueViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen = view.findViewById<ImageView>(R.id.iv_planta_almanaque)
        val nombre = view.findViewById<TextView>(R.id.tv_nombre_almanaque)
        val descripcion = view.findViewById<TextView>(R.id.tv_descripcion_almanaque)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlmanaqueViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_almanaque, parent, false)
        return AlmanaqueViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlmanaqueViewHolder, position: Int) {
        val planta = listaFiltrada[position]
        holder.nombre.text = planta.nombre_comun
        holder.descripcion.text = planta.descripcion

        Glide.with(holder.itemView.context)
            .load(planta.imagen_ref)
            .placeholder(R.drawable.planta)
            .into(holder.imagen)

        holder.itemView.setOnClickListener { onPlantaClick(planta) }
    }

    override fun getItemCount() = listaFiltrada.size

    fun actualizarLista(nuevaLista: List<PlantaAlmanaque>) {
        listaOriginal = nuevaLista
        listaFiltrada = nuevaLista.toList()
        notifyDataSetChanged()
    }

    fun filtrar(texto: String, onResult: (Boolean) -> Unit) {
        listaFiltrada = if (texto.isEmpty()) {
            listaOriginal
        } else {
            listaOriginal.filter { 
                it.nombre_comun.contains(texto, ignoreCase = true) || 
                it.descripcion.contains(texto, ignoreCase = true) 
            }
        }
        notifyDataSetChanged()
        onResult(listaFiltrada.isNotEmpty())
    }
}
