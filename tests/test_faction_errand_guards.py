from pathlib import Path
import unittest

SOURCE = Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext/FactionCampaign.java"

class FactionErrandGuards(unittest.TestCase):
    def test_only_world_and_result_allow_errands(self):
        code = SOURCE.read_text(encoding="utf-8")
        block = code.split("String block(GameEngine g,int f){", 1)[1].split("boolean perform", 1)[0]
        self.assertIn('!("WORLD".equals(g.mode)||"RESULT".equals(g.mode))', block)
        self.assertIn('if(!valid(f)||!g.started)', block)

    def test_allegiance_guard_applies_at_every_stage(self):
        code = SOURCE.read_text(encoding="utf-8")
        block = code.split("String block(GameEngine g,int f){", 1)[1].split("boolean perform", 1)[0]
        self.assertIn('if((f==4&&stage[5]>0)||(f==5&&stage[4]>0)||(f==4&&g.factionStories.started(5))||(f==5&&g.factionStories.started(4)))', block)

    def test_blocked_actions_have_no_chance_and_cannot_execute(self):
        code = SOURCE.read_text(encoding="utf-8")
        self.assertIn('s>TASK_COUNT||!block(g,f).isEmpty()?-1', code)
        self.assertIn('if(!block(g,f).isEmpty())return false;', code)

if __name__ == "__main__":
    unittest.main()
