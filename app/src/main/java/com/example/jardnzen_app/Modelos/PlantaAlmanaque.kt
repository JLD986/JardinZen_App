package com.example.jardnzen_app.Modelos

import java.io.Serializable

data class PlantaAlmanaque(
    var id: String = "",
    var nombre_comun: String = "",
    var descripcion: String = "",
    var imagen_ref: String = "",
    var humedad_ideal: Int = 0,
    var temp_max: Int = 0
) : Serializable
