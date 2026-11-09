"""Protect the offline Qwen worker from the original native-crash regression."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT / "app/src/main/java/dev/infend/skyrimtext"
MANIFEST=ROOT / "app/src/main/AndroidManifest.xml"

class OfflineCrashGuard(unittest.TestCase):
    def test_remote_process_survives_native_failures_without_game_state_loss(self):
        xml=MANIFEST.read_text(encoding="utf-8")
        self.assertIn('android:name=".LocalAiInferenceService"',xml)
        self.assertIn('android:exported="false" android:process=":llm"',xml)
        ui=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("new LocalAiClient(this)",ui)
        self.assertIn("aiTurnStatus=\"Ошибка генерации:",ui)
        self.assertIn("GameEngine.fromJson(saved)",ui)
        self.assertIn("completed.apply(next,actionResolved)",ui)
        client=(JAVA/"LocalAiClient.kt").read_text(encoding="utf-8")
        self.assertIn("onServiceDisconnected",client)
        self.assertIn("failGeneration",client)
        self.assertIn("context.unbindService(conn)",client)
        self.assertIn("MSG_CANCEL",client)
    def test_prompt_is_compact_and_native_context_is_smaller(self):
        source=(JAVA/"AiStoryClient.java").read_text(encoding="utf-8")
        self.assertIn("localSystemPrompt",source)
        self.assertIn("localUserPrompt",source)
        self.assertIn("crop(b.toString(),1300)",source)
        worker=(JAVA/"LocalAiInferenceService.kt").read_text(encoding="utf-8")
        self.assertIn("contextSize=4096",worker)
        self.assertIn("maxTokens=maxTokens",worker)
        self.assertIn("if(prompt.isBlank()",worker)
        self.assertNotIn("api.openai.com",worker)
if __name__=="__main__": unittest.main()
