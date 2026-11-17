"""A completed AI scene and its pending roll must be stored together."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class AiAtomicSaveTest(unittest.TestCase):
    def test_commit_before_exposing_scene(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        helper=ui.split("private void commitAiSave(",1)[1].split("private boolean storeAiPending",1)[0]
        self.assertIn('putString("save",serialized)',helper)
        for key in ("AI_PENDING_ACTION","AI_PENDING_DIE","AI_PENDING_CHECKS","AI_PENDING_SESSION"):
            self.assertIn(".remove("+key+")",helper)
        self.assertIn(".commit()",helper)
        completed=ui.split("AiGameTurn completed=AiGameTurn.parseModel",1)[1].split("catch(Exception ex)",1)[0]
        self.assertLess(completed.index("commitAiSave(next)"),completed.index("game=next"))
        self.assertLess(completed.index("game=next"),completed.index("enqueueLivingResult"))
    def test_previous_scene_precedes_optional_companion_context(self):
        ai=(SRC/"AiStoryClient.java").read_text(encoding="utf-8")
        prompt=ai.split("static String localUserPrompt(",1)[1].split("boolean isRunning()",1)[0]
        self.assertLess(prompt.index("Конец предыдущей сцены"),prompt.index("Инициатива Кэтрин"))
if __name__=="__main__":unittest.main()
