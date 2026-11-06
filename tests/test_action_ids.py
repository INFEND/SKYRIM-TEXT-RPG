"""Guard action IDs against forged variants that bypass daily reward limits."""
from pathlib import Path
import re
import unittest

SOURCE = Path(__file__).resolve().parents[1] / "app/src/main/java/dev/infend/skyrimtext/RpgProgress.java"


class ActionIdContract(unittest.TestCase):
    def test_chance_requires_canonical_action_id(self):
        src = SOURCE.read_text(encoding="utf-8")
        self.assertIn('if(action.matches("ACT:[0-9]")){', src)

    def test_execution_rejects_noncanonical_ids_before_reward(self):
        src = SOURCE.read_text(encoding="utf-8")
        self.assertIn('if(action==null||!action.matches("ACT:[0-9]"))return false;', src)
        self.assertIn('if(attemptedToday.contains(action))return false;', src)

    def test_save_restore_uses_same_canonical_format(self):
        src = SOURCE.read_text(encoding="utf-8")
        self.assertIn('key.matches("ACT:[0-9]")', src)

    def test_accepted_ids_are_single_digits_only(self):
        pattern = re.compile(r"ACT:[0-9]", re.ASCII)
        for value in ("ACT:0", "ACT:9"):
            self.assertIsNotNone(pattern.fullmatch(value))
        for value in ("ACT:00", "ACT:10", "ACT:garbage", "ACT:-1", "ACT:0 ", "ACT:0:bonus"):
            self.assertIsNone(pattern.fullmatch(value))


if __name__ == "__main__":
    unittest.main()
