"""Wiki-based Russian display names must not mutate stable item IDs."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"
CATALOG = (JAVA / "ItemCatalog.java").read_text(encoding="utf-8")
WORLD = (JAVA / "WorldItemCatalog.java").read_text(encoding="utf-8")

ARMOR = {
    "Железная кираса": "Железная броня",
    "Стальная кираса": "Стальная броня",
    "Орочья кираса": "Орочья броня",
    "Двемерская кираса": "Двемерская броня",
    "Эльфийская кираса": "Эльфийская броня",
    "Стеклянная кираса": "Стеклянная броня",
    "Эбонитовая кираса": "Эбонитовая броня",
    "Даэдрическая кираса": "Даэдрическая броня",
    "Драконья кираса": "Драконья панцирная броня",
    "Нордская резная кираса": "Нордская резная броня",
    "Сталгримовая кираса": "Сталгримовая броня",
    "Кожаная кираса": "Кожаная броня",
    "Кираса из шкур": "Сыромятная броня",
    "Сапоги из шкур": "Сыромятные сапоги",
    "Наручи из шкур": "Сыромятные наручи",
    "Шлем из шкур": "Сыромятный шлем",
    "Щит из шкур": "Сыромятный щит",
    "Стальная пластинчатая кираса": "Стальная пластинчатая броня",
    "Костяная кираса": "Костяная броня",
    "Тяжёлая хитиновая кираса": "Тяжёлая хитиновая броня",
    "Драконья чешуйчатая кираса": "Драконья чешуйчатая броня",
    "Драконий шлем": "Драконий панцирный шлем",
    "Драконьи перчатки": "Драконьи панцирные перчатки",
    "Драконьи сапоги": "Драконьи панцирные сапоги",
    "Драконий щит": "Драконий панцирный щит",
}

INGREDIENTS = {
    "Грибы имп": "Бесовский гриб",
    "Голубой стрекозник": "Синяя стрекоза",
    "Корень каниса": "Собачий корень",
    "Драконий язык": "Язык дракона",
    "Крыло голубой бабочки": "Крыло синей бабочки",
    "Пурпурный горноцвет": "Лиловый горноцвет",
    "Усики коруса-охотника": "Усик коруса-охотника",
}


DRAGONBONE = {
    "Драконий меч": "Драконий костяной меч",
    "Драконий боевой топор": "Драконий костяной боевой топор",
    "Драконий кинжал": "Драконий костяной кинжал",
    "Драконья булава": "Драконья костяная булава",
    "Драконий двуручный меч": "Драконий костяной двуручный меч",
    "Драконья секира": "Драконья костяная секира",
    "Драконий боевой молот": "Драконий костяной боевой молот",
    "Драконий лук": "Драконий костяной лук",
    "Драконья стрела": "Драконья костяная стрела",
}


class WikiRuEquipmentTest(unittest.TestCase):
    def test_exact_lookup_has_all_armor_and_ingredient_names(self):
        actual = dict(re.findall(r'case "([^"]+)": return "([^"]+)";', CATALOG))
        for old, new in {**ARMOR, **INGREDIENTS, **DRAGONBONE}.items():
            with self.subTest(old=old):
                self.assertEqual(actual.get(old), new)

    def test_legacy_catalog_entries_and_generated_equipment_unchanged(self):
        # The original name is still the inventory ID, visible labels are separate.
        self.assertIn("items.put(name,new Item(name,visibleName", CATALOG)
        self.assertIn('DISPLAY_ALIASES.putIfAbsent(visibleName,name)', CATALOG)
        self.assertIn('return legacyId==null?null:items.get(legacyId)', CATALOG)
        self.assertIn('static synchronized void registerCustom(String id,String name', CATALOG)
        for name in ("Кожаная кираса", "Кираса из шкур",
                     "Драконья чешуйчатая кираса", "Костяная кираса",
                     "Стальная пластинчатая кираса", "Тяжёлая хитиновая кираса",
                     "Грибы имп", "Пурпурный горноцвет", "Крыло голубой бабочки"):
            with self.subTest(legacy_id=name):
                self.assertIn('"' + name + '"', CATALOG)
        for old in ("Голубой стрекозник", "Корень каниса", "Драконий язык",
                    "Усики коруса-охотника"):
            with self.subTest(legacy_id=old):
                self.assertIn('"' + old + '"', WORLD)

    def test_visible_new_names_are_one_to_one(self):
        names = [*ARMOR.values(), *INGREDIENTS.values(), *DRAGONBONE.values()]
        self.assertEqual(len(names), len(set(names)),
                         "Two different historical item IDs must not share a new display name")

    def test_dragonbone_family_stays_generated_under_historic_ids(self):
        self.assertIn('"Драконий"', CATALOG)
        self.assertIn('"Драконья"', CATALOG)
        self.assertIn('String name=metals[i]+" "+kinds[k]', CATALOG)
        self.assertIn('put(arrows[i]+" стрела"', CATALOG)
        self.assertIn('return legacyId==null?null:items.get(legacyId)', CATALOG)

    def test_armor_generator_keeps_actual_stats_and_skill(self):
        self.assertIn('put(feminine[i]+" кираса"', CATALOG)
        self.assertIn('armor(prefix+" шлем"', CATALOG)
        self.assertIn('armor(plural[i]+" сапоги"', CATALOG)
        self.assertIn('armor(metals[i]+" щит"', CATALOG)


if __name__ == "__main__":
    unittest.main()
