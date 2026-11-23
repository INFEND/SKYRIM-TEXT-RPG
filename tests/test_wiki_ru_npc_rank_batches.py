"""Verifies ranked bandit and necromancer names against the reviewed Wiki rows."""
import csv
import pathlib
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
DATA=ROOT/"tools/localization"

ROWS=[
    ("Бандит", "Bandit", "Бандит", "Бандиты (Skyrim)"),
    ("Бандит-изгой", "Bandit Outlaw", "Разбойник", "Бандиты (Skyrim)"),
    ("Бандит-головорез", "Bandit Thug", "Громила", "Бандиты (Skyrim)"),
    ("Бандит-разбойник", "Bandit Highwayman", "Грабитель", "Бандиты (Skyrim)"),
    ("Бандит-грабитель", "Bandit Plunderer", "Головорез", "Бандиты (Skyrim)"),
    ("Бандит-мародёр", "Bandit Marauder", "Мародёр", "Бандиты (Skyrim)"),
    ("Главарь бандитов", "Bandit Chief", "Главарь бандитов", "Бандиты (Skyrim)"),
    ("Ученик некроманта", "Apprentice Necromancer", "Некромант-ученик", "Селвени Нетри"),
    ("Адепт некромантии", "Adept Necromancer", "Некромант-адепт", "Селвени Нетри"),
    ("Некромант-мастер", "Master Necromancer", "Некромант-мастер", "Селвени Нетри"),
    ("Некромант", "Necromancer", "Некромант", "Селвени Нетри"),
]

class RankedNpcLocalization(unittest.TestCase):
    def test_11_evidence_rows_and_runtime_catalog_agree(self):
        with (DATA/"evidence.tsv").open(encoding="utf-8",newline="") as fd:
            evidence={(r["kind"],r["id"]):r for r in csv.DictReader(fd,delimiter="\t")}
        with (DATA/"catalog.tsv").open(encoding="utf-8",newline="") as fd:
            catalog={(r["kind"],r["id"]):r for r in csv.DictReader(fd,delimiter="\t")}
        self.assertEqual(11,len(ROWS))
        for old,en,ru,wiki in ROWS:
            with self.subTest(english=en):
                item=evidence["npc-template",old]
                self.assertEqual(en,item["english"])
                self.assertEqual(ru,item["canonical"])
                self.assertEqual(wiki,item["wiki"])
                self.assertFalse(item["review"])
                self.assertEqual(ru,catalog["npc-template",old]["name"])
    def test_no_ambiguous_legacy_bandit_alias_and_no_renamed_save_keys(self):
        src=(ROOT/"app/src/main/java/dev/infend/skyrimtext/NpcProfiles.java").read_text(encoding="utf-8")
        self.assertIn('"Bandit Highwayman");',src)
        self.assertNotIn('"Bandit Highwayman","Разбойник");',src)
        for old,en,ru,_ in ROWS:
            with self.subTest(name=old):
                self.assertIn('addTemplate("'+old+'"',src)
                if old!=ru:self.assertIn('case "'+old+'": return "'+ru+'";',src)

if __name__=="__main__": unittest.main()
