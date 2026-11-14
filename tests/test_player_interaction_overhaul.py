"""Regression contracts for the player-requested 1.0.28 interaction overhaul."""
from pathlib import Path
import unittest

BASE = Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext"
UI = (BASE / "MainActivity.java").read_text(encoding="utf-8")
MAP = (BASE / "WorldAtlas.java").read_text(encoding="utf-8")
TYPO = (BASE / "SceneTypography.java").read_text(encoding="utf-8")
CLIENT = (BASE / "AiStoryClient.java").read_text(encoding="utf-8")


class PlayerInteractionOverhaul(unittest.TestCase):
    def test_one_or_many_actions_only_submitted_by_button(self):
        block = UI.split("private void renderAiChoice(", 1)[1].split("All effects are committed", 1)[0]
        self.assertIn("selectedAiChoices.add(number)", block)
        self.assertIn("selectedAiChoices.remove(number)", block)
        self.assertNotIn("requestAiTurn(", block)
        self.assertNotIn("number==1?", block)
        self.assertIn('actionButton("ОТПРАВИТЬ"', UI)
        self.assertIn("java.util.Collections.sort(indexes)", UI)
        self.assertIn('СОБСТВЕННЫЙ ТЕКСТ ИГРОКА:', UI)
        self.assertIn("persistAiDraft()", UI)
        self.assertIn("restoreAiDraft()", UI)

    def test_location_gallery_is_present_during_ai_generation(self):
        ai = UI.split("private void renderAiGame()", 1)[1].split("private void renderAiChoice(", 1)[0]
        self.assertIn("LocationGalleryView.hasGallery(game.progress.location)", ai)
        self.assertIn("sceneGallery(game.progress.location)", ai)
        self.assertIn("LocationGalleryView gallery=new LocationGalleryView(this,location,accent(),false)", UI)
        self.assertLess(ai.index("LocationGalleryView.hasGallery("), ai.index("if(aiTurnBusy)"))

    def test_map_filter_hides_labels_and_click_targets(self):
        self.assertIn("renderMapTypeFilters()", UI)
        self.assertIn('type.equals("Столица")?"Города"', UI)
        self.assertIn('type.equals("Деревня")?"Деревни"', UI)
        self.assertIn("hiddenMapTypes.addAll(types)", UI)
        self.assertIn("locations.removeIf(site->hiddenMapTypes.contains(site.type)", UI)
        self.assertIn("Set<String> hiddenTypes", MAP)
        self.assertEqual(MAP.count("if(!visible(p))continue;"), 2)
        self.assertIn("place.name.equals(game.progress.location)||", MAP)

    def test_setting_sections_are_animated_and_persisted(self):
        for kind in ("appearance", "ai", "updates", "saves"):
            self.assertIn(f'foldSettingsSection("{kind}"', UI)
        self.assertIn("android.transition.AutoTransition().setDuration(230)", UI)
        self.assertIn('prefs.edit().putBoolean("settings_group_"+key,expanded)', UI)

    def test_spacing_reasoning_summary_and_truthful_progress(self):
        self.assertIn("if(spoken&&!lines[i+1].trim().isEmpty())result.append", TYPO)
        self.assertIn("Unclosed markup is not a license", TYPO)
        self.assertIn("spinner.setIndeterminate(true)", UI)
        self.assertIn("aiProgressInfo.setText", UI)
        self.assertIn("onReasoningSummary(String summary)", CLIENT)
        self.assertIn('response.reasoning_summary_text.delta', CLIENT)
        self.assertIn('reasoning.put("summary","auto")', CLIENT)
        self.assertNotIn("response.reasoning_text.delta", CLIENT)


if __name__ == "__main__":
    unittest.main()

