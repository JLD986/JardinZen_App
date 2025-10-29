package com.example.jardnzen_app.Modelos

data class RegistroHistorial(
    var id: String = "",
    val fecha: String = "",
    val temperatura: Double = 0.0,
    val humedad: Int = 0,
    val luz: Int = 0
)