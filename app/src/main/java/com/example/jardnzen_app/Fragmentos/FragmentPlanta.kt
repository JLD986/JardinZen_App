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
import com.locochones.jardnzen_app.databinding.FragmentPlantaBinding
import java.text.SimpleDateFormat
import java.util.*

class FragmentPlanta : Fragment() {

    // --- Binding para acceder a los elementos del layout ---
    private lateinit var binding: FragmentPlantaBinding

    // --- Firebase ---
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // --- ID del dispositivo vinculado ---
    private val deviceId = "JardinZenESP32"

    // --- Planta seleccionada (recibida desde otro fragment) ---
    private var planta: Planta? = null

    // --- Clave para pasar datos entre fragments ---
    companion object {
        private const val ARG_PLANTA = "planta"

        // Método para crear una nueva instancia del fragment con una planta específica
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
        // Recuperar la planta enviada como argumento
        arguments?.let {
            planta = it.getSerializable(ARG_PLANTA) as? Planta
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el layout y vincularlo
        binding = FragmentPlantaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Usuarios")

        // Configurar datos iniciales de la planta
        configurarDatosPlanta()

        // Configurar botones de riego
        configurarBotones()

        // Cargar datos en tiempo real desde Firebase
        cargarDatosTiempoReal()
    }

    // --- Muestra los datos básicos de la planta seleccionada ---
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

    // --- Evalúa el estado de la planta según sus valores ---
    private fun configurarEstadoPlanta(planta: Planta) {
        val humedad = planta.humedad.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
        val temperatura = planta.temperatura.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
        val luz = planta.luz.replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0

        // Determina el estado general según los valores, es mas facil poner emojis que usar Iconos xd
        val estado = when {
            humedad < 30 -> "Necesita riego 💧"
            temperatura > 35 -> "Calor extremo 🔥"
            temperatura < 10 -> "Frío extremo ❄️"
            luz > 70 -> "Mucha luz, mover a otro lugar ☀️"
            else -> "Saludable ✅"
        }

        // Muestra el estado en pantalla
        binding.estadoPlanta.text = "Estado: $estado"

        // Colorea el texto según la condición
        when {
            humedad < 30 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FF6B6B")) // rojo
            temperatura > 35 || temperatura < 10 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FFA726")) // naranja
            luz > 70 -> binding.estadoPlanta.setTextColor(Color.parseColor("#FFB300")) // amarillo oscuro
            else -> binding.estadoPlanta.setTextColor(Color.parseColor("#4CAF50")) // verde
        }
    }

    // --- Configura los botones de control de riego ---
    private fun configurarBotones() {
        binding.btnRegar.setOnClickListener {
            activarRiegoManual()
        }

        binding.btnDetenerRiego.setOnClickListener {
            detenerRiegoManual()
        }

        // Valida si los botones deben estar activos o no según el modo de riego
        actualizarEstadoBotones()
    }

    // --- Envía señal para activar riego manual ---
    private fun activarRiegoManual() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // Referencia al nodo de control
        val controlRef = database.child(uid).child("dispositivos").child(deviceId).child("control")
        val fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // Actualiza en Firebase el estado del riego manual
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

    // --- Envía señal para detener el riego manual ---
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

    // --- Activa o desactiva botones según el modo de riego (Manual / Automático) ---
    private fun actualizarEstadoBotones() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        database.child(uid).child("modoRiego").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val modo = snapshot.getValue(String::class.java) ?: "Automatico"
                val esManual = modo.equals("Manual", ignoreCase = true)

                // Solo habilita los botones si está en modo manual
                binding.btnRegar.isEnabled = esManual
                binding.btnDetenerRiego.isEnabled = esManual
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- Carga datos en tiempo real de sensores, control y modo de riego ---
    private fun cargarDatosTiempoReal() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        // --- Escucha los valores de los sensores ---
        val sensoresRef = database.child(uid).child("dispositivos").child(deviceId).child("sensores")
        sensoresRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                // Obtiene los valores de Firebase
                val temperatura = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java) ?: 0
                val luz = snapshot.child("luz").getValue(Int::class.java) ?: 0
                val agua = snapshot.child("nivel_pct").getValue(Double::class.java) ?: 0.0

                // Muestra los valores en pantalla
                binding.valorTemperatura.text = "$temperatura °C"
                binding.valorHumedadSuelo.text = "$humedad%"
                binding.valorLuz.text = "$luz%"
                binding.valorAgua.text = "$agua %"

                // Actualiza el estado visual de la planta
                val plantaActualizada = Planta(
                    nombre = planta?.nombre ?: "Mi planta 🌱",
                    temperatura = "$temperatura °C",
                    humedad = "$humedad%",
                    luz = "$luz%",
                    agua = "$agua %",
                    imagenUrl = ""
                )
                configurarEstadoPlanta(plantaActualizada)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // --- Escucha el estado del control (riego y último riego) ---
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

        // --- Escucha el modo de riego en tiempo real ---
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

    // --- Actualiza visualmente el estado del riego (manual o automático) ---
    private fun actualizarEstadoRiego(uid: String, riegoManual: Boolean) {
        val estadoRef = database.child(uid).child("dispositivos").child(deviceId).child("estado")
        estadoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val regando = snapshot.child("regando").getValue(Boolean::class.java) ?: false

                database.child(uid).child("modoRiego").get().addOnSuccessListener { modoSnap ->
                    val modo = modoSnap.getValue(String::class.java) ?: "Automatico"
                    val esManual = modo.equals("Manual", ignoreCase = true)

                    // Cambia el texto según el estado
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
