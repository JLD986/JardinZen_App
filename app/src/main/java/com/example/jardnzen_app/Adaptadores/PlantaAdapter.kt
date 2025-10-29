package com.example.jardnzen_app.Adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.jardnzen_app.Fragmentos.FragmentPlanta
import com.example.jardnzen_app.Modelos.Planta
import com.locochones.jardnzen_app.R

class PlantaAdapter(
    private val listaPlantas: List<Planta>
) : RecyclerView.Adapter<PlantaAdapter.PlantaViewHolder>() {

    inner class PlantaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagenPlanta: ImageView = view.findViewById(R.id.imagen_planta)
        val nombrePlanta: TextView = view.findViewById(R.id.nombre_planta)
        val datoTemperatura: TextView = view.findViewById(R.id.dato_temperatura)
        val datoHumedad: TextView = view.findViewById(R.id.dato_humedad)
        val datoLuz: TextView = view.findViewById(R.id.dato_luz)
        val datoAgua: TextView = view.findViewById(R.id.dato_agua)
        val botonVerMas: Button = view.findViewById(R.id.ver_mas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.target_planta, parent, false)
        return PlantaViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantaViewHolder, position: Int) {
        val planta = listaPlantas[position]

        holder.nombrePlanta.text = planta.nombre
        holder.datoTemperatura.text = planta.temperatura
        holder.datoHumedad.text = planta.humedad
        holder.datoLuz.text = planta.luz
        holder.datoAgua.text = planta.agua

        // Cargar imagen con Glide
        if (planta.imagenUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(planta.imagenUrl)
                .placeholder(R.drawable.planta)
                .into(holder.imagenPlanta)
        } else {
            holder.imagenPlanta.setImageResource(R.drawable.hortensia)
        }

        // Configuraracion de el clic del botón "Ver más"
        holder.botonVerMas.setOnClickListener {
            navegarAFragmentPlanta(holder.itemView.context as FragmentActivity, planta)
        }

    }

    private fun navegarAFragmentPlanta(activity: FragmentActivity, planta: Planta) {
        // Crear instancia del FragmentPlanta con los datos de la planta
        val fragmentPlanta = FragmentPlanta.newInstance(planta)

        // Aca realizamos el cambio entre fragmentos usando fragmentoFL
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentoFL, fragmentPlanta)
            .addToBackStack("inicio") // para devolvernos al fragmento anterior
            .commit()
    }

    override fun getItemCount(): Int = listaPlantas.size
}