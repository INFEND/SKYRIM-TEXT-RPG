from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext/AlmaStory.java"

class AlmaSceneAvailabilityTest(unittest.TestCase):
    def test_unavailable_scene_only_offers_exit(self):
        source = SOURCE.read_text(encoding="utf-8")
        scene = source.split("GameEngine.Scene scene(GameEngine g){", 1)[1]
        guard = scene.split("String stats=", 1)[0]
        self.assertIn("if(!enabled||terminalLoss()||(!atRiften(g)&&!companion))return", guard)
        self.assertIn('.add("ALMA:exit"', guard)
        self.assertEqual(guard.count(".add("), 1)

    def test_joint_dialogue_uses_shared_cooldown(self):
        source = SOURCE.read_text(encoding="utf-8")
        lines = [line for line in source.splitlines() if '"ALMA:katherine"' in line]
        self.assertEqual(len(lines), 2)
        for line in lines:
            self.assertIn("g.katherine.companion.lastJoint!=g.day", line)
        self.assertIn("g.katherine.companion.lastJoint=g.day", lines[1])
