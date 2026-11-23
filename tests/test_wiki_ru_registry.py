import gzip
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import audit_localization as audit


class RussianRegistry(unittest.TestCase):
    def test_accepted_evidence_matches_the_runtime_catalog(self):
        rows = {(r["kind"], r["id"]): r for r in audit.catalog()}
        seen = set()
        for entry in audit.evidence():
            key = (entry["kind"], entry["id"])
            self.assertNotIn(key, seen)
            seen.add(key)
            self.assertIn(key, rows)
            if not entry["review"]:
                self.assertEqual(rows[key]["name"], entry["canonical"], key)
                self.assertTrue(entry["english"] or entry["original_id"], key)
                self.assertTrue(entry["wiki"], key)
                if entry["original_id"].startswith("XX"):
                    self.assertIn(entry["edition"], ("Hearthfire", "Dawnguard", "Dragonborn"), key)
        self.assertEqual((audit.JAVA / "RussianNameData.java").read_text(), audit.java_source())

    def test_published_registry_preserves_project_keys_and_separates_ambiguous_quests(self):
        doc = json.loads(gzip.decompress(audit.REGISTRY.read_bytes()))
        by_id = {entry["identity"]: entry for entry in doc["entities"]}
        self.assertEqual(len(doc["entities"]), len(by_id))
        keys = [(use["kind"], use["id"]) for entry in doc["entities"] for use in entry["project"]]
        self.assertEqual(len(keys), len(set(keys)))
        self.assertEqual(set(keys), {(r["kind"], r["id"]) for r in audit.catalog()})
        quest = by_id["record:DLC1VQ01MiscObjective"]
        self.assertEqual("Страж Рассвета", quest["canonical_ru"])
        self.assertIn("Стража Рассвета", quest["project"][0]["aliases"])
        self.assertTrue(any(e["status"] == "author" and e["category"] == "Задания" for e in by_id.values()))

    def test_percentage_counts_unique_original_entries(self):
        report = audit.markdown_table({"Книги": {"verified": 1, "corrected": 1, "conflict": 1,
                                                "unverified": 1, "author": 4, "creation-club": 2}})
        self.assertIn("| Книги | 4 | 2 | 1 | 2 | 1 | 50,00% |", report)
        self.assertIn("| **Итого** | 4 | 2 | 1 | 2 | 1 | 50,00% |", report)

    def test_player_text_detection_excludes_identifiers_and_search_aliases(self):
        source = 'register("Farengar");\n// setTitle("Farengar");\nsetTitle("Farengar");'
        self.assertEqual([(3, "Farengar")], list(audit.player_literals("app/src/main/java/Game.java", source)))
        self.assertEqual([], list(audit.player_literals("tests/Game.java", source)))
        data = '{"id":"Farengar","description":"Farengar"}'
        self.assertEqual([(1, "Farengar")], list(audit.player_literals("app/src/main/assets/lines.json", data)))
