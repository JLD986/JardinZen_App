package com.example.jardnzen_app.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.databinding.FragmentAlertasBinding

class FragmentAlertas : Fragment() {
    private var _binding: FragmentAlertasBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val deviceId = "JardinZenESP32"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
            .child("dispositivos").child(deviceId).child("configuracion").child("alertas")

        cargarConfiguracionAlertas()

        // Listeners para los switches
        binding.switchAlertaAgua.setOnCheckedChangeListener { _, isChecked -> 
            database.child("agua_baja").setValue(isChecked) 
        }
        binding.switchAlertaHumedad.setOnCheckedChangeListener { _, isChecked -> 
            database.child("humedad_baja").setValue(isChecked) 
        }
        binding.switchAlertaLuz.setOnCheckedChangeListener { _, isChecked -> 
            database.child("exceso_luz").setValue(isChecked) 
        }
        binding.switchAlertaTemperatura.setOnCheckedChangeListener { _, isChecked -> 
            database.child("temperatura_extrema").setValue(isChecked) 
        }
        binding.switchAlertaMantenimiento.setOnCheckedChangeListener { _, isChecked -> 
            database.child("mantenimiento").setValue(isChecked) 
        }
    }

    private fun cargarConfiguracionAlertas() {
        database.get().addOnSuccessListener { snapshot ->
            binding.switchAlertaAgua.isChecked = snapshot.child("agua_baja").getValue(Boolean::class.java) ?: true
            binding.switchAlertaHumedad.isChecked = snapshot.child("humedad_baja").getValue(Boolean::class.java) ?: true
            binding.switchAlertaLuz.isChecked = snapshot.child("exceso_luz").getValue(Boolean::class.java) ?: false
            binding.switchAlertaTemperatura.isChecked = snapshot.child("temperatura_extrema").getValue(Boolean::class.java) ?: false
            binding.switchAlertaMantenimiento.isChecked = snapshot.child("mantenimiento").getValue(Boolean::class.java) ?: true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}