"""Release-history invariants: protect cumulative OTA notes and app identity."""
import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class ReleaseHistoryContract(unittest.TestCase):
    def test_history_is_ordered_unique_and_matches_app_version(self):
        history = json.loads((ROOT / "release_notes/history.json").read_text(encoding="utf-8"))
        self.assertTrue(history, "OTA history must not be empty")
        codes = [entry["versionCode"] for entry in history]
        self.assertEqual(codes, sorted(set(codes)), "History must be strictly increasing without duplicates")
        names = [entry["versionName"] for entry in history]
        self.assertEqual(len(names), len(set(names)), "Version names must not repeat")
        for entry in history:
            self.assertIsInstance(entry["notes"], str)
            self.assertTrue(entry["notes"].strip(), "Every released version needs a changelog")
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        code = re.search(r"\bversionCode\s+(\d+)", gradle)
        name = re.search(r"\bversionName\s+['\"]([^'\"]+)['\"]", gradle)
        self.assertIsNotNone(code)
        self.assertIsNotNone(name)
        self.assertEqual(int(code.group(1)), codes[-1], "Update history when bumping versionCode")
        self.assertEqual(name.group(1), names[-1], "Update history when bumping versionName")

    def test_package_id_and_legacy_history_remain_stable(self):
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertRegex(gradle, r"applicationId\s+['\"]dev\.infend\.skyrimtext['\"]")
        history = json.loads((ROOT / "release_notes/history.json").read_text(encoding="utf-8"))
        self.assertTrue({2, 3, 4}.issubset({entry["versionCode"] for entry in history}),
                        "Do not discard legacy client release history")


if __name__ == "__main__":
    unittest.main()
