"""Offline generation must report real phases and allow a tiny smoke test."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
J=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class LocalAiProgressContract(unittest.TestCase):
    def test_progress_and_short_test(self):
        api=(J/"AiStoryClient.java").read_text(encoding="utf-8")
        client=(J/"LocalAiClient.kt").read_text(encoding="utf-8")
        worker=(J/"LocalAiInferenceService.kt").read_text(encoding="utf-8")
        ui=(J/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("default void onLocalStatus(String status)",api)
        self.assertIn("default void onLocalMetrics(int tokens,float tokensPerSecond,long elapsedMs)",api)
        self.assertIn("fun testModel(",client)
        self.assertIn("MSG_STATUS",client)
        self.assertIn("MSG_STATUS",worker)
        self.assertIn("Загрузка модели Qwen из памяти телефона",worker)
        self.assertIn("Генерация ответа, токены появятся",worker)
        self.assertIn('val maxTokens=if(chanceOnly)128 else if(controlled)2304 else 768',client)
        self.assertIn('ui.postDelayed(expiry,12L*60L*1000L)',client)
        self.assertIn('this::testLocalModel',ui)
        self.assertIn('aiLocalPhase',ui)
        self.assertIn('Прошло ',ui)
        self.assertIn('Последний замер: ',ui)
    def test_safe_state_still_commits_only_after_complete_json(self):
        ui=(J/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("AiGameTurn completed=AiGameTurn.parse(answer)",ui)
        self.assertIn("completed.apply(next,actionResolved)",ui)
        self.assertIn("next.aiTurn=completed",ui)
        self.assertIn("requestAiTurn(lockedFreeInstruction(customAction,fixedChance,die)",ui)
        self.assertIn("if(!game.toJson().toString().equals(saved))",ui)
        self.assertIn("game.choose(c.action)",ui)
    def test_native_model_not_in_ui_process(self):
        service=(J/"LocalAiInferenceService.kt").read_text(encoding="utf-8")
        self.assertIn("engine.startEngine(",service)
        self.assertIn("engine.launchCompletion(",service)
        self.assertIn("status(",service)
        self.assertIn("count = tokenCount",service)
        self.assertIn("speed = if (elapsed > 0)",service)
        self.assertIn("Process.killProcess(Process.myPid())",service)
        manifest=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:process=":llm"',manifest)
if __name__=="__main__":
    unittest.main()
