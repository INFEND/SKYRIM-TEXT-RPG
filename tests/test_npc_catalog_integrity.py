import pathlib
import re
import unittest
ROOT = pathlib.Path(__file__).resolve().parents[1]
class NpcCatalogIntegrity(unittest.TestCase):
    def test_unique_normalized_names(self):
        source = (ROOT / "app/src/main/java/dev/infend/skyrimtext/NpcProfiles.java").read_text(encoding="utf-8")
        rows = re.findall(r'^\s*add\(".*?\);$', source, re.M)
        self.assertGreaterEqual(len(rows), 646)
        owners = {}
        for row in rows:
            fields = re.findall(r'"([^"]*)"', row)
            self.assertIn(fields[2], ("Мужской", "Женский"))
            for name in [fields[0], *fields[4:]]:
                key = name.lower().replace("ё", "е").replace("’", "'").replace("\u0301", "").strip()
                if key in owners:
                    self.assertEqual(owners[key], fields[0])
                owners[key] = fields[0]
