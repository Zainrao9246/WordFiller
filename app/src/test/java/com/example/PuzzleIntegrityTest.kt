package com.example

import com.example.data.model.LevelType
import com.example.data.puzzle.BonusDictionary
import com.example.data.puzzle.CuratedPuzzles
import com.example.data.puzzle.PuzzleGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleIntegrityTest {

  @Test
  fun all100CuratedPuzzlesAreValidPartitions() {
    assertEquals(100, CuratedPuzzles.allPuzzles.size)
    val seenIds = HashSet<String>()
    CuratedPuzzles.allPuzzles.forEachIndexed { index, puzzle ->
      assertEquals("Sequential levelNumber mismatch at index $index", index + 1, puzzle.levelNumber)
      assertTrue("Duplicate puzzle id ${puzzle.id}", seenIds.add(puzzle.id))
      assertTrue(
        "Puzzle ${puzzle.id} (${puzzle.title}) failed grid partition validation",
        puzzle.validate(),
      )
    }

    val types = CuratedPuzzles.allPuzzles.map { it.levelType }.toSet()
    assertTrue(LevelType.CLASSIC in types)
    assertTrue(LevelType.BLITZ in types)
    assertTrue(LevelType.MEGA in types)
    assertTrue(LevelType.SIGNATURE in types)
  }

  @Test
  fun signatureClassic8x11MatchesExactBoard() {
    val grid = CuratedPuzzles.signatureClassicPuzzle.buildCharGrid()
    val expectedRows = listOf(
      "QUESTION",
      "FILLWORD",
      "SMARTIDE",
      "THINKOLA",
      "BBOOKGIC",
      "RAWORDSE",
      "NIVEKINE",
      "CLERSLLS",
      "LETTERPU",
      "SEARCHZZ",
      "GENIUSLE",
    )
    assertEquals(11, grid.size)
    expectedRows.forEachIndexed { r, expected ->
      assertEquals("Row $r mismatch", expected, String(grid[r]))
    }
  }

  @Test
  fun bonusDictionaryHasRichWordCoverage() {
    assertTrue("Expected at least 400 bonus words", BonusDictionary.words.size >= 400)
    assertTrue("WORD" in BonusDictionary.words)
    assertTrue("SEARCH" in BonusDictionary.words)
    assertTrue("SUN" in BonusDictionary.words)
  }

  @Test
  fun proceduralGeneratorProducesValidBoardsAcrossAllSizes() {
    val sizes = listOf(5 to 5, 6 to 6, 7 to 7, 10 to 8)
    for ((rows, cols) in sizes) {
      for (seed in 1L..10L) {
        val puzzle = PuzzleGenerator.generate(rows = rows, cols = cols, seed = seed * 997L)
        assertTrue(
          "Generated ${rows}x${cols} seed=$seed failed validation",
          puzzle.validate(),
        )
      }
    }
  }
}
