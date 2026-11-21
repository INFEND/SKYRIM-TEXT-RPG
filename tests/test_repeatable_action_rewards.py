"""Regression guard: repeatable skill checks must not mint money or quests."""
import pathlib
import unittest

SOURCE=(pathlib.Path(__file__).resolve().parents[1]/
        "app/src/main/java/dev/infend/skyrimtext/RpgProgress.java").read_text(encoding="utf-8")

class RepeatableActionContracts(unittest.TestCase):
    def test_no_repeatable_money_farm_or_fake_quest_history(self):
        start=SOURCE.index("public boolean perform(GameEngine g,String action)")
        end=SOURCE.index("public void observe(GameEngine g,String action",start)
        body=SOURCE[start:end]
        self.assertIn("attemptedToday.contains(action)",body)
        self.assertIn("attemptedToday.add(action)",body)
        self.assertIn("skillExperience(g,skill,14+index*2)",body)
        self.assertNotIn("g.gold+=",body)
        self.assertNotIn("completed.add(",body)
        self.assertNotIn("failed.add(",body)
        self.assertIn('g.mode="RESULT"',body)

if __name__=="__main__":
    unittest.main()
