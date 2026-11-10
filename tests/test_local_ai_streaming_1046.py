"""Local GGUF must stream model tokens into the existing safe RPG pipeline."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class StreamingLocalAi(unittest.TestCase):
    def test_modern_backend_streams_in_isolated_process(self):
        service=(SRC/"LocalAiInferenceService.kt").read_text(encoding="utf-8")
        client=(SRC/"LocalAiClient.kt").read_text(encoding="utf-8")
        build=(ROOT/"app/build.gradle").read_text(encoding="utf-8")
        self.assertIn("org.nehuatl.llamacpp.LlamaAndroid",service)
        self.assertIn("engine.startEngine(config)",service)
        self.assertIn("engine.getFormattedChat(context, messages,",service)
        self.assertIn('"emit_partial_completion" to true',service)
        self.assertIn("engine.launchCompletion(",service)
        self.assertIn("MSG_TEXT = 5",service)
        self.assertIn("receiver.send(msg)",service)
        self.assertIn("onNativeToken(",service)
        self.assertIn("onText(partial)",client)
        self.assertIn('llamacpp-kotlin:0.4.0',build)
        self.assertNotIn('llama-android:0.1.1',build)

    def test_partial_scene_is_visible_and_never_committed(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("ТЕКСТ ЛОКАЛЬНОЙ МОДЕЛИ · ПРЕДПРОСМОТР",ui)
        self.assertIn("AiGameTurn.previewScene(accumulated)",ui)
        self.assertIn("aiTurnText.setText(SceneTypography.render(visible",ui)
        self.assertIn("AiGameTurn completed=AiGameTurn.parse(answer)",ui)
        self.assertIn("completed.apply(next,actionResolved)",ui)
        self.assertIn("next.aiTurn=completed",ui)
        self.assertIn("game.choose(c.action)",ui)
        self.assertIn('aiClient.generateTurn(token,model,true',ui)
        self.assertIn('aiClient.generateTurn(key,model,false',ui)

if __name__=="__main__": unittest.main()
