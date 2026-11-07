from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
CONT = (ROOT / "app/src/main/java/dev/infend/skyrimtext/DragonContinuation.java").read_text(encoding="utf-8")
PROGRESS = (ROOT / "app/src/main/java/dev/infend/skyrimtext/RpgProgress.java").read_text(encoding="utf-8")

class ContinuationChoicesTest(unittest.TestCase):
    def test_ten_unique_investigation_choices_per_chapter(self):
        block = CONT.split("private static final String[][] OPTIONS=", 1)[1].split("private static final int[][] SKILLS", 1)[0]
        for label in ("Составить список укрытий", "Проверить запасы переправы",
                      "Согласовать охрану обозов", "Отметить безопасные ночлеги",
                      "Устроить встречу свидетелей", "Передать предупреждение капитанам"):
            self.assertIn(label, block)
        self.assertEqual(30, len(re.findall(r'"[^"\n]+\\|[^"\n]+"', block)))
        self.assertIn('i<=OPTIONS[stage].length', CONT)

    def test_new_actions_are_persisted_and_one_time(self):
        self.assertIn('(?:[1-8]|10|11)', PROGRESS)
        self.assertIn('dragonNextUsed.contains(action)', CONT)
        self.assertIn('dragonNextUsed.add(action)', CONT)
        self.assertIn('dragonNextUsed",new JSONArray(dragonNextUsed)', PROGRESS)
        self.assertIn('choice==10||choice==11', CONT)

if __name__ == "__main__":
    unittest.main()
