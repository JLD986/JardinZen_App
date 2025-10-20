package com.example.jardnzen_app.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.FragmentInicioBinding


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class FragmentInicio : Fragment() {
    private lateinit var binding: FragmentInicioBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference



    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInicioBinding.inflate(inflater, container, false)
        firebaseAuth = FirebaseAuth.getInstance()
        val user = firebaseAuth.currentUser

        databaseRef = FirebaseDatabase.getInstance().getReference("Usuarios")
        if (user != null) {
            obtenerUsuario(user.uid)

        }else{
            binding.holaUsuario.text = "Hola ????"
        }
        return binding.root
    }

    private fun obtenerUsuario(uid: String) {
        databaseRef.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val nombre = snapshot.child("nombre").value.toString()
                if (!nombre.isNullOrEmpty()) {
                    binding.holaUsuario.text = "Hola $nombre"
                } else {
                    binding.holaUsuario.text = "Hola ????"
                }

            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentInicio().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}