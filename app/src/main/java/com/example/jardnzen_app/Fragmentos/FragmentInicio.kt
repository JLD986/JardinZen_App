package com.example.jardnzen_app.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jardnzen_app.Adaptadores.PlantaAdapter
import com.example.jardnzen_app.Modelos.Planta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.locochones.jardnzen_app.databinding.FragmentInicioBinding

import android.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.jardnzen_app.Adaptadores.AlmanaqueAdapter
import com.example.jardnzen_app.Modelos.PlantaAlmanaque
import com.locochones.jardnzen_app.R

class FragmentInicio : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseUsuarios: DatabaseReference
    private lateinit var databasePlantas: DatabaseReference
    private lateinit var databaseSensores: DatabaseReference
    private lateinit var databaseAlmanaque: DatabaseReference

    private lateinit var adapter: PlantaAdapter
    private val listaPlantas = mutableListOf<Planta>()
    private val listaAlmanaque = mutableListOf<PlantaAlmanaque>()
    private val deviceId = "JardinZenESP32"

    private var currentAlmanaqueAdapter: AlmanaqueAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return binding.root
        
        val firebaseDB = FirebaseDatabase.getInstance()
        databaseUsuarios = firebaseDB.getReference("Usuarios")
        databasePlantas = firebaseDB.getReference("Usuarios").child(uid).child("plantas")
        databaseAlmanaque = firebaseDB.getReference("Almanaque")

        binding.recyclerJardin.layoutManager = LinearLayoutManager(requireContext())
        adapter = PlantaAdapter(listaPlantas)
        binding.recyclerJardin.adapter = adapter

        obtenerNombreUsuario(uid)
        cargarPlantas(uid)
        cargarAlmanaque()

        binding.btnAgregarPlanta.setOnClickListener { mostrarDialogoSeleccionarPlanta(uid) }
        binding.btnBorrarPlanta.setOnClickListener { mostrarDialogoBorrarPlanta() }

        return binding.root
    }

    private fun obtenerNombreUsuario(uid: String) {
        databaseUsuarios.child(uid).child("nombre").get().addOnSuccessListener { snapshot ->
            val nombre = snapshot.value?.toString()
            binding.holaUsuario.text = if (!nombre.isNullOrEmpty()) "Hola $nombre" else "Hola ????"
        }
    }

    private fun cargarPlantas(uid: String) {
        databasePlantas.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaPlantas.clear()
                for (plantaSnapshot in snapshot.children) {
                    val planta = plantaSnapshot.getValue(Planta::class.java)
                    planta?.let {
                        it.id = plantaSnapshot.key ?: ""
                        listaPlantas.add(it)
                    }
                }
                
                actualizarSensoresEnTiempoReal(uid)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error al cargar plantas", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cargarAlmanaque() {
        android.util.Log.d("FirebaseDebug", "Iniciando escucha de Almanaque...")
        databaseAlmanaque.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listaAlmanaque.clear()
                if (!snapshot.exists()) {
                    android.util.Log.e("FirebaseDebug", "¡OJO! El nodo 'Almanaque' NO EXISTE o está vacío en Firebase.")
                } else {
                    android.util.Log.d("FirebaseDebug", "Datos crudos de Almanaque: ${snapshot.value}")
                }
                for (plantaSnap in snapshot.children) {
                    android.util.Log.d("FirebaseDebug", "--- Analizando nodo: ${plantaSnap.key} ---")
                    android.util.Log.d("FirebaseDebug", "Contenido crudo: ${plantaSnap.value}")
                    
                    // Verificación manual de campos
                    val nComun = plantaSnap.child("nombre_comun").value
                    val desc = plantaSnap.child("descripcion").value
                    android.util.Log.d("FirebaseDebug", "Campo 'nombre_comun' detectado: $nComun")
                    android.util.Log.d("FirebaseDebug", "Campo 'descripcion' detectado: $desc")

                    try {
                        val planta = plantaSnap.getValue(PlantaAlmanaque::class.java)
                        if (planta != null) {
                            listaAlmanaque.add(planta.copy(id = plantaSnap.key ?: "")) 
                            android.util.Log.d("FirebaseDebug", "¡ÉXITO! Planta parseada: ${planta.nombre_comun}")
                        } else {
                            android.util.Log.e("FirebaseDebug", "FALLO: El objeto PlantaAlmanaque resultó nulo para ${plantaSnap.key}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseDebug", "ERROR de mapeo en ${plantaSnap.key}: ${e.message}")
                    }
                }
                
                // Actualizar el adaptador si el diálogo está abierto
                currentAlmanaqueAdapter?.actualizarLista(listaAlmanaque)
                android.util.Log.d("FirebaseDebug", "Finalizado: ${listaAlmanaque.size} plantas listas.")
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseDebug", "Error de conexión/permisos: ${error.message}")
            }
        })
    }

    private fun actualizarSensoresEnTiempoReal(uid: String) {
        databaseSensores = FirebaseDatabase.getInstance()
            .getReference("Usuarios")
            .child(uid)
            .child("dispositivos")
            .child(deviceId)
            .child("sensores")

        databaseSensores.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (listaPlantas.isEmpty()) return

                val temperatura = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                val humedad = snapshot.child("humedad_suelo").getValue(Int::class.java) ?: 0
                val luz = snapshot.child("luz").getValue(Int::class.java) ?: 0
                val agua = snapshot.child("nivel_pct").getValue(Double::class.java) ?: 0.0

                val primeraPlanta = listaPlantas[0]
                val plantaActualizada = primeraPlanta.copy(
                    temperatura = "$temperatura °C",
                    humedad = "$humedad %",
                    luz = "$luz %",
                    agua = "$agua %"
                )
                listaPlantas[0] = plantaActualizada
                adapter.notifyItemChanged(0)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun mostrarDialogoSeleccionarPlanta(uid: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_seleccionar_planta, null)
        val etBuscar = dialogView.findViewById<EditText>(R.id.et_buscar_planta)
        val rvAlmanaque = dialogView.findViewById<RecyclerView>(R.id.rv_almanaque)
        val layoutSugerir = dialogView.findViewById<LinearLayout>(R.id.layout_sugerir)
        val btnSugerir = dialogView.findViewById<View>(R.id.btn_sugerir_planta)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        currentAlmanaqueAdapter = AlmanaqueAdapter(listaAlmanaque) { plantaSeleccionada ->
            agregarPlantaAUsuario(uid, plantaSeleccionada)
            dialog.dismiss()
        }

        rvAlmanaque.layoutManager = LinearLayoutManager(requireContext())
        rvAlmanaque.adapter = currentAlmanaqueAdapter

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString()
                currentAlmanaqueAdapter?.filtrar(texto) { tieneResultados ->
                    layoutSugerir.visibility = if (tieneResultados || texto.isEmpty()) View.GONE else View.VISIBLE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSugerir.setOnClickListener {
            val plantaSugerida = etBuscar.text.toString()
            FirebaseDatabase.getInstance().getReference("peticiones_plantas").push().setValue(
                mapOf(
                    "nombre_sugerido" to plantaSugerida,
                    "usuario_id" to uid,
                    "fecha_peticion" to System.currentTimeMillis(),
                    "estado" to "pendiente"
                )
            )
            Toast.makeText(requireContext(), "¡Gracias! Petición enviada al nodo peticiones_plantas.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            currentAlmanaqueAdapter = null
        }

        dialog.show()
    }

    private fun agregarPlantaAUsuario(uid: String, plantaAlmanaque: PlantaAlmanaque) {
        val nuevaPlanta = Planta(
            nombre = plantaAlmanaque.nombre_comun,
            imagenUrl = plantaAlmanaque.imagen_ref,
            temperatura = "--",
            humedad = "--",
            luz = "--",
            agua = "--"
        )
        databasePlantas.push().setValue(nuevaPlanta).addOnSuccessListener {
            Toast.makeText(requireContext(), "${plantaAlmanaque.nombre_comun} agregada a tu jardín", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoBorrarPlanta() {
        if (listaPlantas.isEmpty()) {
            Toast.makeText(requireContext(), "No hay plantas para borrar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombresPlantas = listaPlantas.map { it.nombre }.toTypedArray()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona planta a borrar")
        builder.setItems(nombresPlantas) { _, which ->
            val plantaId = listaPlantas[which].id
            databasePlantas.child(plantaId).removeValue().addOnSuccessListener {
                Toast.makeText(requireContext(), "Planta eliminada", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
