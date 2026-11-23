"""Compound atlas names are displayed with a colon without breaking old saves."""
import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext"
ROWS=(ROOT/"SkyrimMapPoints.java").read_text(encoding="utf-8")
CATALOG=(ROOT/"SkyrimMapCatalog.java").read_text(encoding="utf-8")

class AtlasColonLabels(unittest.TestCase):
    def test_city_subsites_keep_id_and_readable_name(self):
        for label in ("Вайтран: Конюшни","Маркарт: Конюшни",
                      "Солитьюд: Лесопилка","Вайтран: Лагерь Братьев Бури"):
            self.assertIn(label,ROWS)
            self.assertNotIn(label.replace(": "," - "),ROWS)
        self.assertIn("Дрифтшейд — Приют",ROWS)
        self.assertIn("Снежная Завеса — Святилище",ROWS)
        self.assertIn('INDEX.putIfAbsent(normalize(site.name.replace(": "," - ")),site)',CATALOG)

if __name__=="__main__":unittest.main()
