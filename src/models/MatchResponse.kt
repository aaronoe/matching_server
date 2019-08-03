package de.aaronoe.models

import de.aaronoe.benchmark.Statistics

data class MatchResponse(
    val matches: List<Matching>,
    val statistics: Statistics,
    val runtime: Long
)