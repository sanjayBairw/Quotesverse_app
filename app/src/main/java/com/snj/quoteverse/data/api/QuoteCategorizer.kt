package com.snj.quoteverse.data.api

import java.util.Locale

/**
 * Automatically assigns one of QuoteVerse's 10 categories to ZenQuotes quotes
 * based on contextual keyword analysis and author heuristics.
 */
object QuoteCategorizer {

    private val categoryKeywords: Map<String, List<String>> = mapOf(
        "Love" to listOf(
            "love", "lover", "loving", "heart", "affection", "passion", "soulmate",
            "beloved", "devotion", "kindness", "compassion", "cherish", "adore"
        ),
        "Friendship" to listOf(
            "friend", "friendship", "companion", "company", "together", "kindred",
            "fellow", "brotherhood", "sisterhood", "solidarity"
        ),
        "Happiness" to listOf(
            "happy", "happiness", "joy", "joyful", "smile", "smiles", "laugh", "laughter", "laughs",
            "cheerful", "pleasure", "delight", "peace", "bliss", "serenity", "grateful", "gratitude", "tears"
        ),
        "Success" to listOf(
            "success", "successful", "succeed", "win", "winner", "victory", "triumph",
            "achieve", "achievement", "goal", "excellence", "mastery", "prosper", "wealth", "finish"
        ),
        "Motivation" to listOf(
            "motivate", "motivation", "effort", "action", "hustle", "drive", "discipline",
            "courage", "brave", "persist", "persistence", "perseverance", "hard work", "never give up",
            "conquer", "climb", "fight", "strive", "strong", "strength"
        ),
        "Study" to listOf(
            "study", "learn", "learning", "education", "teach", "teacher", "student",
            "school", "book", "read", "reading", "knowledge", "skill", "intelligent", "intelligence"
        ),
        "Attitude" to listOf(
            "attitude", "optimism", "optimistic", "pessimism", "mindset", "positive",
            "positivity", "react", "reaction", "habit", "perspective", "mood", "confidence", "calm"
        ),
        "Wisdom" to listOf(
            "wisdom", "wise", "truth", "philosophy", "philosophical", "silence",
            "understand", "understanding", "fool", "reason", "virtue", "conscience", "judgment", "depth"
        ),
        "Inspirational" to listOf(
            "inspire", "inspiration", "inspirational", "dream", "dreams", "future",
            "vision", "hope", "ignite", "magic", "spark", "miracle", "wonder", "soar", "wings", "star"
        ),
        "Life" to listOf(
            "life", "live", "living", "alive", "journey", "world", "human",
            "time", "moment", "destiny", "exist", "existence", "path", "experience"
        )
    )

    /**
     * Determines the most appropriate category for a quote.
     * Defaults to "Wisdom" or "Life" if no specific keywords match.
     */
    fun categorize(quoteText: String, author: String = ""): String {
        val lowerText = (quoteText + " " + author).lowercase(Locale.ROOT)

        var bestCategory = "Wisdom"
        var maxScore = 0

        for ((category, keywords) in categoryKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (keyword.contains(" ")) {
                    if (lowerText.contains(keyword)) score += 3
                } else {
                    // Match whole word or prominent substring
                    val regex = "\\b$keyword\\b".toRegex()
                    if (regex.containsMatchIn(lowerText)) {
                        score += 2
                    } else if (lowerText.contains(keyword)) {
                        score += 1
                    }
                }
            }

            if (score > maxScore) {
                maxScore = score
                bestCategory = category
            }
        }

        return if (maxScore > 0) bestCategory else "Inspirational"
    }
}

