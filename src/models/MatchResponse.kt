package de.aaronoe.models


data class MatchResponse(
    val matches: List<Matching>,
    val profile: List<Int>,
    val unassignedCount: Int
)