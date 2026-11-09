"""Guard against losing all confirmed scene facts due to one damaged save entry."""
from pathlib import Path
import unittest

SOURCE = (Path(__file__).resolve().parents[1] /
          "app/src/main/java/dev/infend/skyrimtext/SceneState.java").read_text(encoding="utf-8")


class SceneStateRecoveryTest(unittest.TestCase):
    def test_legacy_saves_without_ledger(self):
        self.assertIn("if(data==null)return state", SOURCE)

    def test_one_bad_record_does_not_reset_all_facts(self):
        self.assertIn("for(int i=0;i<limit;i++)", SOURCE)
        self.assertIn("JSONObject entry=arr.optJSONObject(i)", SOURCE)
        self.assertIn("catch(JSONException invalid)", SOURCE)
        self.assertNotIn("return new SceneState();", SOURCE)

    def test_restore_is_bounded_and_duplicate_safe(self):
        self.assertIn("MAX_ENTRIES = 32", SOURCE)
        self.assertIn("Math.min(arr.length(),MAX_ENTRIES)", SOURCE)
        self.assertIn("if(state.contains(kind,key))continue", SOURCE)

    def test_only_validated_engine_facts_enter_context(self):
        self.assertIn("state.confirm(kind,key,entry.getString", SOURCE)
        self.assertIn('status.matches("[A-Z_]{2,24}")', SOURCE)
        self.assertIn("confirmedContext()", SOURCE)


if __name__ == "__main__":
    unittest.main()
