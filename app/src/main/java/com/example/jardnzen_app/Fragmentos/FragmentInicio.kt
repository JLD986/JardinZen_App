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

    // ViewBinding para acceder fácilmente a los elementos del layout XML
    private lateinit var binding: FragmentInicioBinding

    // Para la autenticación del usuario
    private lateinit var auth: FirebaseAuth

    // Referencia a la base de datos de usuarios
    private lateinit var databaseUsuarios: DatabaseReference
    // Referencia a la base de datos de sensores
    private lateinit var databaseSensores: DatabaseReference



    // Adaptador del RecyclerView
    private lateinit var adapter: PlantaAdapter

    // Lista que contendrá las plantas a mostrar
    private val listaPlantas = mutableListOf<Planta>()

    // ID del dispositivo en Firebase (ESP32)
    private val deviceId = "JardinZenESP32"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInicioBinding.inflate(inflater, container, false)

        // Inicializa Firebase Authentication y la referencia a los usuarios
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

        // Ruta completa hacia los sensores del usuario y su dispositivo
        databaseSensores = FirebaseDatabase.getInstance()
            .getReference("Usuarios")
            .child(uid)
            .child("dispositivos")
            .child(deviceId)
            .child("sensores")

        // Escucha en tiempo real los cambios en los valores de los sensores

        databaseSensores.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Limpia la lista anterior de plantas, pero como solo tenemos 1

                listaPlantas.clear()

                // Obtiene los valores de cada sensor desde Firebase

                val temperatura = snapshot.child("temperatura").getValue(Double::class.java)
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java)
                val luz = snapshot.child("luz").getValue(Int::class.java)
                val agua = snapshot.child("nivel_pct").getValue(Double::class.java)

                // Crea una instancia del modelo Planta con los datos actuales

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
            // Si ocurre un error al leer datos desde Firebase
            override fun onCancelled(error: DatabaseError) {
                println(" Error en Firebase: ${error.message}")
            }
        })
    }
}
