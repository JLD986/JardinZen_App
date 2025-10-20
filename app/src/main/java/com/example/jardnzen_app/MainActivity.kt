package com.locochones.jardnzen_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.jardnzen_app.Fragmentos.FragmentConfiguraciones
import com.example.jardnzen_app.Fragmentos.FragmentEstadisticas
import com.example.jardnzen_app.Fragmentos.FragmentInicio
import com.example.jardnzen_app.Fragmentos.FragmentNotifiaciones
import com.example.jardnzen_app.Fragmentos.FragmentPerfil
import com.example.jardnzen_app.InicioSesionActivity
import com.google.firebase.auth.FirebaseAuth
import com.locochones.jardnzen_app.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        firebaseAuth = FirebaseAuth.getInstance()

        // Si el usuario no ha iniciado sesión, lo mandamos al login
        if (firebaseAuth.currentUser == null) {
            irInicioSesion()
            return
        }

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNV) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }


        // Fragment por defecto
        verFragmentInicio()

        // Navegación inferior
        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_Inicio -> {
                    verFragmentInicio()
                    true
                }
                R.id.item_Estadisticas -> {
                    verFragmentEstadisticas()
                    true
                }
                R.id.item_Perfil -> {
                    verFragmentPerfil()
                    true
                }
                R.id.item_Notificaciones -> {
                    verFragmentNotificaciones()
                    true
                }
                R.id.item_Configuracion -> {
                    verFragmentConfiguracion()
                    true
                }
                else -> false
            }
        }
    }

    private fun irInicioSesion() {
        startActivity(Intent(applicationContext, InicioSesionActivity::class.java))
        finishAffinity()
    }

    // ---------- Fragmentos ----------

    private fun verFragmentInicio() {

        val fragment = FragmentInicio()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragment Inicio")
            .commit()
    }

    private fun verFragmentEstadisticas() {

        val fragment = FragmentEstadisticas()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragment Estadísticas")
            .commit()
    }

    private fun verFragmentPerfil() {

        val fragment = FragmentPerfil()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragment Perfil")
            .commit()
    }

    private fun verFragmentNotificaciones() {

        val fragment = FragmentNotifiaciones()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragment Notificaciones")
            .commit()
    }

    private fun verFragmentConfiguracion() {

        val fragment = FragmentConfiguraciones()
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentoFL.id, fragment, "Fragment Configuración")
            .commit()
    }

}
