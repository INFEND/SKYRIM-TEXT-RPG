import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ENGINE=ROOT/"engine/src/main/java/dev/infend/skyrimtext"
class LocalAiRecovery(unittest.TestCase):
    def test_worker_crash_never_replays_request(self):
        c=(APP/"LocalAiClient.kt").read_text()
        recovery=c.split("private fun recoverProcess(",1)[1].split("private fun currentGeneration",1)[0]
        self.assertIn("failGeneration(id",recovery)
        self.assertNotIn("connect(",recovery)
        self.assertNotIn("MSG_GENERATE",recovery)
    def test_local_validation_does_not_restart_or_commit_invalid_turn(self):
        ui=(APP/"MainActivity.java").read_text()
        self.assertIn("if(!local&&!external&&repairAttempt==0",ui)
        self.assertIn("AiGameTurn.parseLocalModel(answer,expectedChance)",ui)
        self.assertIn("НЕСОХРАНЁННЫЙ ОТВЕТ ИИ",ui)
        self.assertIn("if(!game.toJson().toString().equals(saved))",ui)
    def test_direct_qwen_prompt_and_post_action_grounding(self):
        preview=(ENGINE/"AiScenePreview.java").read_text()
        self.assertIn('endsWith("<|im_start|>assistant")',preview)
        self.assertIn(r'<think>\n\n</think>',preview)
        prompt=(APP/"AiStoryClient.java").read_text()
        self.assertIn("/no_think",prompt)
        self.assertIn("Не повторяй уже исполненное действие",prompt)
        self.assertIn("effects.hourDelta",prompt)
        self.assertIn("localChancePrompt(GameEngine game,String command)",prompt)
        self.assertIn("if(chanceOnly) AiStoryClient.localChancePrompt", (APP/"LocalAiClient.kt").read_text())
