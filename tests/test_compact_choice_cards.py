"""Regression checks for dynamic, compact, selectable choice cards."""
import pathlib
import unittest
UI=(pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")

class CompactChoiceCards(unittest.TestCase):
    def test_game_scale_and_touch_target(self):
        for phrase in ("return Math.max(10,size+fontBonus)",
                       "choicePadX(){return Math.max(8,10+fontBonus/2)",
                       "choicePadY(){return Math.max(6,8+fontBonus/2)",
                       "choiceIconSize(){return Math.max(20,24+fontBonus)"):
            self.assertIn(phrase,UI)
        self.assertGreaterEqual(UI.count("setMinimumHeight(dp(48))"),3)

    def test_native_ai_and_live_cards(self):
        for phrase in ('text(c.title,16,TEXT,true)',
                       'text(c.detail,14,MUTED,false)',
                       'text(option.title,16,TEXT,true)',
                       'text(option.detail,14,MUTED,false)',
                       'TextView title=text("",16,TEXT,true)',
                       'compactChoiceText(title);compactChoiceText(detail);compactChoiceText(meta)',
                       'choiceChanceBar(detail,displayedChance',
                       'choiceChanceBar(prose,chance',
                       'showChoiceSkillLevel(detail,option.skill',
                       'selectedAiChoices.add(number)',
                       'aiPreviewSelected.add(candidate.title)'):
            self.assertIn(phrase,UI)

if __name__=="__main__":
    unittest.main()
