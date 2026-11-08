"""Continuation prose remains chapter-specific and does not alter mechanics."""
import pathlib
import re
import unittest

SOURCE=(pathlib.Path(__file__).resolve().parents[1]/
        "app/src/main/java/dev/infend/skyrimtext/DragonContinuation.java")

class ContinuationNarrative(unittest.TestCase):
    def test_three_independent_investigation_scenes(self):
        source=SOURCE.read_text(encoding="utf-8")
        section=source.split("private static final String[] STORIES={",1)[1].split(
            "private static final String[][] OPTIONS=",1)[0]
        for marker in ("Хранительница лесопилки", "Капитан стражи", "Портовый писарь",
                       "Ривервуд", "Драконьем Пределе", "Виндхельму"):
            self.assertIn(marker,section)
        self.assertEqual(section.count(">**—"),3)
        self.assertEqual(section.count("**\\n>**—"),3)
        self.assertGreater(len(section),8500)
        self.assertIn("private static final String[][] OPTIONS=",source)
        self.assertIn("DRAGON_NEXT:",source)
        self.assertIn("dragonNextUsed",source)
        self.assertIn("dragonNextFailed",source)

if __name__=="__main__":
    unittest.main()
