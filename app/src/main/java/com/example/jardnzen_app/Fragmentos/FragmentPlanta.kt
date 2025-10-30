package com.example.jardnzen_app.Fragmentos

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.jardnzen_app.Modelos.Planta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.FragmentPlantaBinding
import java.text.SimpleDateFormat
import java.util.*

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

        configurarDatosPlanta()
        configurarBotones()
        cargarDatosTiempoReal()
    }

    private fun configurarDatosPlanta() {
        planta?.let {
            binding.nombrePlanta.text = it.nombre
            binding.valorTemperatura.text = it.temperatura
            binding.valorHumedadSuelo.text = it.humedad
            binding.valorLuz.text = it.luz
            binding.valorAgua.text = it.agua

            configurarEstadoPlanta(it)
        }
    }

    private fun configurarEstadoPlanta(planta: Planta) {
        val humedad = extraerValorNumerico(planta.humedad)
        val temperatura = extraerValorNumerico(planta.temperatura)

        val estado = when {
            humedad < 30 -> "Necesita riego 💧"
            temperatura > 35 -> "Calor extremo 🔥"
            temperatura < 10 -> "Frío extremo ❄️"
            else -> "Saludable ✅"
        }

        binding.estadoPlanta.text = "Estado: $estado"

        when {
            humedad < 30 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FF6B6B"))
            temperatura > 35 || temperatura < 10 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FFA726"))
            else -> binding.estadoPlanta.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    private fun extraerValorNumerico(texto: String): Int {
        return texto.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
    }

    private fun configurarBotones() {
        binding.btnRegar.setOnClickListener {
            activarRiegoManual()
        }

        binding.btnDetenerRiego.setOnClickListener {
            detenerRiegoManual()
        }

        actualizarEstadoBotones()
    }

    private fun activarRiegoManual() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val controlRef = database.child(uid).child("dispositivos").child(deviceId).child("control")
        val fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val updates = mapOf(
            "riego_manual" to true,
            "ultimo_riego" to fechaHora
        )

        controlRef.updateChildren(updates)
            .addOnSuccessListener {
                binding.riegoStatus.text = "Riego manual 'ACTIVADO'"
                binding.riegoStatus.setTextColor(Color.parseColor("#4CAF50"))
                binding.ultimoRiego.text = "Último riego: $fechaHora"
                actualizarEstadoBotones()
            }
            .addOnFailureListener {
                binding.riegoStatus.text = "Error activando riego"
                binding.riegoStatus.setTextColor(Color.parseColor("#FF6B6B"))
            }
    }

    private fun detenerRiegoManual() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val controlRef = database.child(uid).child("dispositivos").child(deviceId).child("control")

        controlRef.child("riego_manual").setValue(false)
            .addOnSuccessListener {
                binding.riegoStatus.text = "Riego manual 'DESACTIVADO'"
                binding.riegoStatus.setTextColor(Color.parseColor("#FFA726"))
                actualizarEstadoBotones()
            }
            .addOnFailureListener {
                binding.riegoStatus.text = "Error deteniendo riego"
                binding.riegoStatus.setTextColor(Color.parseColor("#FF6B6B"))
            }
    }

    private fun actualizarEstadoBotones() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        database.child(uid).child("modoRiego").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val modo = snapshot.getValue(String::class.java) ?: "Automatico"
                val esManual = modo.equals("Manual", ignoreCase = true)

                binding.btnRegar.isEnabled = esManual
                binding.btnDetenerRiego.isEnabled = esManual
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun cargarDatosTiempoReal() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Leer sensores
        val sensoresRef = database.child(uid).child("dispositivos").child(deviceId).child("sensores")
        sensoresRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val temperatura = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java) ?: 0
                val luz = snapshot.child("luz").getValue(Int::class.java) ?: 0
                val agua = snapshot.child("nivel_agua").getValue(Double::class.java) ?: 0.0

                binding.valorTemperatura.text = "$temperatura °C"
                binding.valorHumedadSuelo.text = "$humedad%"
                binding.valorLuz.text = "$luz%"
                binding.valorAgua.text = "$agua cm"

                val plantaActualizada = Planta(
                    nombre = planta?.nombre ?: "Mi planta 🌱",
                    temperatura = "$temperatura °C",
                    humedad = "$humedad%",
                    luz = "$luz%",
                    agua = "$agua cm",
                    imagenUrl = ""
                )
                configurarEstadoPlanta(plantaActualizada)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Leer control y último riego
        val controlRef = database.child(uid).child("dispositivos").child(deviceId).child("control")
        controlRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val riegoManual = snapshot.child("riego_manual").getValue(Boolean::class.java) ?: false
                val ultimoRiego = snapshot.child("ultimo_riego").getValue(String::class.java) ?: ""

                if (ultimoRiego.isNotEmpty()) {
                    binding.ultimoRiego.text = "Último riego: $ultimoRiego"
                }

                actualizarEstadoRiego(uid, riegoManual)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Leer modoRiego
        val modoRef = database.child(uid).child("modoRiego")
        modoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val modo = snapshot.getValue(String::class.java) ?: "Automatico"
                val esManual = modo.equals("Manual", ignoreCase = true)
                binding.btnRegar.isEnabled = esManual
                binding.btnDetenerRiego.isEnabled = esManual
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun actualizarEstadoRiego(uid: String, riegoManual: Boolean) {
        val estadoRef = database.child(uid).child("dispositivos").child(deviceId).child("estado")
        estadoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val regando = snapshot.child("regando").getValue(Boolean::class.java) ?: false

                database.child(uid).child("modoRiego").get().addOnSuccessListener { modoSnap ->
                    val modo = modoSnap.getValue(String::class.java) ?: "Automatico"
                    val esManual = modo.equals("Manual", ignoreCase = true)

                    if (esManual && riegoManual) {
                        binding.riegoStatus.text = "Riego manual 'ACTIVADO'"
                        binding.riegoStatus.setTextColor(Color.parseColor("#4CAF50"))
                    } else if (!esManual && regando) {
                        binding.riegoStatus.text = "Riego automático 'ACTIVADO'"
                        binding.riegoStatus.setTextColor(Color.parseColor("#4CAF50"))
                    } else {
                        binding.riegoStatus.text = if (esManual) "Riego manual listo" else "Riego automático listo"
                        binding.riegoStatus.setTextColor(Color.parseColor("#757575"))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
