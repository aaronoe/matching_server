package de.aaronoe.algorithms.cpp

import de.aaronoe.algorithms.StudentMatchingAlgorithm

object CppHungarian: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.Hungarian)
object CppRsd: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.RSD)
object CppPopular: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.Popular)
object CppPopularModified: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.PopularModified)
object CppMaxPareto: StudentMatchingAlgorithm by BaseCppAlgorithm(BaseCppAlgorithm.Algorithm.MaxPareto)