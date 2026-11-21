from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext/KatherineStory.java"

class KatherineUnavailableExitTest(unittest.TestCase):
    def test_exit_precedes_availability_guard(self):
        choose = SOURCE.read_text(encoding="utf-8").split("void choose(", 1)[1]
        self.assertLess(choose.index('if("KATH:exit".equals(action))'), choose.index('if(!enabled||!present(g))return;'))
        self.assertIn('if("KATH:exit".equals(action)){g.mode="WORLD";return;}', choose)

    def test_unavailable_scene_only_offers_exit(self):
        scene = SOURCE.read_text(encoding="utf-8").split("GameEngine.Scene scene(", 1)[1].split("switch(stage)", 1)[0]
        self.assertIn('if(!enabled||!present(g))return new GameEngine.Scene', scene)
        self.assertIn('.add("KATH:exit"', scene)
