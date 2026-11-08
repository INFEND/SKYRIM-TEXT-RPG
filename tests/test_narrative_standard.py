"""Long-form scene format, context coverage, and UI span preservation."""
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

def read(name):
    return (JAVA/name).read_text(encoding="utf-8")

class NarrativeContracts(unittest.TestCase):
    def test_all_playable_modes_are_expanded(self):
        engine=read("GameEngine.java")
        prose=read("SceneNarrative.java")
        self.assertIn("return SceneNarrative.expand(this,s);",engine)
        for mode in ("INTRO","WORLD","TRAVEL","QUEST","DRAGON","TALK",
                     "SHOP","EVENT","COMBAT","RESULT","DEFEAT"):
            self.assertIn('case "'+mode+'":',prose)
        self.assertIn("rich.choices.addAll(s.choices);",prose)
        self.assertIn("return rich;",prose)
        self.assertGreater(len(prose),12000)
        self.assertGreaterEqual(prose.count("dialogue("),12)

    def test_dialogue_style_and_clickable_items_both_survive(self):
        formatter=read("SceneTypography.java")
        view=read("MainActivity.java")
        self.assertIn('line.startsWith(">")',formatter)
        self.assertIn('line.replace("**","")',formatter)
        self.assertIn('new QuoteSpan(accent)',formatter)
        self.assertIn('Typeface.ITALIC',formatter)
        self.assertIn('new ForegroundColorSpan(0xffeeeae5)',formatter)
        self.assertIn('new QuoteSpan(accent)',formatter)
        self.assertIn('body.setText(SceneTypography.render(s.body,accent()));',view)
        self.assertIn('storyChoice(++number,c,()->',view)
        self.assertIn('SceneChoiceMeta.chanceColor(chance)',view)
        self.assertIn('renderStoryJournal();',view)
        self.assertIn('new android.text.SpannableStringBuilder(view.getText())',view)
        self.assertIn('makeItemsClickable(body,body.getText().toString())',view)

    def test_offline_narrative_does_not_invent_clock_or_weather(self):
        prose=read("SceneNarrative.java")
        self.assertNotIn("g.day%3==0",prose)
        self.assertNotIn("К вечеру воздух темнеет",prose)
        self.assertNotIn("дневной свет находит каждый след",prose)
        self.assertNotIn("В лунном свете чернила",prose)
        self.assertNotIn("мокрый снег, а вокруг",prose)

    def test_dragon_arc_has_six_independent_contextual_scenes(self):
        prose=read("SceneNarrative.java")
        self.assertIn("DRAGON_SCENES",prose)
        self.assertIn("g.progress.dragonStage",prose)
        self.assertIn("dialogue(part[2],part[3])",prose)
        self.assertIn('+"\\n\\n"+s.body+',prose)
        for landmark in ("Хелгена", "Ривервуду", "Драконьем Пределе",
                         "западной башни", "Айварстеде", "Высокого Хротгара"):
            self.assertIn(landmark,prose)
        self.assertNotIn('dialogue("Пожилой путник"',prose)

    def test_ai_adopts_same_longform_format_without_changing_game_rules(self):
        client=read("AiStoryClient.java")
        ui=read("MainActivity.java")
        self.assertIn('Меняй ритм длинных и коротких абзацев',client)
        self.assertIn('**Персонаж:**',client)
        self.assertIn('>**— Его реплика**',client)
        self.assertIn('только игровым движком',client)
        self.assertIn('SceneTypography.render(accumulated,accent())',ui)

if __name__=="__main__":
    unittest.main()
