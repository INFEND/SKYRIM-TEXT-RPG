"""The uploaded Skyrim screenshots define the cinematic AI response contract."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
def java(name):
    return (SRC/name).read_text(encoding="utf-8")

class ScreenshotNarrativeContract(unittest.TestCase):
    def test_ai_has_one_coherent_long_scene_target(self):
        code=java("AiStoryClient.java")
        for key in ("от второго лица","6000–14000 знаков",
                    "Меняй ритм длинных и коротких абзацев",
                    "**Персонаж:**","**— Его реплика**",
                    "только игровым движком",
                    "пронумерованные действия игрока"):
            self.assertIn(key,code)
        self.assertNotIn("Старайся писать 1000-2000 знаков",code)
        self.assertNotIn("Уместный объём 2500-5000 знаков",code)

    def test_real_character_state_and_chance_is_used(self):
        code=java("AiStoryClient.java")
        for key in ("g.progress.carryWeight()","g.progress.inventory.keys()",
                    "g.enemyHp","g.questDetails()","DragonArc.status(g)",
                    "g.progress.chance(g,c.action)","SceneChoiceMeta.skill(c.action)"):
            self.assertIn(key,code)
        self.assertIn('if("COMBAT".equals(g.mode))',code)

    def test_long_scenes_not_truncated_in_sqlite(self):
        code=java("AiStoryMemory.java")
        self.assertIn("STORED_TURN_LIMIT=32000",code)
        self.assertIn("Math.min(STORED_TURN_LIMIT, body.length())",code)
        self.assertIn("Math.min(body.length(), STORED_TURN_LIMIT)",code)
        self.assertNotIn("Math.min(6000, body.length())",code)
        self.assertIn('Collections.reverse(newest)',code)
        self.assertIn('"id DESC","26"',code)
        self.assertIn("if(used+line.length()+1>limit)break;",code)

    def test_streaming_and_interactive_engine_actions_remain(self):
        client=java("AiStoryClient.java")
        ui=java("MainActivity.java")
        effort=java("AiReasoningEffort.java")
        self.assertIn("response.output_text.delta",client)
        self.assertIn("SceneTypography.render(accumulated,accent(),game.name,heroColor())",ui)
        self.assertIn("storyChoice(++number,c,()->",ui)
        self.assertIn("game.choose(c.action)",ui)
        self.assertIn('case "medium":return 12000',effort)
        self.assertIn('case "max":return 32000',effort)
        self.assertIn("reply.setLineSpacing(dp(6),1.16f)",ui)

if __name__=="__main__":
    unittest.main()
