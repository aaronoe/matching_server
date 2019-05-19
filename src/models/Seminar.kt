package de.aaronoe.models

import java.util.*

data class Seminar(
    val name: String,
    val capacity: Int,
    val id: String = UUID.randomUUID().toString()
) {

    val assignments: MutableList<Student> = mutableListOf()

    val canAssignMore: Boolean
        get() = assignments.size < capacity

}