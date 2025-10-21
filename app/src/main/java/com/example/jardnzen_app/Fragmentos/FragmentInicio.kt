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

class FragmentInicio : Fragment() {

    private lateinit var binding: FragmentInicioBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseUsuarios: DatabaseReference
    private lateinit var databaseSensores: DatabaseReference
    private lateinit var adapter: PlantaAdapter
    private val listaPlantas = mutableListOf<Planta>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInicioBinding.inflate(inflater, container, false)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        databaseUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios")
        databaseSensores = FirebaseDatabase.getInstance().getReference("sensores")

        // Configurar RecyclerView
        binding.recyclerJardin.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlantaAdapter(listaPlantas)
        binding.recyclerJardin.adapter = adapter

        // Mostrar nombre del usuario
        val user = auth.currentUser
        if (user != null) {
            obtenerNombreUsuario(user.uid)
        } else {
            binding.holaUsuario.text = "Hola ????"
        }

        // Cargar datos de sensores
        obtenerDatosSensores()

        return binding.root
    }

    private fun obtenerNombreUsuario(uid: String) {
        databaseUsuarios.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val nombre = snapshot.child("nombre").value?.toString()
                if (!nombre.isNullOrEmpty()) {
                    binding.holaUsuario.text = "Hola $nombre"
                } else {
                    binding.holaUsuario.text = "Hola ????"
                }
            } else {
                binding.holaUsuario.text = "Hola ????"
            }
        }.addOnFailureListener {
            binding.holaUsuario.text = "Hola ????"
        }
    }

    private fun obtenerDatosSensores() {
        databaseSensores.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaPlantas.clear()

                val temperatura = snapshot.child("temperatura").getValue(Double::class.java)
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java)
                val luz = snapshot.child("luz").getValue(Int::class.java)
                val agua = snapshot.child("distancia_agua").getValue(Double::class.java)

                val planta = Planta(
                    nombre = "Mi planta 🌱",
                    temperatura = "${temperatura ?: 0.0} °C",
                    humedad = "${humedad ?: 0} %",
                    luz = "${luz ?: 0} %",
                    agua = "${agua ?: 0.0} cm",
                    imagenUrl = "" // puedes agregar una URL si quieres mostrar imagen
                )

                listaPlantas.add(planta)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ Error Firebase: ${error.message}")
            }
        })
    }
}
