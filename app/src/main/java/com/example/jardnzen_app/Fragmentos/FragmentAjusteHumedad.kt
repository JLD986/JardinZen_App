package com.example.jardnzen_app.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.FragmentAjusteHumedadBinding

class FragmentAjusteHumedad : Fragment() {
    private var _binding: FragmentAjusteHumedadBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val deviceId = "JardinZenESP32"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAjusteHumedadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            .child("dispositivos").child(deviceId).child("configuracion")

        // Cargar valor actual desde Firebase
        database.child("humedad_minima").get().addOnSuccessListener { snapshot ->
            val value = snapshot.getValue(Int::class.java) ?: 45
            binding.sliderHumedad.value = value.toFloat()
            actualizarUI(value)
        }

        // Listener para el slider
        binding.sliderHumedad.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                actualizarUI(value.toInt())
            }
        }

        // Guardar cambios
        binding.btnGuardarCambios.setOnClickListener {
            val value = binding.sliderHumedad.value.toInt()
            database.child("humedad_minima").setValue(value).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarUI(humedadMin: Int) {
        binding.tvRiegoAutomaticoInfo.text = getString(R.string.riego_activado_si_baja, humedadMin)
        
        // Simular estado del suelo basado en un valor de ejemplo o lectura actual
        // Aquí podríamos traer la humedad actual para comparar
        val uid = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            .child("dispositivos").child(deviceId).child("sensores").child("humedad_suelo")
            .get().addOnSuccessListener { snapshot ->
                val humedadActual = snapshot.getValue(Int::class.java) ?: 0
                if (humedadActual < humedadMin) {
                    binding.tvEstadoSuelo.text = "Estado actual: SECO 🏜️"
                } else {
                    binding.tvEstadoSuelo.text = "Estado actual: HÚMEDO 💧"
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}