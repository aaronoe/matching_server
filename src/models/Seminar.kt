package de.aaronoe.models

import java.util.*

typealias Priority = Int

data class Seminar(
    val name: String,
    val capacity: Int,
    val id: String = UUID.randomUUID().toString()
) {

    val assignment: MutableList<Student> = mutableListOf()

    val canAssignMore: Boolean
        get() = assignment.size < capacity

    private val assignedStudents = mutableSetOf<Pair<Student, Priority>>()

    private fun addStudentToAssignment(
        student: Student,
        priority: Priority,
        assignments: MutableMap<Student, Seminar>,
        unassignedStudents: MutableSet<Student>
    ) {
        assignedStudents.add(student to priority)
        assignments[student] = this
        unassignedStudents.remove(student)
    }

    private fun removeStudentFromAssignment(
        student: Pair<Student, Priority>,
        assignments: MutableMap<Student, Seminar>,
        unassignedStudents: MutableSet<Student>
    ) {
        assignedStudents.remove(student)
        assignments.remove(student.first)
        unassignedStudents.add(student.first)
    }

    fun tryAssign(
        student: Student,
        priority: Int,
        assignments: MutableMap<Student, Seminar>,
        unassignedStudents: MutableSet<Student>
    ): Boolean {
        return if (assignedStudents.size < capacity) {
            addStudentToAssignment(student, priority, assignments, unassignedStudents)
            true
        } else {
            val lowestPriorityAssignment = assignedStudents.maxBy(Pair<Student, Priority>::second)!!

            // if the new student has a higher priority, assign him instead
            if (priority < lowestPriorityAssignment.second) {
                assignedStudents.apply {
                    removeStudentFromAssignment(lowestPriorityAssignment, assignments, unassignedStudents)
                    addStudentToAssignment(student, priority, assignments, unassignedStudents)
                }
                true
            } else false
        }
    }

}