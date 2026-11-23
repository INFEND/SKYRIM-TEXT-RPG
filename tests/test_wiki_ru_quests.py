"""Quest title fidelity and backward-compatible inventory search by old IDs."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"
CAMPAIGN = (JAVA / "CanonicalCampaign.java").read_text(encoding="utf-8")
UI = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
ITEMS = (JAVA / "ItemCatalog.java").read_text(encoding="utf-8")


class WikiRuQuests(unittest.TestCase):
    def test_original_quest_titles_follow_russian_skyrim_wiki(self):
        stages = re.findall(r'new Stage\("([^"]+)","([^"]+)",', CAMPAIGN)
        self.assertEqual(len(stages), 33)
        self.assertEqual(stages[0][0], "На свободу!")
        self.assertEqual(stages[8][0], "Рог Юргена")
        self.assertEqual(stages[27][0], "Дом Пожирателя мира")
        self.assertEqual(stages[5][0], "Дракон в небе")
        self.assertEqual(stages[21][0], "Проклятие Алдуина")
        self.assertIn('public static final int COUNT=33;', CAMPAIGN)

    def test_legacy_and_canonical_items_both_appear_in_inventory_search(self):
        self.assertIn('!item.matches(query)', UI)
        self.assertIn('id.toLowerCase(Locale.ROOT).contains(needle)', ITEMS)
        self.assertIn('DISPLAY_ALIASES.putIfAbsent(visibleName,name)', ITEMS)
        self.assertIn('return legacyId==null?null:items.get(legacyId)', ITEMS)

    def test_equipment_artwork_lookup_uses_legacy_id(self):
        # Rewriting display labels must not make fallback icons or art disappear.
        self.assertIn('String n=item.id.toLowerCase(java.util.Locale.ROOT);', UI)
        self.assertIn('if(n.equals("эбонитовый меч"))file="ebony_sword"', UI)
        self.assertIn('else if(n.equals("эбонитовая кираса"))file="ebony_armor"', UI)


if __name__ == "__main__":
    unittest.main()
