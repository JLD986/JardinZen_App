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
    private val deviceId = "JardinZenESP32" // IP ESP32

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInicioBinding.inflate(inflater, container, false)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        databaseUsuarios = FirebaseDatabase.getInstance().getReference("Usuarios")

        // Configurar RecyclerView
        binding.recyclerJardin.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlantaAdapter(listaPlantas)
        binding.recyclerJardin.adapter = adapter

        // Aca se muestra el nombre del usuario y cargamos los datos de los sensores
        val user = auth.currentUser
        if (user != null) {
            obtenerNombreUsuario(user.uid)
            obtenerDatosSensores(user.uid)
        } else {
            binding.holaUsuario.text = "Hola ????"
        }

        return binding.root
    }

    private fun obtenerNombreUsuario(uid: String) {
        databaseUsuarios.child(uid).get().addOnSuccessListener { snapshot ->
            val nombre = snapshot.child("nombre").value?.toString()
            binding.holaUsuario.text = if (!nombre.isNullOrEmpty()) "Hola $nombre" else "Hola ????"
        }.addOnFailureListener {
            binding.holaUsuario.text = "Hola ????"
        }
    }

    private fun obtenerDatosSensores(uid: String) {
        databaseSensores = FirebaseDatabase.getInstance()
            .getReference("Usuarios")
            .child(uid)
            .child("dispositivos")
            .child(deviceId)
            .child("sensores")

        databaseSensores.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaPlantas.clear()

                val temperatura = snapshot.child("temperatura").getValue(Double::class.java)
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java)
                val luz = snapshot.child("luz").getValue(Int::class.java)
                val agua = snapshot.child("nivel_pct").getValue(Double::class.java)

                val planta = Planta(
                    nombre = " Hortensia 🌱",
                    temperatura = "${temperatura ?: 0.0} °C",
                    humedad = "${humedad ?: 0} %",
                    luz = "${luz ?: 0} %",
                    agua = "${agua ?: 0.0} %",
                    imagenUrl = ""
                )

                listaPlantas.add(planta)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                println(" Error en Firebase: ${error.message}")
            }
        })
    }
}
