"""Regression contract for one-shot scene actions and stale UI callbacks."""
from pathlib import Path
import unittest

SRC = (Path(__file__).resolve().parents[1] /
       "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")

class SceneActionReplayTest(unittest.TestCase):
    def test_event_actions_require_live_encounter(self):
        self.assertIn('"EVENT_HELP".equals(action) || "EVENT_SEARCH".equals(action)', SRC)
        self.assertIn('"EVENT_LEAVE".equals(action)) && !"EVENT".equals(mode)) return;', SRC)

    def test_dialogue_actions_require_conversation(self):
        self.assertIn('"TALK_HELP".equals(action) || "TALK_GIVE".equals(action)', SRC)
        self.assertIn('&& !"TALK".equals(mode)) return;', SRC)

    def test_quest_actions_require_quest_scene(self):
        self.assertIn('"Q_ACCEPT".equals(action) || "Q_ALTAR".equals(action)', SRC)
        self.assertIn('"Q_BOSS".equals(action) || "Q_FINISH".equals(action)', SRC)
        self.assertIn('&& !"QUEST".equals(mode)) return;', SRC)

    def test_battle_escape_guard_still_present(self):
        self.assertIn('if ("COMBAT".equals(mode)', SRC)
        self.assertIn('"FLEE".equals(action))) return;', SRC)

if __name__ == "__main__":
    unittest.main()
