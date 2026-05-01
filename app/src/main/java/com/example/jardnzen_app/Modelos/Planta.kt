package com.example.jardnzen_app.Modelos

import java.io.Serializable

data class Planta(
   var id: String = "",
   var nombre: String = "",
   var temperatura: String = "",
   var humedad: String = "",
   var luz: String = "",
   var agua: String = "",
   var imagenUrl: String = ""
) : Serializable
