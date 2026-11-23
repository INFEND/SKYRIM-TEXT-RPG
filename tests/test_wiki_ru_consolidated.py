"""Exact Skyrim Wiki names and stable save/search aliases for the new map/NPC batch."""
from pathlib import Path
import json
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/infend/skyrimtext"
ROWS=(JAVA/"SkyrimMapPoints.java").read_text(encoding="utf-8")
COMPILED=json.loads((ROOT/"tools/map_source/compiled.json").read_text(encoding="utf-8"))
NAMES=(ROOT/"tools/map_names_ru.txt").read_text(encoding="utf-8")
MAP=(JAVA/"SkyrimMapCatalog.java").read_text(encoding="utf-8")
NPC=(JAVA/"NpcProfiles.java").read_text(encoding="utf-8")
DOSSIER=(JAVA/"NpcDossier.java").read_text(encoding="utf-8")
QUEST=(JAVA/"CanonicalCampaign.java").read_text(encoding="utf-8")

SITES={
    "Darkwater Pass":("Черный проход","Чёрный проход"),
    "Duskglow Crevice":("Закатная расщелина","Закатная расселина"),
    "Brinewater Grotto":("Грот Соленая Вода","Грот Солёная Вода"),
    "Reachwater Rock":("Озерный утес","Озёрный утёс"),
    "Shadowgreen Cavern":("Пещера Зеленая Тень","Пещера Зелёная Тень"),
    "Knifepoint Ridge":("Хребет Острие Ножа","Хребет Остриё Ножа"),
}
WHITERUN={
    "Фаренгар Тайный Огонь":"Фаренгар",
    "Провентус Авениччи":"Провантус Авениччи",
    "Тильма Усталая":"Тильма Измождённая",
    "Йорлунд Серая Грива":"Эорлунд Серая Грива",
    "Садия":"Саадия",
}

class WikiRuConsolidation(unittest.TestCase):
    def test_six_map_sites_keep_identity_and_reviewed_source(self):
        points=COMPILED
        self.assertEqual(len(points),515)
        for en, (old, canonical) in SITES.items():
            with self.subTest(site=en):
                matches=[p for p in points if p["name"]==en]
                self.assertTrue(matches,en)
                self.assertTrue(any(p["nameRu"]==canonical for p in matches),en)
                self.assertIn("|"+canonical+"|"+en+"|",ROWS)
                self.assertNotIn("|"+old+"|"+en+"|",ROWS)
                self.assertIn(en+"|"+canonical, NAMES)
        self.assertIn('alias("Закатная расщелина","Duskglow Crevice");',MAP)
        self.assertIn("replace('ё','е')",MAP)  # Old unaccented spelling still resolves.
        self.assertEqual(len({p["id"] for p in COMPILED}),515)

    def test_whiterun_npc_names_and_original_aliases(self):
        for canonical,old in WHITERUN.items():
            with self.subTest(npc=canonical):
                rows=[line.strip() for line in NPC.splitlines()
                      if line.strip().startswith('add("'+canonical+'"')]
                self.assertEqual(len(rows),1,canonical)
                self.assertIn('"'+old+'"',rows[0])
                self.assertNotIn('add("'+old+'"', NPC)
        self.assertIn('add("Алем","Редгард","Женский"',NPC)
        self.assertIn('"Ahlam");',NPC)

    def test_dossier_and_canonical_campaign_consistent(self):
        self.assertIn('add("Фаренгар Тайный Огонь","Придворный маг',DOSSIER)
        self.assertIn('Провентус Авениччи — управитель',DOSSIER)
        self.assertNotIn('Провантус Авениччи — управитель',DOSSIER)
        self.assertIn(',"Фаренгар Тайный Огонь","На камне обозначены',QUEST)

if __name__=="__main__":
    unittest.main()
