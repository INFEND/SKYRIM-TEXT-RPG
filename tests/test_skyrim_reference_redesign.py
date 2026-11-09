"""Six user screenshots drive the full Skyrim-style Android menu contract."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/infend/skyrimtext"
RES=ROOT/"app/src/main/res"
def code(name):return (SRC/name).read_text(encoding="utf-8")

class SkyrimReferenceRedesign(unittest.TestCase):
    def test_common_theme_is_monochrome_and_cornered(self):
        ui=code("MainActivity.java")
        border=code("RuneFrameDrawable.java")
        back=code("SkyrimBackdropDrawable.java")
        rule=code("SkyrimRuleView.java")
        for text in ("PANEL=0xe806090b","TEXT=0xfff0f1ef",
                     "new SkyrimBackdropDrawable()","new SkyrimRuleView(",
                     "new RuneFrameDrawable("):
            self.assertIn(text,ui)
        self.assertIn("corner(c,x,y,1,1,corner)",border)
        self.assertIn("corner(c,right,bottom,-1,-1,corner)",border)
        self.assertIn("new LinearGradient(",back)
        self.assertIn("onDraw(Canvas",rule)

    def test_all_navigation_icons_are_native_and_transparent(self):
        icon=code("SkyrimIconView.java")
        app=(RES/"drawable/ic_app.xml").read_text(encoding="utf-8")
        ui=code("MainActivity.java")
        self.assertIn("Color.TRANSPARENT",icon)
        # The player's signed 48-image archive now supersedes the placeholder
        # programmatic line icons; their transparent WebP pixels must remain.
        self.assertIn('ASSET="icons/skyrim_icons_48.webp"',icon)
        self.assertIn('TOTAL=48',icon)
        self.assertIn("canvas.drawBitmap(b,crop,dest,paint)",icon)
        self.assertIn("BitmapFactory.decodeStream(input)",icon)
        self.assertIn('INDEX.put("shield",20)',icon)
        self.assertIn('String[] glyphs={"story","map","bag","codex","hero","journal","settings"}',ui)
        self.assertIn('No background path',app)
        self.assertNotIn('M0,0h108v108',app)

    def test_inventory_journal_and_skill_screens_follow_reference(self):
        ui=code("MainActivity.java")
        self.assertIn("HorizontalScrollView categoryScroll",ui)
        self.assertIn('String[] sorts={"Название","Вес","Цена"}',ui)
        self.assertIn('TextView price=text(String.valueOf(item.value)',ui)
        self.assertIn('focusedInventoryId=item.id',ui)
        self.assertIn('skyrimTopTabs(4)',ui)
        self.assertIn('skyrimTopTabs(3)',ui)
        self.assertIn('skyrimTopTabs(5)',ui)
        self.assertIn('"ЗАВЕРШЁННЫЕ ЗАДАНИЯ"',ui)
        self.assertIn('"ПРОВАЛЕННЫЕ ЗАДАНИЯ"',ui)
        self.assertIn('ProgressBar progress=new ProgressBar',ui)

    def test_wait_dialog_is_real_not_a_static_screenshot(self):
        ui=code("MainActivity.java")
        game=code("GameEngine.java")
        self.assertIn("private void showWaitDialog()",ui)
        self.assertIn("SeekBar slider=new android.widget.SeekBar",ui)
        self.assertIn("game.waitHours(slider.getProgress()+1)",ui)
        self.assertIn('if(!started||"COMBAT".equals(mode)||hours<1||hours>24)',game)
        self.assertIn('.put("hour",hour)',game)
        self.assertIn('j.optInt("hour",8)',game)

    def test_AI_and_offline_choices_share_skyrim_spacing_without_rule_changes(self):
        ui=code("MainActivity.java")
        self.assertIn("private void renderAiChoice(",ui)
        self.assertIn("private void storyChoice(",ui)
        self.assertIn("AiGameTurn.success(option.chance,die)",ui)
        self.assertIn("game.progress.chance(game,c.action)",ui)
        self.assertIn('СВОЙ ВАРИАНТ ОТВЕТА',ui)
        self.assertIn('new RuneFrameDrawable(PANEL,c.special?accent():BORDER,c.special)',ui)
        self.assertIn('new RuneFrameDrawable(PANEL,selected?accent():BORDER,selected)',ui)
        self.assertIn('selectedAiChoices.contains(number)',ui)
        self.assertNotIn('number==1?accent():BORDER',ui)

if __name__=="__main__":
    unittest.main()
