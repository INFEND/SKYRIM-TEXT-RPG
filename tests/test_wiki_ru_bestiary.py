"""Russian Skyrim Wiki bestiary terms and backwards-compatible lookup."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
BESTIARY=(JAVA/"BestiaryCatalog.java").read_text(encoding="utf-8")
NPC=(JAVA/"NpcProfiles.java").read_text(encoding="utf-8")
AI=(JAVA/"AiStoryClient.java").read_text(encoding="utf-8")
MAPPINGS={
    "Морозный тролль":"Ледяной тролль",
    "Ледяной призрак":"Ледяное привидение",
    "Скелетный дракон":"Дракон-скелет",
    "Бронированный тролль":"Тролль в броне",
    "Горгулья":"Гаргулья",
    "Адская гончая":"Гончая смерти",
    "Туманник Каирна":"Туманный человек",
    "Гневный страж":"Яростный человек",
    "Оборотень-медведь":"Медведь-оборотень",
    "Сожжённый спригган":"Горелый спригган",
    "Пепельное порождение":"Порождение пепла",
}

class WikiRuBestiary(unittest.TestCase):
    def test_names_and_legacy_aliases(self):
        for old, new in MAPPINGS.items():
            with self.subTest(old=old):
                self.assertIn('"'+new+'|', BESTIARY)
                self.assertNotIn('"'+old+'|', BESTIARY)
                self.assertIn('alias("'+old+'","'+new+'");', BESTIARY)

    def test_search_resolves_saved_names(self):
        self.assertIn("if(ALIASES.containsKey(k))k=ALIASES.get(k);", BESTIARY)
        self.assertIn("if(ALIASES.containsKey(q))q=ALIASES.get(q);", BESTIARY)
        self.assertIn('alias("Порождения пепла","Порождение пепла")', BESTIARY)
        self.assertIn('alias("Снежный тролль","Ледяной тролль")', BESTIARY)

    def test_npc_templates_match_bestiary_without_losing_old_lookups(self):
        for old, new in [
            ("Морозный тролль","Ледяной тролль"),
            ("Бронированный тролль","Тролль в броне"),
            ("Пепельное порождение","Порождение пепла"),
        ]:
            with self.subTest(npc=new):
                self.assertIn('addTemplate("'+new+'"', NPC)
                self.assertIn('"'+old+'");', NPC)

    def test_narrator_and_loot_use_canonical_terms(self):
        self.assertIn("Гончая смерти", AI)
        self.assertIn("Порождение пепла", AI)
        self.assertIn("Зубы ледяного привидения", BESTIARY)
        self.assertNotIn("Зубы ледяного призрака", BESTIARY)

if __name__=="__main__":
    unittest.main()
