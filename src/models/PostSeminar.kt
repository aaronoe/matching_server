package de.aaronoe.models

data class PostSeminar(val name: String, val capacity: Int) {

    fun toSeminar() = Seminar(name = name, capacity = capacity)

}