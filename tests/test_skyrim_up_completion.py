"""Regression checks for the Skyrim-up cleanup and conservative AI updates."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"


class SkyrimUpCompletion(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        cls.engine=(JAVA/"GameEngine.java").read_text(encoding="utf-8")
        cls.shop=(JAVA/"FixShopState.java").read_text(encoding="utf-8")
        cls.ai=(JAVA/"AiGameTurn.java").read_text(encoding="utf-8")
        cls.prompt=(JAVA/"AiStoryClient.java").read_text(encoding="utf-8")

    def test_world_editor_entrypoints_and_flag_are_gone(self):
        for source in (self.ui,self.engine,self.shop):
            self.assertNotIn("editorEnabled",source)
            self.assertNotIn("fixTeleport(",source)
        for old in ("fixCreatePlace(","fixCreateQuest(","fixQueueEncounter(",
                    "fixMembership(","fixRewriteEnding(","fixNpcChoice("):
            self.assertNotIn(old,self.ui)
        self.assertIn('"world",world.save()',self.shop)  # Existing saves retained.
        self.assertIn('FixShopWorld.restore(j.optJSONObject("world"))',self.shop)

    def test_safe_actions_stay_outside_ai_probability_proposal(self):
        source=(JAVA/"AiRoutineAction.java").read_text(encoding="utf-8")
        self.assertIn("requiresNoCheck(value)",self.ui)
        self.assertIn("storeAiPending(compound,-1,-1,-1)",self.ui)
        self.assertIn("RISK.matcher(action).find()",source)
        self.assertIn("GameEngine.fromJson(saved,true)",self.ui)

    def test_quest_rewards_apply_once_and_are_visible(self):
        self.assertIn('"rewardGold"',self.ai)
        self.assertIn('"rewardXp"',self.ai)
        self.assertIn('"COMPLETED".equals(status)',self.ai)
        self.assertIn('questGoldReward+=rewardGold',self.ai)
        self.assertIn('questXpReward+=rewardXp',self.ai)
        self.assertIn('selectedAiQuest.optInt("rewardGold",0)',self.ui)
        self.assertIn('questUpdates:[{id,title,objective,stage,status,rewardGold',self.prompt)


if __name__=="__main__":
    unittest.main()
