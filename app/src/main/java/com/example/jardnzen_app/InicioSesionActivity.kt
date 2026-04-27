package com.example.jardnzen_app

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.locochones.jardnzen_app.MainActivity
import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.ActivityInicioSesionBinding

import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot

class InicioSesionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioSesionBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Iniciando sesión...")
        progressDialog.setCancelable(false)

        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // BOTÓN INGRESAR
        binding.btnIngresar.setOnClickListener {
            val email = binding.etCorreo.text.toString().trim()
            val password = binding.etContrasena.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                progressDialog.show()
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        progressDialog.dismiss()
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // BOTÓN GOOGLE
        binding.botonGoogle.setOnClickListener {
            iniciarSesionGoogle()
        }

        // BOTÓN REGISTRAR
        binding.btnRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun iniciarSesionGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleLauncher.launch(signInIntent)
    }

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            val errorMsg = when (e.statusCode) {
                7 -> "Error de red (Sin Internet)"
                10 -> "Error de configuración (SHA-1 o Web Client ID incorrecto)"
                12500 -> "Error interno de Google Play Services"
                12501 -> "Usuario canceló el inicio de sesión"
                12502 -> "Inicio de sesión en progreso"
                else -> "Error ${e.statusCode}: ${e.message}"
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        progressDialog.show()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val uid = user?.uid
                    val nombre = user?.displayName // Obtenemos el nombre de la cuenta de Google

                    if (uid != null && nombre != null) {
                        // Guardamos el nombre en la base de datos para que aparezca en el Inicio
                        val userRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(uid)
                        
                        // Verificamos si ya existe el nombre para no sobrescribir datos personalizados
                        userRef.get().addOnSuccessListener { snapshot ->
                            if (!snapshot.child("nombre").exists()) {
                                userRef.child("nombre").setValue(nombre)
                            }
                            if (!snapshot.child("modoRiego").exists()) {
                                userRef.child("modoRiego").setValue("Automático")
                            }
                            if (!snapshot.child("notificaciones").exists()) {
                                userRef.child("notificaciones").setValue(true)
                            }
                            
                            progressDialog.dismiss()
                            Toast.makeText(this, "Inicio con Google exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }.addOnFailureListener {
                            // Si falla la consulta, igual entramos
                            progressDialog.dismiss()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        progressDialog.dismiss()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    progressDialog.dismiss()
                    val errorMsg = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error Firebase: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
    }
}
