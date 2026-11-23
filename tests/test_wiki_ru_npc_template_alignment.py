"""Check 16 canonically named NPC templates and their original save keys."""
import csv, json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=(ROOT/"app/src/main/java/dev/infend/skyrimtext/NpcProfiles.java").read_text(encoding="utf-8")
BESTIARY=(ROOT/"app/src/main/java/dev/infend/skyrimtext/BestiaryCatalog.java").read_text(encoding="utf-8")
with (ROOT/"tools/localization/catalog.tsv").open(encoding="utf-8") as f: CATALOG={(r["kind"],r["id"]):r for r in csv.DictReader(f,delimiter="\t")}
with (ROOT/"tools/localization/evidence.tsv").open(encoding="utf-8") as f: EVIDENCE={(r["kind"],r["id"]):r for r in csv.DictReader(f,delimiter="\t")}
CASES=[
    ("Фалмер-воитель","Фалмер-мастер теней","Falmer Shadowmaster","Фалмер (Skyrim)",True),
    ("Фалмер-военачальник","Фалмер-наёмник","Falmer Warmonger","Фалмер (Skyrim)",True),
    ("Фалмер-скрытень","Фалмер-мастер укрытий","Falmer Skulker","Фалмер (Skyrim)",False),
    ("Фалмер-мракобес","Фалмер-сумрачный страж","Falmer Gloomlurker","Фалмер (Skyrim)",False),
    ("Фалмер-сумеречный","Фалмер-ночной охотник","Falmer Nightprowler","Фалмер (Skyrim)",False),
    ("Гневный человек","Яростный человек","Wrathman","Яростный человек (Dawnguard)",True),
    ("Скрытень","Луркер","Lurker","Луркер (Dragonborn)",True),
    ("Волк (существо)","Волк","Wolf","Волк (Skyrim)",True),
    ("Ледяной волк","Снежный волк","Ice Wolf","Снежный волк",True),
    ("Мать дымков","Мать-дымок","Wispmother","Мать-дымок (Skyrim)",True),
    ("Огонёк","Дымок","Wisp","Дымок (Skyrim)",True),
    ("Кор","Корус","Chaurus","Корус (Skyrim)",True),
    ("Кор-жнец","Корус-жнец","Chaurus Reaper","Корус (Skyrim)",False),
    ("Кор-охотник","Корус-охотник","Chaurus Hunter","Корус (Skyrim)",True),
    ("Горящий спригган","Горелый спригган","Burnt Spriggan","Неотмеченные локации (Skyrim)",True),
    ("Спригган-земная мать","Спригган-мать земли","Spriggan Earth Mother","Существа (Skyrim)",False),
]
class CanonicalNpcTemplates(unittest.TestCase):
    def test_sixteen_display_names_keep_all_legacy_ids(self):
        self.assertEqual(len(CASES),16)
        self.assertIn("displayName=t?canonicalTemplateName(n)",SOURCE)
        self.assertIn("target==INDEX||target==TEMPLATES",SOURCE)
        for old,canonical,english,page,has_creature in CASES:
            with self.subTest(old=old):
                self.assertIn("case "+json.dumps(old,ensure_ascii=False)+": return "+json.dumps(canonical,ensure_ascii=False)+";",SOURCE)
                self.assertIn('addTemplate("'+old+'"',SOURCE)
                self.assertEqual(canonical,CATALOG["npc-template",old]["name"])
                if has_creature: self.assertIn('alias("'+old+'","'+canonical+'")',BESTIARY)
                else:
                    row=EVIDENCE["npc-template",old]
                    self.assertEqual(canonical,row["canonical"])
                    self.assertEqual(english,row["english"])
                    self.assertEqual(old,row["prior"])
                    self.assertEqual(page,row["wiki"])
if __name__=="__main__": unittest.main()
