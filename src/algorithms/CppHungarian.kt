package de.aaronoe.algorithms

import de.aaronoe.algorithms.cpp.BaseCppAlgorithm

object CppHungarian: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.Hungarian)
object CppRsd: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.RSD)
object CppPopular: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.Popular)
object CppMaxPareto: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.MaxPareto)