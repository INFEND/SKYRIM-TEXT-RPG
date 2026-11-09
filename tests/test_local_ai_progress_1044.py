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
        self.assertIn("Загрузка GGUF в оперативную память",worker)
        self.assertIn("Обработка контекста и генерация на CPU",worker)
        self.assertIn('val maxTokens=if(chanceOnly)80 else if(controlled)1024 else 448',client)
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
        self.assertIn("Llama.loadModel(",service)
        self.assertIn("Llama.complete(",service)
        self.assertIn("replyStatus(",service)
        self.assertIn("tokens=result.tokensGenerated",service)
        self.assertIn("speed=result.tokensPerSecond",service)
        self.assertIn("Process.killProcess(Process.myPid())",service)
        manifest=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:process=":llm"',manifest)
if __name__=="__main__":
    unittest.main()
