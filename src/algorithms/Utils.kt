package de.aaronoe.algorithms

fun Collection<Int>.standartDeviation(): Double {
    val mean = average()
    val sd = fold(0.0) { accumulator, next -> accumulator + Math.pow(next - mean, 2.0) }
    return Math.sqrt(sd / size)
}