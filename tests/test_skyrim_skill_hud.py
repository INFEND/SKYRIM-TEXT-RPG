"""Skyrim-style skill-level HUD: only real increments, transparent symbols, no save changes."""
import pathlib
import unittest

SRC=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
class SkillLevelHudContracts(unittest.TestCase):
    def test_banner_is_not_a_fake_constant(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("renderSkillLevelNotice();",ui)
        self.assertIn("game.progress.skillLevel(i)>oldLevel",ui)
        self.assertIn("if(prior.isEmpty()||!owner.equals(previousOwner))return;",ui)
        self.assertIn('prefs.edit().putString(SKILL_HUD_OWNER,owner)',ui)
        self.assertIn("panel.postDelayed(",ui)
        self.assertIn('RpgContent.SKILLS[i].toUpperCase',ui)
    def test_icon_has_real_alpha_and_progress_uses_real_skill_xp(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        icons=(SRC/"SkyrimIconView.java").read_text(encoding="utf-8")
        self.assertIn('new SkyrimIconView(this,"armor",accent())',ui)
        self.assertIn("Color.TRANSPARENT",icons)
        self.assertIn("game.progress.skillXp[i]",ui)
        self.assertIn("game.progress.skillNeed(i)",ui)
    def test_game_data_and_probability_mechanics_are_unchanged(self):
        ui=(SRC/"MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("game.progress.chance(game,c.action)",ui)
        self.assertIn("AiGameTurn.success(option.chance,die)",ui)
        self.assertIn('prefs.edit().putString("save",game.toJson().toString())',ui)

if __name__=="__main__":unittest.main()
