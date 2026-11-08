"""Make source of models/effort explicit: Work/Codex plan vs Chat vs API."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
MAIN=(ROOT/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java")
BUILD=(ROOT/"app/build.gradle")

class AiSourceClarity(unittest.TestCase):
    def test_subscription_source_labels_are_honest(self):
        code=MAIN.read_text(encoding="utf-8")
        self.assertIn("лимиты «Работа / Codex», НЕ лимиты обычного «Чата»",code)
        self.assertIn("ChatGPT · Работа/Codex",code)
        self.assertIn("Модель · Работа/Codex",code)
        self.assertIn("Источник: ChatGPT · лимиты Работа/Codex",code)
        self.assertIn("Обычный «Чат» и его личные настройки не считываются",code)
    def test_effort_selected_in_app_not_synced(self):
        code=MAIN.read_text(encoding="utf-8")
        self.assertIn("Уровень размышления · в игре",code)
        self.assertIn("не копируется из Чата/Работы",code)
        self.assertIn("Это не синхронизация ползунка из ChatGPT",code)
        self.assertIn("selectedReasoningEffort()",code)
        self.assertIn('aiClient.generate(token,selectedModel,true,reasoningEffort',code)
        self.assertIn('aiClient.generate(key,selectedModel,false,reasoningEffort',code)
    def test_original_game_and_ota_stable(self):
        code=MAIN.read_text(encoding="utf-8")
        self.assertIn("game.choose(c.action)",code)
        gradle=BUILD.read_text(encoding="utf-8")
        self.assertIn("applicationId 'dev.infend.skyrimtext'",gradle)

if __name__=="__main__":unittest.main()
