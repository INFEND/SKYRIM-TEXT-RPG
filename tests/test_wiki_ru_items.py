"""Preserve historical item keys while showing wiki-verified Skyrim RU names."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"
CATALOG = (JAVA / "ItemCatalog.java").read_text(encoding="utf-8")
WORLD = (JAVA / "WorldItemCatalog.java").read_text(encoding="utf-8")
APPAREL = (JAVA / "ApparelAndPotionCatalog.java").read_text(encoding="utf-8")
RPG = (JAVA / "RpgContent.java").read_text(encoding="utf-8")
AI = (JAVA / "AiStoryClient.java").read_text(encoding="utf-8")

# Old keys must NOT be mass-renamed in source or in players' saved inventories.
ALIASES = {
    "Пурпурный горноцвет": "Лиловый горноцвет",
    "Грибы имп": "Бесовский гриб",
    "Крыло голубой бабочки": "Крыло синей бабочки",
    "Голубой стрекозник": "Синяя стрекоза",
    "Оранжевый стрекозник": "Оранжевая стрекоза",
    "Усики коруса-охотника": "Усик коруса-охотника",
    "Ястребиное яйцо": "Яйцо ястреба",
    "Яйцо рыбы-убийцы": "Икра рыбы-убийцы",
    "Яйцо паука": "Паучье яйцо",
    "Повреждённая Звезда Азуры": "Сломанная Звезда Азуры",
    "Малый камень душ (заполненный)": "Маленький камень душ (заполненный)",
    "Кольцо серебряное": "Серебряное кольцо",
    "Кольцо золотое": "Золотое кольцо",
}


class WikiRuItemsTests(unittest.TestCase):
    def test_all_verified_aliases_are_display_only(self):
        self.assertIn("id=key;name=n;", CATALOG)
        self.assertIn("items.put(name,new Item(name,canonicalItemDisplayName(name)", CATALOG)
        self.assertIn("Item existing=items.get(id);", CATALOG)
        self.assertIn("if(existing!=null)return existing;", CATALOG)
        for old, canon in ALIASES.items():
            with self.subTest(old=old):
                self.assertIn(f'if("{old}".equals(id))', CATALOG)
                self.assertIn(f'return "{canon}";', CATALOG)
                if old != "Повреждённая Звезда Азуры":
                    self.assertIn(f'items.get("{old}")', CATALOG)

    def test_legacy_items_remain_registered_for_old_saves(self):
        source = WORLD + "\n" + APPAREL + "\n" + CATALOG
        for old in ALIASES:
            with self.subTest(item=old):
                self.assertIn(f'"{old}"', source)
        self.assertIn('registerMissing("Повреждённая Звезда Азуры"', CATALOG)

    def test_language_and_ai_rules_follow_skyrim_wiki(self):
        self.assertIn('ATTRIBUTES={"Здоровье","Магия","Запас сил"}', RPG)
        for name in ("Лиловый горноцвет", "Бесовский гриб", "Крыло синей бабочки",
                     "Яйцо ястреба", "Паучье яйцо", "Икра рыбы-убийцы"):
            self.assertIn(name, AI)
        self.assertIn("Ресурс выносливости называется Запас сил", AI)
        # Skill indices are save keys: never rearrange the 18 skills.
        match = re.search(r'public static final String\[\] SKILLS=\{(.*?)\};',
                          RPG, re.S)
        self.assertIsNotNone(match)
        self.assertEqual(len(re.findall(r'"[^"]+"', match.group(1))), 18)


if __name__ == "__main__":
    unittest.main()
