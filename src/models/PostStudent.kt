package de.aaronoe.models

data class PostStudent(
    val name: String,
    val preferences: List<Seminar>
) {

    fun toStudent() = Student(name = name, preferences = preferences)

}