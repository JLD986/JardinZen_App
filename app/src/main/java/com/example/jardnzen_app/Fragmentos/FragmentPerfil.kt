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

        val user = auth.currentUser
        if (user != null) {
            correoUsuario.text = user.email ?: "Correo no disponible"
            val uid = user.uid
            obtenerDatosUsuario(uid)
            verificarDispositivoVinculado(uid)
            contarPlantasActivas(uid)
        } else {
            nombreUsuario.text = "No hay sesión activa"
            correoUsuario.text = ""
            infoDispositivo.text = "Sin conexión a Firebase"
        }

        // Configurar clics en las tarjetas
        setupClickListeners(view)

        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        return view
    }

    private fun obtenerDatosUsuario(uid: String) {
        database.child(uid).child("nombre")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val nombre = snapshot.getValue(String::class.java)
                        nombreUsuario.text = nombre ?: "Usuario"
                    } else {
                        nombreUsuario.text = "Usuario"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error al cargar nombre", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun verificarDispositivoVinculado(uid: String) {
        database.child(uid).child("dispositivoIoT")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.value.toString().isNotEmpty()) {
                        infoDispositivo.text = "Sí" // Dispositivo vinculado
                    } else {
                        infoDispositivo.text = "No" // Sin dispositivo
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    infoDispositivo.text = "Error"
                }
            })
    }

    private fun contarPlantasActivas(uid: String) {
        // Contar cuántas plantas están siendo monitoreadas (basado en los sensores activos)
        database.child(uid).child("dispositivos").child("JardinZenESP32").child("sensores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Si hay datos de sensores, asumimos que hay al menos 1 planta activa
                        plantasRegistradas.text = "1 planta"
                    } else {
                        plantasRegistradas.text = "0 plantas"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    plantasRegistradas.text = "0 plantas"
                }
            })
    }

    private fun setupClickListeners(view: View) {
        // Tarjeta de dispositivo IoT - Vincular nuevo dispositivo
        view.findViewById<View>(R.id.card_dispositivo).setOnClickListener {
            val intent = Intent(requireContext(), VincularDispositivoActivity::class.java)
            startActivity(intent)
        }

        // Tarjeta de notificaciones - Toggle simple
        view.findViewById<View>(R.id.card_notificaciones).setOnClickListener {
            toggleNotificaciones()
        }

        // Tarjeta de modo riego - Cambiar modo
        view.findViewById<View>(R.id.card_modo_riego).setOnClickListener {
            cambiarModoRiego()
        }
    }

    private fun toggleNotificaciones() {
        val user = auth.currentUser
        user?.let { uid ->
            database.child(uid.uid).child("notificaciones").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val estadoActual = snapshot.getValue(Boolean::class.java) ?: true
                    val nuevoEstado = !estadoActual

                    database.child(uid.uid).child("notificaciones").setValue(nuevoEstado)
                        .addOnSuccessListener {
                            notificacionesEstado.text = if (nuevoEstado) "Activadas ✅" else "Desactivadas ❌"
                            Toast.makeText(requireContext(),
                                if (nuevoEstado) "Notificaciones activadas" else "Notificaciones desactivadas",
                                Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error al cambiar notificaciones", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun cambiarModoRiego() {
        val user = auth.currentUser
        user?.let { uid ->
            database.child(uid.uid).child("modoRiego").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val modoActual = snapshot.getValue(String::class.java) ?: "Automático"
                    val nuevoModo = when (modoActual) {
                        "Automático" -> "Manual"
                        "Manual" -> "Programado"
                        else -> "Automático"
                    }

                    database.child(uid.uid).child("modoRiego").setValue(nuevoModo)
                        .addOnSuccessListener {
                            modoRiego.text = nuevoModo
                            Toast.makeText(requireContext(), "Modo de riego: $nuevoModo", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error al cambiar modo", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun cerrarSesion() {
        auth.signOut()
        val intent = Intent(requireContext(), InicioSesionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Actualizar datos cuando el fragment se vuelve visible
        auth.currentUser?.let {
            obtenerDatosUsuario(it.uid)
            verificarDispositivoVinculado(it.uid)
            contarPlantasActivas(it.uid)
        }
    }
}