"""Regression guards for the four player-reported v1.0.34 presentation bugs."""
from pathlib import Path
import unittest

BASE=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
UI=(BASE/"MainActivity.java").read_text(encoding="utf-8")
TYPO=(BASE/"SceneTypography.java").read_text(encoding="utf-8")
AI=(BASE/"AiStoryClient.java").read_text(encoding="utf-8")


class PlayerScreens134(unittest.TestCase):
    def test_header_scrolls_as_first_child_without_overlay(self):
        main=UI.split("private void render(){",1)[1].split("private void renderHeader(){",1)[0]
        self.assertIn("scroll.addView(page,new ScrollView.LayoutParams(-1,-2))",main)
        self.assertIn("page.addView(topChrome,lp(-1,-2))",main)
        self.assertIn("page.addView(content,lp(-1,-2))",main)
        self.assertLess(main.index("page.addView(topChrome"),main.index("page.addView(content"))
        self.assertNotIn("topChrome.setTranslationY",UI)
        self.assertNotIn("scroll.setClipBounds",UI)
        self.assertNotIn("handleVerticalScroll",UI)
        self.assertIn("resumeViewport(oldScroll,scroll,thisPage)",main)

    def test_player_speaker_is_named_and_customizable_gold_default(self):
        self.assertIn('heroSpeaker=speaker&&plain.matches("(?iu)ты',TYPO)
        self.assertIn("heroSpeaker?heroColor:accent",TYPO)
        self.assertIn('line=line.startsWith("**")?"**"+safe+":**":safe+":"',TYPO)
        self.assertIn("SceneTypography.render(game.aiTurn.scene,accent(),game.name,heroColor())",UI)
        self.assertIn("SceneTypography.render(s.body,accent(),game.name,heroColor())",UI)
        self.assertIn("0xffdeb66d",UI)
        self.assertIn('prefs.getInt("hero_name_color",0)',UI)
        self.assertIn('prefs.edit().putInt("hero_name_color",selectedColor)',UI)
        self.assertIn("его настоящее имя из игровых данных вместо «Ты:»",AI)

    def test_choices_use_skill_atlas_and_optional_centered_numbers(self):
        self.assertIn('prefs.getBoolean("show_choice_numbers",true)',UI)
        self.assertIn("numberToggle.setText(\"Показывать номера вариантов ответа\")",UI)
        self.assertIn('TextView ordinal=text(String.valueOf(position)',UI)
        self.assertIn('TextView ordinal=showChoiceNumbers()?text(String.valueOf(number)',UI)
        self.assertNotIn('text("◇ "+position',UI)
        self.assertNotIn('"◇ ")+number',UI)
        self.assertIn('SkyrimIconAtlas.exactLabel(this,clean)',UI)
        self.assertIn("box.setGravity(Gravity.CENTER_VERTICAL)",UI)
        self.assertIn("row.setGravity(Gravity.CENTER_VERTICAL)",UI)
        self.assertIn('row.setContentDescription("Вариант "+number',UI)

    def test_ai_loading_has_only_spinner_live_dots_and_stop_action(self):
        busy=UI.split("if(aiTurnBusy) {",1)[1].split('if(!aiTurnStatus.isEmpty())addText(content,aiTurnStatus',1)[0]
        self.assertIn("spinner.setIndeterminate(true)",busy)
        self.assertIn('private final String[] frames={"",".","..","..."}',busy)
        self.assertIn('aiProgressInfo.setText("Генерация ответа"+frames[phase++%frames.length])',busy)
        self.assertIn("aiProgressInfo.postDelayed(this,430L)",busy)
        self.assertIn('actionButton("ОСТАНОВИТЬ"',busy)
        self.assertNotIn('Резюме рассуждений',busy)
        self.assertNotIn('Точный процент недоступен',busy)
        self.assertNotIn('Создано символов сцены',busy)
        self.assertNotIn('ИИ СОЗДАЁТ ПРОДОЛЖЕНИЕ',busy)
        self.assertIn("aiProgressInfo.removeCallbacks(aiDotsTicker)",UI)


if __name__=="__main__":
    unittest.main()
