"""Safety and consistency guards for the Nordic redesign (no Android runtime required)."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"


class DarkFantasyUiContract(unittest.TestCase):
    def test_title_story_map_codex_all_have_shared_visual_language(self):
        main = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        for symbol in ("renderTitle()", "renderNewGame()", "renderStory()",
                       "renderMap()", "renderCodex()", "renderInventory()",
                       "renderHero()", "renderJournal()", "renderSettings()"):
            self.assertIn(symbol, main)
        self.assertIn("new NordicArtView(", main)
        self.assertIn("new RuneFrameDrawable(", main)
        self.assertIn('"Бестиарий","Народы","История","Места"', main)
        self.assertIn('"Новая игра"', main)
        self.assertIn('"Продолжить"', main)
        self.assertIn('"О игре"', main)
        self.assertIn("if(game.started)", main)
        self.assertIn("Существующее сохранение будет заменено", main)

    def test_drawable_and_scenery_offline(self):
        art = (JAVA / "NordicArtView.java").read_text(encoding="utf-8")
        frame = (JAVA / "RuneFrameDrawable.java").read_text(encoding="utf-8")
        self.assertIn("extends View", art)
        self.assertIn("onDraw(Canvas", art)
        self.assertIn("new Random(71+mode*41)", art)
        self.assertIn("extends Drawable", frame)
        self.assertNotIn("http://", art + frame)
        self.assertNotIn("https://", art + frame)

    def test_legacy_ota_and_saves_untouched(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        contract = (ROOT / "tools/build_manifest.py").read_text(encoding="utf-8")
        self.assertIn("applicationId 'dev.infend.skyrimtext'", gradle)
        self.assertIn('"dev.xarkon.link"', contract)
        self.assertIn('"dev.infend.skyrimtext"', contract)
        self.assertIn('"legacyPackageField": True', contract)
        main = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn('prefs.getString("save",null)', main)
        self.assertIn('prefs.edit().putString("save"', main)

    def test_theme_and_map_are_accessible_and_not_fake_exact_map(self):
        style = (ROOT / "app/src/main/res/values/styles.xml").read_text(encoding="utf-8")
        atlas = (JAVA / "WorldAtlas.java").read_text(encoding="utf-8")
        self.assertIn('<item name="android:fontFamily">serif</item>', style)
        self.assertIn("УСЛОВНАЯ ИГРОВАЯ СХЕМА", atlas)
        self.assertIn("performClick()", atlas)
        self.assertIn("isPlayer", atlas)

if __name__ == "__main__":
    unittest.main()
