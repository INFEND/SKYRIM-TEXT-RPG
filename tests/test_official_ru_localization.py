"""Small synthetic fixtures; no copyrighted Skyrim text is stored in this repo."""
import importlib.util
import json
import struct
import tempfile
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "tools/official_ru_localization.py"
SPEC = importlib.util.spec_from_file_location("official_ru_localization", MODULE_PATH)
ru = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ru)


def write_strings(path, rows, *, length_prefixed=False, encoding="cp1251"):
    payload = bytearray()
    index = []
    for record_id, text in rows:
        offset = len(payload)
        encoded = text.encode(encoding) + b"\x00"
        if length_prefixed:
            payload.extend(struct.pack("<I", len(encoded)))
        payload.extend(encoded)
        index.append((record_id, offset))
    data = bytearray(struct.pack("<II", len(index), len(payload)))
    for record_id, offset in index:
        data.extend(struct.pack("<II", record_id, offset))
    data.extend(payload)
    path.write_bytes(data)


class OfficialRuLocalizationTests(unittest.TestCase):
    def test_binary_file_formats_and_corruption_detection(self):
        with tempfile.TemporaryDirectory() as folder:
            base = Path(folder)
            for ext in ("STRINGS", "ILSTRINGS", "DLSTRINGS"):
                filename = base / ("Skyrim_Russian." + ext)
                write_strings(filename, [(100, "Северный предел"), (200, "Ёж")],
                              length_prefixed=ext != "STRINGS")
                self.assertEqual(ru.read_strings(filename)[100], "Северный предел")
                self.assertEqual(ru.read_strings(filename)[200], "Ёж")
                filename.write_bytes(filename.read_bytes()[:-1])
                with self.assertRaises(ru.ImportErrorDetail):
                    ru.read_strings(filename)

    def test_names_are_verified_by_shared_ids_not_fuzzy_similarity(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            write_strings(root / "Skyrim_English.STRINGS",
                          [(1, "North Keep"), (2, "South Keep")], encoding="cp1252")
            write_strings(root / "Skyrim_Russian.STRINGS",
                          [(1, "Северная крепость"), (2, "Южная крепость")])
            glossary = root / "names.txt"
            glossary.write_text("North Keep|Северная крепость\n"
                               "South Keep|Северная крепость\n"
                               "Missing|Неизвестно\n", encoding="utf-8")
            report = root / "report.json"
            result = ru.audit_names(root, glossary, report)
            self.assertEqual(result["verified"], 1)
            self.assertEqual(result["needs_review"], 1)
            self.assertEqual(result["not_found"], 1)
            self.assertEqual(json.loads(report.read_text(encoding="utf-8"))
                             ["entries"][1]["official_candidates"][0]["string_id"], 2)

    def test_build_story_requires_all_33_chapters_and_real_ids(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            write_strings(root / "Skyrim_Russian.DLSTRINGS",
                          [(i, f"Тестовая реплика {i}") for i in range(1, 34)],
                          length_prefixed=True)
            java = root / "CanonicalCampaign.java"
            java.write_text("\n".join(
                f'new Stage("Этап {i}", "Тест", 0);' for i in range(1, 34)
            ), encoding="utf-8")
            rows = [
                {"scene_id": i, "source": "Skyrim", "speaker": "Тестовый NPC",
                 "line_start": i, "string_id": i}
                for i in range(1, 34)
            ]
            manifest = root / "manifest.json"
            manifest.write_text(json.dumps({"segments": rows}, ensure_ascii=False),
                                encoding="utf-8")
            output = root / "imported_story_v7.json"
            stats = ru.build_story(root, java, manifest, output)
            self.assertEqual(stats["chapters"], 33)
            self.assertEqual(stats["segments"], 33)
            pack = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual(pack["segments"][8]["reading_text"], "Тестовая реплика 9")
            self.assertEqual(pack["scenes"][32]["title"], "Этап 33")
            rows[-1]["string_id"] = 999999
            manifest.write_text(json.dumps({"segments": rows}, ensure_ascii=False),
                                encoding="utf-8")
            with self.assertRaises(ru.ImportErrorDetail):
                ru.build_story(root, java, manifest, output)

    def test_unknown_speaker_and_path_traversal_are_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            write_strings(root / "Skyrim_Russian.DLSTRINGS",
                          [(i, f"Тест {i}") for i in range(1, 34)],
                          length_prefixed=True)
            java = root / "CanonicalCampaign.java"
            java.write_text("\n".join(
                f'new Stage("Сцена {i}", "Тест", 0);' for i in range(1, 34)
            ), encoding="utf-8")
            rows = [{"scene_id": i, "speaker": "Актёр", "string_id": i,
                     "source": "Skyrim", "line_start": i} for i in range(1, 34)]
            manifest = root / "manifest.json"
            output = root / "private.json"
            for field, value in (("speaker", ""), ("source", "../other")):
                changed = [dict(row) for row in rows]
                changed[2][field] = value
                manifest.write_text(json.dumps({"segments": changed}), encoding="utf-8")
                with self.assertRaises(ru.ImportErrorDetail):
                    ru.build_story(root, java, manifest, output)
                self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
