"""358 unique named sprites rebuilt from the user's reference PNG archive."""
import hashlib,json,pathlib,struct,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
ICON=ROOT/"app/src/main/assets/icons/skyrim_icons_358_exact.webp"
DATA=ROOT/"app/src/main/assets/icons/skyrim_icons_358_exact.json"
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"

class SkyrimAtlas358Contracts(unittest.TestCase):
    def test_exact_source_atlas_is_compact_and_transparent(self):
        art=ICON.read_bytes()
        self.assertEqual(hashlib.sha256(art).hexdigest(),
                         "e96f6dcf5fecd9cec146d8284c4b22055c262e0c6a873c75e9711df473e78f27")
        self.assertLess(len(art),1300000)
        self.assertEqual(art[:4],b"RIFF")
        self.assertEqual(art[8:12],b"WEBP")
        self.assertIn(b"ALPH",art)
        self.assertIn(b"VP8X",art)
        self.assertEqual(art[20]&16,16)  # VP8X alpha flag
        self.assertEqual((int.from_bytes(art[24:27],"little")+1,
                          int.from_bytes(art[27:30],"little")+1),(1728,1920))

    def test_no_aliased_or_duplicate_icons(self):
        j=json.loads(DATA.read_text(encoding="utf-8"))
        self.assertEqual((j["width"],j["height"]),(1728,1920))
        self.assertEqual((j["total"],j["unique"]),(358,358))
        self.assertEqual(len(j["labels"]),358)
        self.assertEqual(len(j["icons"]),358)
        self.assertEqual(j["reused"],{})
        self.assertEqual(len(j["entries"]),358)
        self.assertEqual(len({tuple(rect) for rect in j["icons"].values()}),358)
        self.assertEqual(j["labels"][:8],
            ["История","Карта","Инвентарь","Кодекс","Герой","Журнал","Настройки","Дракон"])
        for idx,entry in enumerate(j["entries"],1):
            self.assertEqual(entry["index"],idx)
            self.assertEqual(entry["label"],j["labels"][idx-1])
            self.assertTrue(entry["source"].startswith(f"{idx:03d}_"))
            self.assertEqual(len(entry["sha256"]),64)
        for name,rect in j["icons"].items():
            self.assertEqual(len(rect),4,name)
            x1,y1,x2,y2=rect
            self.assertTrue(0<=x1<x2<=1728 and 0<=y1<y2<=1920,name)
        self.assertIn("замки",j["labels"])
        self.assertIn("замки (сооружения)",j["labels"])
        self.assertNotEqual(j["icons"]["замки"],j["icons"]["замки (сооружения)"])
        self.assertNotEqual(j["icons"]["Топоры"],j["icons"]["Боевые молоты"])
        for label in ("Амулеты","Зелья","Септимы","Отмычки","Факелы",
                      "Еда","Напитки","Пользовательское оружие",
                      "Пользовательские предметы"):
            self.assertIn(label,j["icons"])

    def test_all_game_sections_use_exact_atlas_and_still_have_legacy_fallback(self):
        atlas=(JAVA/"SkyrimIconAtlas.java").read_text(encoding="utf-8")
        view=(JAVA/"SkyrimIconView.java").read_text(encoding="utf-8")
        main=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        world=(JAVA/"WorldAtlas.java").read_text(encoding="utf-8")
        self.assertIn('IMAGE="icons/skyrim_icons_358_exact.webp"',atlas)
        self.assertIn('INDEX="icons/skyrim_icons_358_exact.json"',atlas)
        self.assertIn('json.optInt("unique")!=358',atlas)
        self.assertIn("SkyrimIconAtlas.draw(getContext(),canvas,glyph,tint",view)
        self.assertIn("SkyrimIconAtlas.exactLabel(this,title)",main)
        self.assertIn("new SkyrimIconView(this,RpgContent.SKILLS[i],accent())",main)
        self.assertIn("SkyrimIconAtlas.drawMarker(getContext(),c,landmark",world)
        self.assertIn('SkyrimIconAtlas.drawMarker(getContext(),c,"маркер игрока"',world)
        self.assertIn('ASSET="icons/skyrim_icons_48.webp"',view)

if __name__=="__main__":
    unittest.main()
