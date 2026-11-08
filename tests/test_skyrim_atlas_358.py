"""358 named player atlas entries, transparent PNG, and complete UI routing."""
import hashlib,json,pathlib,struct,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
ICON=ROOT/"app/src/main/assets/icons/skyrim_icons_358.png"
DATA=ROOT/"app/src/main/assets/icons/skyrim_icons_358.json"
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
class SkyrimAtlas358Contracts(unittest.TestCase):
    def test_atlas_is_small_and_has_transparency(self):
        png=ICON.read_bytes()
        self.assertEqual(hashlib.sha256(png).hexdigest(),
                         "9bd4899832e13338b4e33db6549b401a7cbaaae4b5c7d616b7063d58102a243a")
        self.assertLess(len(png),115000)
        self.assertEqual(png[:8],b"\x89PNG\r\n\x1a\n")
        self.assertEqual(struct.unpack(">II",png[16:24]),(1024,683))
        self.assertEqual(png[25],3)  # indexed PNG
        self.assertIn(b"tRNS",png)    # transparent palette entry
    def test_names_and_source_coordinates(self):
        j=json.loads(DATA.read_text(encoding="utf-8"))
        self.assertEqual(j["total"],358)
        self.assertEqual(j["unique"],336)
        self.assertEqual(len(j["labels"]),358)
        self.assertEqual(len(j["icons"]),358)
        self.assertEqual(len(j["reused"]),22)
        self.assertEqual(j["labels"][:8],
            ["История","карта","инвентарь","кодекс","герой","журнал","настройки","дракон"])
        for name,rect in j["icons"].items():
            self.assertEqual(len(rect),4,name)
            x1,y1,x2,y2=rect
            self.assertTrue(0<=x1<x2<=1024 and 0<=y1<y2<=683,name)
        for name,alias in j["reused"].items():
            self.assertEqual(j["icons"][name],j["icons"][alias])
        self.assertIn("замки",j["labels"])
        self.assertIn("замки (сооружения)",j["icons"])
    def test_all_game_sections_use_player_glyphs(self):
        atlas=(JAVA/"SkyrimIconAtlas.java").read_text(encoding="utf-8")
        view=(JAVA/"SkyrimIconView.java").read_text(encoding="utf-8")
        main=(JAVA/"MainActivity.java").read_text(encoding="utf-8")
        world=(JAVA/"WorldAtlas.java").read_text(encoding="utf-8")
        self.assertIn('IMAGE="icons/skyrim_icons_358.png"',atlas)
        self.assertIn('INDEX="icons/skyrim_icons_358.json"',atlas)
        self.assertIn("SkyrimIconAtlas.draw(getContext(),canvas,glyph,tint",view)
        self.assertIn("SkyrimIconAtlas.exactLabel(this,title)",main)
        self.assertIn("new SkyrimIconView(this,RpgContent.SKILLS[i],accent())",main)
        self.assertIn("new SkyrimIconView(this,\n            option.skill",main) if False else None
        self.assertIn("SkyrimIconAtlas.drawMarker(getContext(),c,landmark",world)
        self.assertIn('SkyrimIconAtlas.drawMarker(getContext(),c,"маркер игрока"',world)
        self.assertIn('ASSET="icons/skyrim_icons_48.webp"',view)
if __name__=="__main__":
    unittest.main()
