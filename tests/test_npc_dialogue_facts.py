"""Regression checks for persistent NPC dialogue outcomes and one-time rewards."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ENGINE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")
SCENE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/SceneState.java").read_text(encoding="utf-8")


class NpcDialogueFactsTest(unittest.TestCase):
    def test_reopening_dialogue_does_not_reset_npc_status(self):
        self.assertIn('if (!sceneState.contains("npc", currentConversationId()))', ENGINE)
        self.assertIn('sceneState.confirm("npc", currentConversationId(), currentConversationName(), "MET")', ENGINE)

    def test_help_and_charity_have_distinct_persistent_outcomes(self):
        self.assertIn('currentConversationName(), "INFORMED"', ENGINE)
        self.assertIn('currentConversationName(), "HELPED"', ENGINE)
        self.assertIn('currentConversationId()+"_helped"', ENGINE)
        self.assertIn('currentConversationName(), "REWARDED"', ENGINE)

    def test_repeat_charity_cannot_grant_xp(self):
        start = ENGINE.index('case "TALK_GIVE":')
        end = ENGINE.index('case "BUY_HP":', start)
        block = ENGINE[start:end]
        self.assertLess(block.index('if(hasNpcStatus("_helped"))'), block.index('gold-=10;reputation++;addXp(8);'))
        self.assertIn('Повторная награда не положена', block)

    def test_scene_facts_survive_schema_one_save(self):
        self.assertIn('.put("sceneState",sceneState.toJson())', ENGINE)
        self.assertIn('SceneState.fromJson(j.optJSONObject("sceneState"))', ENGINE)
        self.assertIn('entries.put(safeId', SCENE)


if __name__ == "__main__":
    unittest.main()
