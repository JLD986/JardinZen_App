package com.example.jardnzen_app

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.locochones.jardnzen_app.MainActivity


import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Instancia de FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Registrando Usuario")
        progressDialog.setCanceledOnTouchOutside(false)

        //Evento del botón de registro
        binding.btnRegistrar.setOnClickListener {
            validarDatos()
        }

        binding.btnIniciosesion.setOnClickListener {
            startActivity(Intent(this, InicioSesionActivity::class.java))
            finish()
        }

    }


    // Variables del registro
    private var nombre = ""
    private var correo = ""
    private var contrasena = ""


    private fun validarDatos() {
        nombre = binding.etNombreusuario.text.toString().trim()
        correo = binding.etCorreo.text.toString().trim()
        contrasena = binding.etContrasena.text.toString().trim()


        // Validaciones
        if (nombre.isEmpty()) {
            binding.etNombreusuario.error = "Ingrese su nombre"
            binding.etNombreusuario.requestFocus()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.etCorreo.error = "Ingrese un correo válido"
            binding.etCorreo.requestFocus()
        } else if (correo.isEmpty()) {
            binding.etCorreo.error = "Ingrese un correo"
            binding.etCorreo.requestFocus()
        } else if (contrasena.isEmpty()) {
            binding.etContrasena.error = "Ingrese una contraseña"
            binding.etContrasena.requestFocus()
        } else {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando cuenta...")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnSuccessListener {
                actualizarInformacion()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "No se pudo crear el usuario: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun actualizarInformacion() {
        progressDialog.setMessage("Guardando información del usuario...")

        val uid = firebaseAuth.uid
        val correoU = firebaseAuth.currentUser?.email ?: ""
        val nombreU = nombre
        val fechaRegistro = System.currentTimeMillis()

        // Crear mapa de datos para subir a Firebase Realtime Database
        val datosUsuario = HashMap<String, Any>()
        datosUsuario["uid"] = uid!!
        datosUsuario["nombre"] = nombreU
        datosUsuario["correo"] = correoU
        datosUsuario["fechaRegistro"] = fechaRegistro
        datosUsuario["proveedor"] = "Email"
        datosUsuario["online"] = true

        val reference = FirebaseDatabase.getInstance().getReference("Usuarios")
        reference.child(uid)
            .setValue(datosUsuario)
            .addOnSuccessListener {
                progressDialog.dismiss()

                val session = SessionManager(this)
                session.saveCredentials(correo, contrasena)


                Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, VincularDispositivoActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Error al guardar la información: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


}
