package com.example.jardnzen_app.Fragmentos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.jardnzen_app.InicioSesionActivity
import com.example.jardnzen_app.VincularDispositivoActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.R

class FragmentPerfil : Fragment() {
    // Variables de Firebase y vistas
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var nombreUsuario: TextView
    private lateinit var correoUsuario: TextView
    private lateinit var infoDispositivo: TextView
    private lateinit var plantasRegistradas: TextView
    private lateinit var notificacionesEstado: TextView
    private lateinit var modoRiego: TextView
    private lateinit var btnCerrarSesion: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Usuarios")

        // Referencias del layout
        nombreUsuario = view.findViewById(R.id.nombre_usuario)
        correoUsuario = view.findViewById(R.id.correo_usuario)
        infoDispositivo = view.findViewById(R.id.txt_dispositivo_iot_vinculado)
        plantasRegistradas = view.findViewById(R.id.txt_plantas_registradas)
        notificacionesEstado = view.findViewById(R.id.txt_notificaciones_estado)
        modoRiego = view.findViewById(R.id.txt_modo_riego)
        btnCerrarSesion = view.findViewById(R.id.btn_cerrar_sesion)

        // Si hay usuario autenticado, cargar su información
        val user = auth.currentUser
        if (user != null) {
            correoUsuario.text = user.email ?: "Correo no disponible"
            val uid = user.uid
            obtenerDatosUsuario(uid)
            verificarDispositivoVinculado(uid)
            contarPlantasActivas(uid)
            escucharModoRiego(uid)
        } else {
            // Si no hay sesión activa, mostrar mensaje por defecto
            nombreUsuario.text = "No hay sesión activa"
            correoUsuario.text = ""
            infoDispositivo.text = "Sin conexión a Firebase"
        }

        // Configurar clics en las tarjetas
        setupClickListeners(view)

        // Acción del botón "Cerrar sesión"
        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        return view
    }
    // --- Carga el nombre del usuario desde Firebase ---
    private fun obtenerDatosUsuario(uid: String) {
        database.child(uid).child("nombre")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    nombreUsuario.text = snapshot.getValue(String::class.java) ?: "Usuario"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error al cargar nombre", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // --- Verifica si el usuario tiene un dispositivo IoT vinculado ---
    private fun verificarDispositivoVinculado(uid: String) {
        database.child(uid).child("dispositivoIoT")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    infoDispositivo.text =
                        if (snapshot.exists() && snapshot.value.toString().isNotEmpty()) "Sí"
                        else "No"
                }

                override fun onCancelled(error: DatabaseError) {
                    infoDispositivo.text = "Error"
                }
            })
    }
    // --- Cuenta cuántas plantas activas tiene el usuario, pero en este caso solo podemos manejar 1 ---
    private fun contarPlantasActivas(uid: String) {
        database.child(uid).child("dispositivos").child("JardinZenESP32").child("sensores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    plantasRegistradas.text = if (snapshot.exists()) "1 planta" else "0 plantas"
                }

                override fun onCancelled(error: DatabaseError) {
                    plantasRegistradas.text = "0 plantas"
                }
            })
    }
    // --- Configura los clics de las tarjetas del perfil ---
    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.card_dispositivo).setOnClickListener {
            val intent = Intent(requireContext(), VincularDispositivoActivity::class.java)
            startActivity(intent)
        }
        // Tarjeta para activar/desactivar notificaciones, no tiene una funcion real

        view.findViewById<View>(R.id.card_notificaciones).setOnClickListener {
            toggleNotificaciones()
        }

        // Tarjeta para cambiar modo de riego, hay un boton en la seccion de configuracion, pero hemos trsladado la funcion aqui

        view.findViewById<View>(R.id.card_modo_riego).setOnClickListener {
            cambiarModoRiego()
        }
    }


    // --- Alterna el estado de las notificaciones (ON/OFF), solo seria de agregar la funcionalidad ---
    private fun toggleNotificaciones() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        database.child(uid).child("notificaciones")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estadoActual = snapshot.getValue(Boolean::class.java) ?: true
                    val nuevoEstado = !estadoActual

                    database.child(uid).child("notificaciones").setValue(nuevoEstado)
                        .addOnSuccessListener {
                            notificacionesEstado.text = if (nuevoEstado) "Activadas ✅" else "Desactivadas ❌"
                            Toast.makeText(
                                requireContext(),
                                if (nuevoEstado) "Notificaciones activadas" else "Notificaciones desactivadas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error al cambiar notificaciones", Toast.LENGTH_SHORT).show()
                }
            })
    }
    // --- Escucha los cambios en el modo de riego (Automático / Manual) ---
    private fun escucharModoRiego(uid: String) {
        database.child(uid).child("modoRiego")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val modo = snapshot.getValue(String::class.java) ?: "Automático"
                    modoRiego.text = modo
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
    // --- Cambia el modo de riego manualmente ---
    private fun cambiarModoRiego() {
        val user = auth.currentUser ?: return
        val uid = user.uid

        database.child(uid).child("modoRiego").get().addOnSuccessListener { snapshot ->
            val modoActual = snapshot.getValue(String::class.java) ?: "Automático"
            val nuevoModo = if (modoActual == "Automático") "Manual" else "Automático"

            database.child(uid).child("modoRiego").setValue(nuevoModo)
                .addOnSuccessListener {
                    modoRiego.text = nuevoModo
                    Toast.makeText(requireContext(), "Modo de riego: $nuevoModo", Toast.LENGTH_SHORT).show()
                }
        }
    }
    // --- Cierra la sesión del usuario ---
    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(requireContext(), InicioSesionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
    // --- Cuando se vuelve al fragmento, refresca los datos ---
    override fun onResume() {
        super.onResume()
        auth.currentUser?.let {
            obtenerDatosUsuario(it.uid)
            verificarDispositivoVinculado(it.uid)
            contarPlantasActivas(it.uid)
        }
    }
}
