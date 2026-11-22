"""Regression contracts for wiki-verified Russian names, IDs and legacy saves."""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "app/src/main/java/dev/infend/skyrimtext"
MAP = (SRC / "SkyrimMapPoints.java").read_text(encoding="utf-8")
CATALOG = (SRC / "SkyrimMapCatalog.java").read_text(encoding="utf-8")
ATLAS = (SRC / "WorldAtlas.java").read_text(encoding="utf-8")
RPG = (SRC / "RpgContent.java").read_text(encoding="utf-8")
ITEMS = (SRC / "ItemCatalog.java").read_text(encoding="utf-8")
AI = (SRC / "AiStoryClient.java").read_text(encoding="utf-8")
GLOSSARY = (ROOT / "tools/map_names_ru.txt").read_text(encoding="utf-8")

# Native game location ID never changes when a Russian display name is corrected.
LOCATION_CASES = {
    "Broken Oar Grotto": ("Сломанное Весло", "Грот Сломанное Весло"),
    "Mzinchaleft": ("Руины Мзинчалефт", "Мзинчалефт"),
    "Goldenglow Estate": ("Золотое поместье", "Поместье «Златоцвет»"),
    "The Guardian Stones": ("Камень Хранитель", "Камни-хранители"),
    "Shor's Stone": ("Шорский Камень", "Камень Шора"),
    "Kynesgrove": ("Кинсгроув", "Роща Кин"),
    "Sarethi Farm": ("Саретийская ферма", "Ферма Сарети"),
    "Nightgate Inn": ("Таверна Ночные ворота", "Таверна «Ночные ворота»"),
}


class WikiRuNamesTests(unittest.TestCase):
    def test_geographic_sites_use_canonical_names_but_keep_ids(self):
        rows = [line.strip().strip('",') for line in MAP.splitlines()
                if line.strip().startswith('"skyrim-')]
        ids = [r.split("|", 1)[0] for r in rows]
        self.assertEqual(len(ids), len(set(ids)), "Do not break map point IDs")
        self.assertGreater(len(rows), 300)
        for english, (legacy, correct) in LOCATION_CASES.items():
            matches = [row for row in rows if len(row.split("|")) > 2 and
                       row.split("|")[2] == english]
            with self.subTest(location=english):
                self.assertEqual(len(matches), 1)
                self.assertEqual(matches[0].split("|")[1], correct)
                self.assertIn(f'alias("{legacy}","{english}")', CATALOG)

    def test_regenerated_map_names_remain_canonical(self):
        translations = dict(row.split("|", 1) for row in GLOSSARY.splitlines()
                            if row.strip() and not row.lstrip().startswith("#"))
        for english, (_, correct) in LOCATION_CASES.items():
            with self.subTest(location=english):
                self.assertEqual(translations.get(english), correct)

    def test_atlas_removes_bogus_duplicate_locations(self):
        self.assertNotIn('put("Пещера Чёрный Предел"', ATLAS)
        self.assertNotIn('put("Медоварня Хоннинга"', ATLAS)
        self.assertIn('if("Пещера Чёрный Предел".equals(name))name="Чёрный Предел"', ATLAS)
        self.assertIn('put("Поместье «Златоцвет»"', ATLAS)
        self.assertIn('put("Камни-хранители"', ATLAS)

    def test_racial_abilities_and_shouts_are_wiki_names(self):
        for legacy, correct in (
            ("Повеление животным", "Приказ животному"),
            ("Прилив адреналина", "Ярость боя"),
            ("Высокорождённый", "Высокое происхождение"),
            ("Кожа Хиста", "Кора хиста"),
            ("Шепот ауры", "Шёпот ауры"),
        ):
            with self.subTest(term=correct):
                self.assertNotIn(legacy, RPG)
                self.assertIn(correct, RPG)
        race_match = re.search(r'public static final String\[\] RACES=\{([^}]*)\}', RPG)
        self.assertIsNotNone(race_match)
        self.assertEqual(len(re.findall(r'"[^"]+"', race_match.group(1))), 10)

    def test_artifacts_show_wiki_name_but_keep_save_key(self):
        self.assertIn('if("Звезда Чёрной Азуры".equals(id))return "Чёрная Звезда"', ITEMS)
        self.assertIn('if("Молот Волендранг".equals(id))return "Волендранг"', ITEMS)
        self.assertIn('items.put(name,new Item(name,canonicalItemDisplayName(name)', ITEMS)
        self.assertIn('items.get("Звезда Чёрной Азуры")', ITEMS)
        self.assertIn('items.get("Молот Волендранг")', ITEMS)

    def test_ai_sources_are_told_to_preserve_canonical_names(self):
        self.assertIn("private static final String CANONICAL_RU_NAMES", AI)
        self.assertIn('+"Продолжай заданный префикс JSON', AI)
        self.assertIn('"+CANONICAL_RU_NAMES;', AI)


if __name__ == "__main__":
    unittest.main()
