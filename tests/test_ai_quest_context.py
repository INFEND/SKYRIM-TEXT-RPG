"""Both remote and local AI must receive persistent quest stages."""
import pathlib
import unittest

SOURCE=(pathlib.Path(__file__).resolve().parents[1] /
        "app/src/main/java/dev/infend/skyrimtext/AiStoryClient.java").read_text(encoding="utf-8")

class AiQuestContext(unittest.TestCase):
    def test_ai_quest_memory_is_in_both_prompts(self):
        self.assertIn('localSection(b,"Авторские задания ИИ: ",aiQuestSummary(game,280)',SOURCE)
        self.assertIn('append(aiQuestSummary(game,1600))',SOURCE)
        self.assertIn('private static String aiQuestSummary(GameEngine game,int budget)',SOURCE)
        self.assertIn('game.progress.aiQuestRecords',SOURCE)
    def test_local_model_knows_validated_patch_fields(self):
        self.assertIn('effects.questUpdates:[{id,title,objective,stage,status}]',SOURCE)
        self.assertIn('effects.npcRewrites:[{id,relation,resurrect}]',SOURCE)
        self.assertIn('/no_think',SOURCE)

if __name__=="__main__": unittest.main()
