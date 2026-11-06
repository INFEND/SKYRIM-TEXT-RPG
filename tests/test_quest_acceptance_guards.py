"""Regression checks for one-time quest acceptance and bounty progress."""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
ENGINE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")


class QuestAcceptanceGuards(unittest.TestCase):
    def test_main_quest_acceptance_is_one_time(self):
        self.assertRegex(ENGINE, r'case "Q_ACCEPT":\s*if\s*\(questStage\s*==\s*0\)')
        self.assertRegex(ENGINE, r'if\s*\(questStage\s*==\s*0\)\s*\{questStage=1;gold\+=40;')

    def test_bounty_acceptance_cannot_reset_active_kills(self):
        self.assertRegex(ENGINE, r'case "SIDE_ACCEPT":\s*if\s*\(sideQuest\s*==\s*2\)')
        self.assertRegex(ENGINE, r'if\s*\(sideQuest\s*==\s*2\)\s*\{sideQuest=1;sideKills=0;')

    def test_quest_stage_survives_schema_one_save(self):
        self.assertIn('.put("questStage",questStage)', ENGINE)
        self.assertIn('j.optInt("questStage",0)', ENGINE)


if __name__ == "__main__":
    unittest.main()
