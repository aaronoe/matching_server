package de.aaronoe.models

import java.util.*

data class Student(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val preferences: List<Seminar>
) {

    var match: Seminar? = null

}
