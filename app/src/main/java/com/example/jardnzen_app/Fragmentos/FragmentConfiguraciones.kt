package com.example.jardnzen_app.Fragmentos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.locochones.jardnzen_app.R
import com.locochones.jardnzen_app.databinding.FragmentConfiguracionesBinding

class FragmentConfiguraciones : Fragment() {
    private var _binding: FragmentConfiguracionesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfiguracionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ajusteHumedad.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentoFL, FragmentAjusteHumedad())
                .addToBackStack(null)
                .commit()
        }

        binding.configAlert.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentoFL, FragmentAlertas())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}