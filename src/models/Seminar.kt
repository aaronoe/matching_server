package de.aaronoe.models

import java.util.*

data class Seminar(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val capacity: Int
)