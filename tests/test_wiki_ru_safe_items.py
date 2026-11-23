"""Wiki-backed display names without canonical-name collisions or save migrations."""
from pathlib import Path
import unittest

JAVA=Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
CATALOG=(JAVA/"ItemCatalog.java").read_text(encoding="utf-8")
RPG=(JAVA/"RpgContent.java").read_text(encoding="utf-8")
AI=(JAVA/"AiStoryClient.java").read_text(encoding="utf-8")
DATA="\n".join((JAVA/n).read_text(encoding="utf-8") for n in (
    "ItemCatalog.java","WorldItemCatalog.java","ApparelAndPotionCatalog.java",
    "CommonItemCatalog.java"))
SAFE={
    "Оранжевый стрекозник":"Оранжевая стрекоза",
    "Ястребиное яйцо":"Яйцо ястреба",
    "Яйцо рыбы-убийцы":"Икра рыбы-убийцы",
    "Яйцо паука":"Паучье яйцо",
    "Кольцо серебряное":"Серебряное кольцо",
    "Кольцо золотое":"Золотое кольцо",
}
class WikiSafeItemNames(unittest.TestCase):
    def test_six_display_names_preserve_historical_keys(self):
        for old, canonical in SAFE.items():
            with self.subTest(item=old):
                self.assertIn('case "'+old+'": return "'+canonical+'";', CATALOG)
                self.assertIn('"'+old+'"', DATA)
                self.assertNotIn('add("'+canonical+'"', DATA)
        self.assertIn("DISPLAY_ALIASES.putIfAbsent(visibleName,name)",CATALOG)
        self.assertIn("String legacyId=DISPLAY_ALIASES.get(id)",CATALOG)

    def test_conflicting_canonical_names_are_not_double_mapped(self):
        # These two visible names already exist as separate inventory/save keys.
        self.assertNotIn('case "Повреждённая Звезда Азуры": return "Сломанная Звезда Азуры";', CATALOG)
        self.assertNotIn('case "Малый камень душ (заполненный)": return "Маленький камень душ (заполненный)";', CATALOG)

    def test_stamina_label_and_ai_terms(self):
        self.assertIn('ATTRIBUTES={"Здоровье","Магия","Запас сил"}', RPG)
        self.assertIn("Икра рыбы-убийцы", AI)
        self.assertIn("Грозовая стена", AI)
        self.assertIn("Третий ресурс героя: Запас сил.", AI)

if __name__=="__main__":
    unittest.main()
