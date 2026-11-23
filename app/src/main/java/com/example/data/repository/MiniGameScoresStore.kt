package com.example.data.repository

import android.content.Context

/**
 * Lightweight persistence for mini-game best scores.
 * Uses SharedPreferences so no Room schema/migration is involved.
 */
object MiniGameScoresStore {
  private const val PREFS_NAME = "fillwords_mini_games"

  const val KEY_ANAGRAM_BEST = "anagram_best"
  const val KEY_WORD_CHAIN_BEST = "word_chain_best"
  const val KEY_HANGMAN_BEST = "hangman_best"
  const val KEY_MEMORY_BEST = "memory_best" // fewest moves wins

  /** Stored best score, or 0 when no record exists yet. */
  fun best(context: Context, key: String): Int =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getInt(key, 0)

  /**
   * Saves [value] only when it beats the stored record.
   *
   * @param lowerIsBetter true for games where fewer moves/time is better.
   * @return true when a new record was set.
   */
  fun saveBest(
    context: Context,
    key: String,
    value: Int,
    lowerIsBetter: Boolean = false,
  ): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getInt(key, if (lowerIsBetter) Int.MAX_VALUE else 0)
    val isRecord = if (lowerIsBetter) value in 1 until current else value > current
    if (isRecord) {
      prefs.edit().putInt(key, value).apply()
    }
    return isRecord
  }
}
