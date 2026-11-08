"""Regression guard for unique, non-generic investigation outcomes."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/DragonContinuation.java").read_text(encoding="utf-8")


class ContinuationConsequencesTest(unittest.TestCase):
    def test_thirty_distinct_results(self):
        section = SOURCE.split("private static final String[][] CONSEQUENCES=", 1)[1].split("private static final int[][] SKILLS=", 1)[0]
        results = re.findall(r'^\s*"([^"]+)"[,}]?\s*$', section, re.MULTILINE)
        self.assertEqual(len(results), 10 * 4)
        self.assertEqual(len(set(results)), 10 * 4)
        for result in results:
            self.assertGreater(len(result), 90)
        self.assertIn("CONSEQUENCES[stage][choice-1]", SOURCE)

    def test_one_time_choices_and_proof_rules_remain(self):
        self.assertIn("dragonNextUsed.contains(action)", SOURCE)
        self.assertIn("dragonNextUsed.add(action)", SOURCE)
        self.assertIn("dragonNextFailed.add(action)", SOURCE)
        self.assertIn("hasRiskProof(g,stage)", SOURCE)
        self.assertIn('setQuestOutcome(questId(stage),"FAILED")', SOURCE)


if __name__ == "__main__":
    unittest.main()
