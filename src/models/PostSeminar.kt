package de.aaronoe.models

data class PostSeminar(val name: String) {

    fun toSeminar() = Seminar(name = name, capacity = 12)

}