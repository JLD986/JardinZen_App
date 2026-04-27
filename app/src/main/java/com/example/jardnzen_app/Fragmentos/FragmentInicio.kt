package com.example.jardnzen_app.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jardnzen_app.Adaptadores.PlantaAdapter
import com.example.jardnzen_app.Modelos.Planta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.databinding.FragmentInicioBinding

import android.app.AlertDialog
import android.widget.EditText
import android.widget.Toast

class FragmentInicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseUsuarios: DatabaseReference
    private lateinit var databasePlantas: DatabaseReference
    private lateinit var databaseSensores: DatabaseReference

    private lateinit var adapter: PlantaAdapter
    private val listaPlantas = mutableListOf<Planta>()
    private val deviceId = "JardinZenESP32"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return binding.root
        databaseUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios")
        databasePlantas = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid).child("plantas")

        binding.recyclerJardin.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlantaAdapter(listaPlantas)
        binding.recyclerJardin.adapter = adapter

        obtenerNombreUsuario(uid)
        cargarPlantas(uid)

        binding.btnAgregarPlanta.setOnClickListener { mostrarDialogoAgregarPlanta(uid) }
        binding.btnBorrarPlanta.setOnClickListener { mostrarDialogoBorrarPlanta() }

        return binding.root
    }

    private fun obtenerNombreUsuario(uid: String) {
        databaseUsuarios.child(uid).child("nombre").get().addOnSuccessListener { snapshot ->
            val nombre = snapshot.value?.toString()
            binding.holaUsuario.text = if (!nombre.isNullOrEmpty()) "Hola $nombre" else "Hola ????"
        }
    }

    private fun cargarPlantas(uid: String) {
        databasePlantas.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaPlantas.clear()
                for (plantaSnapshot in snapshot.children) {
                    val planta = plantaSnapshot.getValue(Planta::class.java)
                    planta?.let {
                        it.id = plantaSnapshot.key ?: ""
                        listaPlantas.add(it)
                    }
                }
                
                if (listaPlantas.isEmpty()) {
                    // Si no hay plantas, podríamos cargar la de defecto o dejarlo vacío
                    // El usuario pidió que aparezcan las disponibles en firebase
                }
                
                // Si hay plantas, vamos a vincular los sensores a la primera planta por ahora 
                // (o a la que coincida con el deviceId si tuviéramos esa lógica)
                actualizarSensoresEnTiempoReal(uid)
                
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error al cargar plantas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun actualizarSensoresEnTiempoReal(uid: String) {
        databaseSensores = FirebaseDatabase.getInstance()
            .getReference("Usuarios")
            .child(uid)
            .child("dispositivos")
            .child(deviceId)
            .child("sensores")

        databaseSensores.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (listaPlantas.isEmpty()) return

                val temperatura = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java) ?: 0
                val luz = snapshot.child("luz").getValue(Int::class.java) ?: 0
                val agua = snapshot.child("nivel_pct").getValue(Double::class.java) ?: 0.0

                // Por simplicidad, actualizamos la primera planta con los sensores reales del ESP32
                val primeraPlanta = listaPlantas[0]
                val plantaActualizada = primeraPlanta.copy(
                    temperatura = "$temperatura °C",
                    humedad = "$humedad %",
                    luz = "$luz %",
                    agua = "$agua %"
                )
                listaPlantas[0] = plantaActualizada
                adapter.notifyItemChanged(0)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarDialogoAgregarPlanta(uid: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Agregar nueva planta")
        
        val input = EditText(requireContext())
        input.hint = "Nombre de la planta"
        builder.setView(input)

        builder.setPositiveButton("Agregar") { _, _ ->
            val nombre = input.text.toString()
            if (nombre.isNotEmpty()) {
                val nuevaPlanta = Planta(nombre = nombre)
                databasePlantas.push().setValue(nuevaPlanta)
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoBorrarPlanta() {
        if (listaPlantas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay plantas para borrar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombresPlantas = listaPlantas.map { it.nombre }.toTypedArray()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona planta a borrar")
        builder.setItems(nombresPlantas) { _, which ->
            val plantaId = listaPlantas[which].id
            databasePlantas.child(plantaId).removeValue().addOnSuccessListener {
                Toast.makeText(requireContext(), "Planta eliminada", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
