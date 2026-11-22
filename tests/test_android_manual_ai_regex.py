"""Prevent the Android-only startup crash on typed AI actions.

The JVM test runner accepts Java's embedded (?U) regex flag, but Android's
regex implementation can reject it during class initialization. A desktop
JUnit pass alone therefore cannot protect the in-app manual-send path.
"""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/dev/infend/skyrimtext"
ACTION = (SOURCE / "AiRoutineAction.java").read_text(encoding="utf-8")
MAIN = (SOURCE / "MainActivity.java").read_text(encoding="utf-8")


class ManualActionAndroidRegex(unittest.TestCase):
    def test_risk_regex_uses_android_safe_flags_and_unicode_word_boundaries(self):
        self.assertNotIn("(?iuU)", ACTION)
        self.assertNotIn("(?U)", ACTION)
        self.assertIn("Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE", ACTION)
        self.assertIn("(?<!\\\\p{L})", ACTION)
        self.assertIn("(?!\\\\p{L})", ACTION)
        self.assertIn("RISK.matcher(action).find()", ACTION)

    def test_manual_send_survives_pattern_initializer_failure_without_autosuccess(self):
        self.assertIn('private boolean safeRoutineAiAction(String value)', MAIN)
        self.assertIn('catch(java.util.regex.PatternSyntaxException | LinkageError failure)', MAIN)
        self.assertIn('return false;', MAIN.split('private boolean safeRoutineAiAction(String value)', 1)[1].split('private void clearAiLiveViews()', 1)[0])
        self.assertIn('&&safeRoutineAiAction(value)', MAIN)
        self.assertIn('requestAiTurn("ОЦЕНКА СВОБОДНОГО ДЕЙСТВИЯ:', MAIN)
        self.assertIn('int die=fixedChance<0?-1:AiGameTurn.roll()', MAIN)


if __name__ == "__main__":
    unittest.main()
