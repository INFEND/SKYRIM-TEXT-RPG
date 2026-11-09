"""Scene ledger is save-compatible and only engine-confirmed facts reach the AI prompt."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class SceneLedgerContextTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.scene = (JAVA / "SceneState.java").read_text(encoding="utf-8")
        cls.client = (JAVA / "AiStoryClient.java").read_text(encoding="utf-8")
        cls.engine = (JAVA / "GameEngine.java").read_text(encoding="utf-8")
        cls.turn = (JAVA / "AiGameTurn.java").read_text(encoding="utf-8")

    def test_old_saves_are_compatible(self):
        self.assertIn('SceneState.fromJson(j.optJSONObject("sceneState"))', self.engine)
        self.assertIn('.put("sceneState",sceneState.toJson())', self.engine)
        self.assertIn('if(data==null)return state', self.scene)

    def test_context_reads_confirmed_facts(self):
        self.assertIn("game.sceneState.confirmedContext()", self.client)
        self.assertIn("ПОДТВЕРЖДЁННЫЕ ФАКТЫ СЦЕНЫ", self.client)
        self.assertIn('collection(this,kind)', self.scene)
        self.assertIn('entry.optString("status")', self.scene)

    def test_ai_cannot_directly_write_scene_ledger(self):
        self.assertNotRegex(self.turn, r'\.sceneState\s*\.\s*confirm\s*\(')
        self.assertNotRegex(self.client, r'\.sceneState\s*\.\s*confirm\s*\(')
        self.assertNotIn('sceneState.confirm(', self.client)
        self.assertIn('Only explicit, validated engine-side confirmations', self.scene)

    def test_ledger_is_bounded(self):
        self.assertIn('MAX_ENTRIES = 32', self.scene)
        self.assertIn('if(arr.length()>MAX_ENTRIES)', self.scene)

if __name__ == "__main__":
    unittest.main()
