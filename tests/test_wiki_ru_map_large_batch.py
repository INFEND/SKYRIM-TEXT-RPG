"""Checks 19 canonical Russian map names and historic ID compatibility."""
import json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SkyrimMapPoints.java").read_text(encoding="utf-8")
MAP=json.loads((ROOT/"tools/map_source/compiled.json").read_text(encoding="utf-8"))
BASE=json.loads((ROOT/"tools/map_source/reference_names_ru.json").read_text(encoding="utf-8"))
GLOSS=dict(s.split("|",1) for s in (ROOT/"tools/map_names_ru.txt").read_text(encoding="utf-8").splitlines() if "|" in s and not s.startswith("#"))
CAT=(ROOT/"app/src/main/java/dev/infend/skyrimtext/SkyrimMapCatalog.java").read_text(encoding="utf-8")
GEN=(ROOT/"tools/build_geographic_atlas.py").read_text(encoding="utf-8")
CASES=[
    ("Merryfair Farm","skyrim-118","Веселая ферма","Весёлая ферма"),
    ("Driftshade Refuge","skyrim-145","Дрифтшейд - Приют","Дрифтшейд — Приют"),
    ("Evergreen Grove","skyrim-163","Вечнозеленая роща","Вечнозелёная роща"),
    ("Black-Briar Lodge","skyrim-277","Резиденция Черный Вереск","Резиденция «Чёрный Вереск»"),
    ("Chillfurrow Farm","skyrim-349","Ферма Холодная борозда","Ферма «Холодная Борозда»"),
    ("Snow Veil Sanctum","skyrim-227","Снежная Завеса - Святилище","Снежная Завеса — Святилище"),
    ("Sundered Towers","skyrim-263","Разделенные башни","Разделённые башни"),
    ("Bleakwind Basin","skyrim-159","Водоем Холодный Ветер","Водоём Холодный Ветер"),
    ("Bleakwind Bluff","skyrim-262","Утес Холодный Ветер","Утёс Холодный Ветер"),
    ("Serpent's Bluff Redoubt","skyrim-2","Оплот Змеиный Утес","Оплот Змеиный Утёс"),
    ("Journeyman's Nook","skyrim-214","Уголок подмастерья","Приют Подмастерья"),
    ("Ruunvald Excavation (DG)","skyrim-2289","Раскопки Рунвальда","Рунвальд — Раскопки"),
    ("Darkwater Crossing","skyrim-212","Черный Брод","Чёрный Брод"),
    ("Stonehill Bluff","skyrim-151","Утес Каменный Холм","Утёс Каменный Холм"),
    ("Wreck Of The Brinehammer","skyrim-311","Место крушения Соленого молота","Место крушения «Солёного молота»"),
    ("Wreck of the Winter War","skyrim-307","Место крушения Зимней Войны","Место крушения «Зимней войны»"),
    ("Wreck of the Icerunner","skyrim-312","Место крушения Бегущего по льду","Место крушения «Бегущего по льду»"),
    ("Redwater Den (DG)","skyrim-2288","Логово Красная Вода","Красноводное логово"),
    ("Frostmoon Crag","solstheim-2532","Ущелье Фростмуна","Утёс Морозной Луны"),
]
PRIOR=[
    ("Brinewater Grotto","Грот Солёная Вода"),
    ("Darkwater Pass","Чёрный проход"),
    ("Shadowgreen Cavern","Пещера Зелёная Тень"),
    ("Snapleg Cave","Щёлкающая пещера"),
    ("Stillborn Cave","Пещера Мертворождённого"),
    ("Sunderstone Gorge","Разделённое ущелье"),
    ("Reachcliff Cave","Утёсная пещера"),
    ("Reachwater Rock","Озёрный утёс"),
    ("Red Eagle Redoubt","Оплот Красный Орёл"),
    ("Dayspring Canyon (DG)","Ущелье Утренней Зари"),
    ("Darkfall Cave (DG)","Вечерняя пещера"),
    ("Knifepoint Ridge","Хребет Остриё Ножа"),
]
class LargeRussianMapBatch(unittest.TestCase):
    def test_all_display_names_and_stable_map_ids(self):
        self.assertEqual(len(CASES),19)
        for en,id,legacy,name in CASES:
            with self.subTest(location=en):
                data=[r for r in MAP if r["name"]==en]
                self.assertEqual(len(data),1)
                self.assertEqual(data[0]["id"],id)
                self.assertEqual(data[0]["nameRu"],name)
                self.assertIn(id+"|"+name+"|"+en+"|",JAVA)
                self.assertEqual(GLOSS.get(en,BASE.get(en)),name)
                self.assertIn("alias("+json.dumps(legacy,ensure_ascii=False)+","+json.dumps(en,ensure_ascii=False)+")",CAT)
    def test_prior_corrections_preserved_by_regeneration(self):
        for en,name in PRIOR:
            with self.subTest(location=en): self.assertEqual(GLOSS.get(en,BASE.get(en)),name)
    def test_legacy_search_and_no_overriding_wrong_frostmoon(self):
        self.assertIn("SEARCH_ALIASES.merge(site.id",CAT)
        self.assertNotIn('"Frostmoon Crag":"Ущелье Фростмуна"',GEN)
if __name__=="__main__": unittest.main()
