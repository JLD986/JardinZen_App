package com.example.jardnzen_app.Modelos

import java.io.Serializable

data class Planta(
   var id: String = "",
   val nombre: String = "",
   val temperatura: String = "",
   val humedad: String = "",
   val luz: String = "",
   val agua: String = "",
   val imagenUrl: String = ""
) : Serializable