package com.example.miguelpavonlimones_tfg

data class Partido(
    var fecha: String = "",
    var rival: String = "",
    var tipo: String = "",
    var nombreEquipo: String? = null,
    var usuarioId: String = ""
)

