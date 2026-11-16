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

    def test_terminal_save_clears_relationship_flags(self):
        source = SOURCE.read_text(encoding="utf-8")
        restore = source.split("static AlmaStory restore(", 1)[1].split("GameEngine.Scene scene(", 1)[0]
        self.assertIn("if(a.terminalLoss()){a.companion=false;a.romance=false;a.married=false;}", restore)
        self.assertLess(restore.index("a.ending="), restore.index("if(a.terminalLoss())"))

    def test_ai_context_respects_terminal_ending(self):
        source = SOURCE.read_text(encoding="utf-8")
        context = source.split("String aiContext(){", 1)[1].split("static int clamp", 1)[0]
        self.assertIn("if(terminalLoss())return", context)
        self.assertIn("не вводи её как живого персонажа", context)
        self.assertIn("не вводи её как спутницу или возлюбленную", context)
        self.assertLess(context.index("if(terminalLoss())"), context.index("спутница "))
