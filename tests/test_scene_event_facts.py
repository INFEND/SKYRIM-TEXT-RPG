"""Scene memory facts are emitted only by validated engine events."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ENGINE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")
SCENE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/SceneState.java").read_text(encoding="utf-8")

class SceneEventFactsTest(unittest.TestCase):
    def test_npc_recorded_only_from_talk_action(self):
        self.assertIn('case "TALK": confirmCurrentConversation(); mode="TALK"; break;', ENGINE)
        self.assertRegex(ENGINE, r'case "TALK_HELP":\s*confirmCurrentConversation\(\);')
        self.assertIn('sceneState.confirm("npc", currentConversationId(), currentConversationName(), "MET")', ENGINE)
        self.assertIn('return "local_" + Math.floorMod(region + day, names.length) + "_region_" + region;', ENGINE)
        self.assertNotIn('confirmCurrentConversation();\n        return new Scene(', ENGINE)

    def test_clue_requires_quest_location_and_stage(self):
        self.assertIn('case "Q_ALTAR": if(questStage==1 && region==5) {questStage=2;confirmSceneFact("clue", "quest_silver_plate"', ENGINE)
        self.assertIn('case "Q_FINISH": if(questStage==3 && region==3) {questStage=4;confirmSceneFact("clue", "quest_seal"', ENGINE)

    def test_optional_memory_does_not_block_gameplay(self):
        self.assertIn('catch (JSONException ignored)', ENGINE)
        self.assertIn('sceneState.confirm(kind, key, title, status)', ENGINE)
        self.assertIn('sceneState.toJson()', ENGINE)
        self.assertIn('SceneState.fromJson(j.optJSONObject("sceneState"))', ENGINE)
        self.assertIn('entries.put(safeId,', SCENE)

if __name__ == "__main__":
    unittest.main()
