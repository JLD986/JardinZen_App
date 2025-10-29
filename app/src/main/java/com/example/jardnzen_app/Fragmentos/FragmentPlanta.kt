package com.example.jardnzen_app.Fragmentos

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.jardnzen_app.Modelos.Planta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.FragmentPlantaBinding

class FragmentPlanta : Fragment() {

    private lateinit var binding: FragmentPlantaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val deviceId = "JardinZenESP32"
    private var planta: Planta? = null

    companion object {
        private const val ARG_PLANTA = "planta"

        fun newInstance(planta: Planta): FragmentPlanta {
            val fragment = FragmentPlanta()
            val args = Bundle()
            args.putSerializable(ARG_PLANTA, planta)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            planta = it.getSerializable(ARG_PLANTA) as? Planta
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlantaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Usuarios")

        // Configurar datos de la planta
        configurarDatosPlanta()

        // Configurar listeners de botones
        configurarBotones()

        // Cargar datos en tiempo real desde Firebase
        cargarDatosTiempoReal()
    }

    private fun configurarDatosPlanta() {
        planta?.let {
            binding.nombrePlanta.text = it.nombre
            binding.valorTemperatura.text = it.temperatura
            binding.valorHumedadSuelo.text = it.humedad
            binding.valorLuz.text = it.luz
            binding.valorAgua.text = it.agua

            // Configurar estado según los valores
            configurarEstadoPlanta(it)
        }
    }

    private fun configurarEstadoPlanta(planta: Planta) {
        // Extraer valores numéricos para determinar el estado
        val humedad = extraerValorNumerico(planta.humedad)
        val temperatura = extraerValorNumerico(planta.temperatura)

        val estado = when {
            humedad < 30 -> "Necesita riego 💧"
            temperatura > 35 -> "Calor extremo 🔥"
            temperatura < 10 -> "Frío extremo ❄️"
            else -> "Saludable ✅"
        }

        binding.estadoPlanta.text = "Estado: $estado"

        // Configurar color según estado
        when {
            humedad < 30 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FF6B6B"))
            temperatura > 35 || temperatura < 10 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FFA726"))
            else -> binding.estadoPlanta.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    private fun extraerValorNumerico(texto: String): Int {
        return try {
            texto.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun configurarBotones() {
        // Botón Regar
        binding.btnRegar.setOnClickListener {
            activarRiegoManual()
        }

        // Botón Detener Riego
        binding.btnDetenerRiego.setOnClickListener {
            detenerRiegoManual()
        }

        // Actualizar estado inicial de los botones
        actualizarEstadoBotones()
    }

    private fun activarRiegoManual() {
        val user = auth.currentUser
        user?.let { uid ->
            database.child(uid.uid).child("dispositivos").child(deviceId).child("control")
                .child("riego_manual").setValue(true)
                .addOnSuccessListener {
                    binding.riegoStatus.text = "Riego manual ACTIVADO "
                    binding.riegoStatus.setTextColor(Color.parseColor("#4CAF50"))
                    actualizarEstadoBotones()
                    actualizarUltimoRiego()
                }
                .addOnFailureListener {
                    binding.riegoStatus.text = "Error activando riego "
                    binding.riegoStatus.setTextColor(Color.parseColor("#FF6B6B"))
                }
        }
    }

    private fun detenerRiegoManual() {
        val user = auth.currentUser
        user?.let { uid ->
            database.child(uid.uid).child("dispositivos").child(deviceId).child("control")
                .child("riego_manual").setValue(false)
                .addOnSuccessListener {
                    binding.riegoStatus.text = "Riego manual DESACTIVADO "
                    binding.riegoStatus.setTextColor(Color.parseColor("#FFA726"))
                    actualizarEstadoBotones()
                }
                .addOnFailureListener {
                    binding.riegoStatus.text = "Error deteniendo riego "
                    binding.riegoStatus.setTextColor(Color.parseColor("#FF6B6B"))
                }
        }
    }

    private fun actualizarEstadoBotones() {
        // Esta función podría leer el estado actual desde Firebase
        // Por ahora, solo actualiza la visibilidad básica
        binding.btnRegar.isEnabled = true
        binding.btnDetenerRiego.isEnabled = true
    }

    private fun actualizarUltimoRiego() {
        val fechaHora = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        binding.ultimoRiego.text = "Último riego: $fechaHora"
    }

    private fun cargarDatosTiempoReal() {
        val user = auth.currentUser
        user?.let { uid ->
            val sensoresRef = database.child(uid.uid).child("dispositivos").child(deviceId).child("sensores")

            sensoresRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val temperatura = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                        val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java) ?: 0
                        val luz = snapshot.child("luz").getValue(Int::class.java) ?: 0
                        val agua = snapshot.child("nivel_agua").getValue(Double::class.java) ?: 0.0
                        val humedadAire = snapshot.child("humedad_aire").getValue(Double::class.java) ?: 0.0

                        // Actualizar UI con datos en tiempo real
                        binding.valorTemperatura.text = "$temperatura °C"
                        binding.valorHumedadSuelo.text = "$humedad%"
                        binding.valorLuz.text = "$luz%"
                        binding.valorAgua.text = "$agua cm"

                        // Actualizar estado
                        val plantaActualizada = Planta(
                            nombre = planta?.nombre ?: "Mi planta 🌱",
                            temperatura = "$temperatura °C",
                            humedad = "$humedad%",
                            luz = "$luz%",
                            agua = "$agua cm",
                            imagenUrl = ""
                        )
                        configurarEstadoPlanta(plantaActualizada)

                        // Verificar si está regando actualmente
                        verificarEstadoRiego(uid.uid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar error
                }
            })
        }
    }

    private fun verificarEstadoRiego(uid: String) {
        val estadoRef = database.child(uid).child("dispositivos").child(deviceId).child("estado")

        estadoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val regando = snapshot.child("regando").getValue(Boolean::class.java) ?: false

                if (regando) {
                    binding.riegoStatus.text = "Riego automático ACTIVADO "
                    binding.riegoStatus.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    binding.riegoStatus.text = "Riego automático listo "
                    binding.riegoStatus.setTextColor(Color.parseColor("#757575"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar error
            }
        })
    }
}