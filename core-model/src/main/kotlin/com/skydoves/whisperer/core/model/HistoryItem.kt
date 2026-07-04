package com.skydoves.whisperer.core.model

/**
 * A "recently worn" entry on the dashboard. [score] is a free-form label
 * (empty when there is no meaningful score, e.g. a plain worn garment).
 */
data class HistoryItem(
    val day: String,
    val outfit: String,
    val score: String = "",
    val weather: String
)
