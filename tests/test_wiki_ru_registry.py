import csv
import gzip
import io
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import audit_localization as audit


class RussianRegistry(unittest.TestCase):
    def test_reviewed_creature_aliases_are_not_used_as_runtime_template_labels(self):
        """The game must expose the reviewed title but retain the old key for saves."""
        source=(ROOT/"app/src/main/java/dev/infend/skyrimtext/NpcProfiles.java").read_text(encoding="utf-8")
        assert_code="if(t!=null)canonical=t.displayName;"
        self.assertIn(assert_code,source)
        self.assertNotIn("if(t!=null)canonical=t.name;",source)
        self.assertIn("if((target==INDEX||target==TEMPLATES)&&!p.displayName.equals(n))",source)
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

    def test_all_perk_ranks_are_counted_once_including_existing_save_indices(self):
        with gzip.open(audit.REGISTRY, "rt", encoding="utf-8") as stream:
            records = json.load(stream)["entities"]
        perks = [entity for entity in records if "perk_rank" in entity]
        self.assertEqual(273, len(perks))
        self.assertEqual(273, len({entity["identity"] for entity in perks}))
        self.assertEqual(55, sum(any(p["kind"] == "perk" for p in e["project"]) for e in perks))
        third = next(e for e in perks if e["original_id"] == "00079342")
        self.assertEqual({"tree": 0, "rank": 3}, third["perk_rank"])
        self.assertEqual("Сильная рука", third["canonical_ru"])

    def test_pending_table_lists_each_unreviewed_identity_once(self):
        with gzip.open(audit.REGISTRY, "rt", encoding="utf-8") as stream:
            entities = json.load(stream)["entities"]
        pending = list(csv.DictReader(io.StringIO(audit.pending_table(entities)), delimiter="\t"))
        expected = {e["identity"] for e in entities if e["status"] in {"conflict", "unverified"}}
        self.assertEqual(expected, {row["Уникальный ключ"] for row in pending})
        self.assertEqual(len(expected), len(pending))
        self.assertTrue(all(row["Причина проверки"] for row in pending if row["Статус"] == "Конфликт"))

    def test_reviewed_font_label_does_not_hide_a_new_geographic_occurrence(self):
        with gzip.open(audit.REGISTRY, "rt", encoding="utf-8") as stream:
            reviewed = json.load(stream)["reviewed_display_literals"]
        font = next(e for e in reviewed if e["match"] == "sovngarde")
        fields = font["file"], font["identity"], font["role"], font["match"]
        decisions = audit.reviewed_literals()
        original = (audit.ROOT / font["file"]).read_text()
        display = next(v for n, v in audit.player_literals(font["file"], original) if n == font["line"] and "Sovngarde" in v)
        self.assertEqual(font["reason"], audit.literal_decision(decisions, *fields, display))
        self.assertIsNone(audit.literal_decision(decisions, *fields, "Путешествие в Sovngarde"))

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
