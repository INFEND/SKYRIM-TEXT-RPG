"""Verify exact Russian bestiary names and historical keys for nine TES creatures."""
import csv, json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
BESTIARY=(ROOT/"app/src/main/java/dev/infend/skyrimtext/BestiaryCatalog.java").read_text(encoding="utf-8")
GENERATED=(ROOT/"app/src/main/java/dev/infend/skyrimtext/RussianNameData.java").read_text(encoding="utf-8")
with (ROOT/"tools/localization/catalog.tsv").open(encoding="utf-8") as f: CATALOG={(row["kind"],row["id"]):row for row in csv.DictReader(f,delimiter="\t")}
with (ROOT/"tools/localization/evidence.tsv").open(encoding="utf-8") as f: EVIDENCE={(row["kind"],row["id"]):row for row in csv.DictReader(f,delimiter="\t")}
CASES=[
    ("Почитаемый дракон","Благородный дракон","Revered Dragon","Благородный дракон","Dawnguard"),
    ("Драугр-повелитель смерти","Драугр-военачальник","Draugr Deathlord","Драугр-военачальник","Skyrim"),
    ("Вампир","Вампир","Vampire","Вампир (Skyrim)","Skyrim"),
    ("Вампир-лорд","Вампир-лорд","Vampire Lord","Вампир-лорд (Dawnguard)","Dawnguard"),
    ("Изгой-вересковое сердце","Изгой-вересковое сердце","Forsworn Briarheart","Изгой-вересковое сердце","Skyrim"),
    ("Риклинг","Риклинг","Riekling","Риклинг (Lore)","Dragonborn"),
    ("Дракон","Дракон","Dragon","Дракон","Skyrim"),
    ("Змеевидный дракон","Змеевидный дракон","Serpentine Dragon","Дракон","Dragonborn"),
    ("Двемерский паук-рабочий","Двемерский паук-рабочий","Dwarven Spider Worker","Путь знания","Dragonborn"),
]
class BestiarySecondLocalizationBatch(unittest.TestCase):
    def test_wiki_evidence_matches_game_labels(self):
        self.assertEqual(len(CASES),9)
        for id,name,en,page,edition in CASES:
            with self.subTest(creature=id):
                row=EVIDENCE["creature",id]
                self.assertEqual(name,CATALOG["creature",id]["name"])
                self.assertEqual(name,row["canonical"])
                self.assertEqual(en,row["english"])
                self.assertEqual(page,row["wiki"])
                self.assertEqual(edition,row["edition"])
                self.assertEqual(id if id!=name else "",row["prior"])
                self.assertIn('"'+id+'|',BESTIARY)
                if id!=name:
                    line="{" + ", ".join(json.dumps(x,ensure_ascii=False) for x in (id,name,en)) + "}"
                    self.assertIn(line,GENERATED)
if __name__=="__main__":unittest.main()
