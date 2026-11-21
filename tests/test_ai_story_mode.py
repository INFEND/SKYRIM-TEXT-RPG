import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class AiStoryModeTests(unittest.TestCase):
    def test_client_is_narrative_only(self):
        body = (SRC / "AiStoryClient.java").read_text()
        self.assertIn("response.output_text.delta", body)
        self.assertIn('"store", false', body)
        self.assertIn("игровым движком", body)
        self.assertNotIn(".choose(", body)
        self.assertNotIn("new Thread", body)

    def test_encrypted_secret(self):
        body = (SRC / "AiKeyVault.java").read_text()
        self.assertIn("AndroidKeyStore", body)
        self.assertIn("AES/GCM/NoPadding", body)
        self.assertNotIn("sk-proj-", body)

    def test_memory_survives_process_death(self):
        body = (SRC / "AiStoryMemory.java").read_text()
        self.assertIn("SQLiteOpenHelper", body)
        self.assertIn("exportSession", body)
        self.assertIn("importSession", body)

    def test_original_game_rules_untouched(self):
        main = (SRC / "MainActivity.java").read_text()
        self.assertIn("game.choose(c.action)", main)
        self.assertIn("game.progress.observe(game,c.action,old)", main)
        self.assertIn("renderAiStoryPanel()", main)
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn("applicationId 'dev.infend.skyrimtext'", gradle)

if __name__ == "__main__":
    unittest.main()
