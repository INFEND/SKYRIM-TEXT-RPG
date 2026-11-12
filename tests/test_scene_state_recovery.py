"""Guard against losing all confirmed scene facts due to one damaged save entry."""
from pathlib import Path
import unittest

SOURCE = (Path(__file__).resolve().parents[1] /
          "app/src/main/java/dev/infend/skyrimtext/SceneState.java").read_text(encoding="utf-8")


class SceneStateRecoveryTest(unittest.TestCase):
    def test_legacy_saves_without_ledger(self):
        self.assertIn("if(data==null)return state", SOURCE)

    def test_one_bad_record_does_not_reset_all_facts(self):
        self.assertIn("for(int i=0;i<limit && collectionSize(state,kind)<MAX_ENTRIES;i++)", SOURCE)
        self.assertIn("JSONObject entry=arr.optJSONObject(i)", SOURCE)
        self.assertIn("catch(JSONException invalid)", SOURCE)
        self.assertNotIn("return new SceneState();", SOURCE)

    def test_restore_is_bounded_and_duplicate_safe(self):
        self.assertIn("MAX_ENTRIES = 32", SOURCE)
        self.assertIn("Math.min(arr.length(),4096)", SOURCE)
        self.assertIn("collectionSize(state,kind)<MAX_ENTRIES", SOURCE)
        self.assertIn("private static int collectionSize", SOURCE)
        self.assertIn("if(state.contains(kind,key))continue", SOURCE)

    def test_malformed_first_32_slots_do_not_discard_later_valid_facts(self):
        self.assertIn("i<limit && collectionSize(state,kind)<MAX_ENTRIES", SOURCE)
        self.assertIn("if(entry==null)continue", SOURCE)
        self.assertIn("if(state.contains(kind,key))continue", SOURCE)
        self.assertIn("state.confirm(kind,key", SOURCE)

    def test_npc_relationship_progress_cannot_be_downgraded(self):
        self.assertIn('if(entries.containsKey(safeId))', SOURCE)
        self.assertIn('if("npc".equals(kind))', SOURCE)
        self.assertRegex(SOURCE, r'if\(oldRank>0 && newRank>0 && newRank<oldRank\)\s*throw new JSONException')
        self.assertIn('case "MET": return 1;', SOURCE)
        self.assertIn('case "INFORMED": return 2;', SOURCE)
        self.assertIn('case "HELPED": return 3;', SOURCE)

    def test_non_relationship_statuses_are_not_forced_into_order(self):
        self.assertIn('default: return 0;', SOURCE)
        self.assertRegex(SOURCE, r'if\(oldRank>0 && newRank>0 && newRank<oldRank\)\s*throw new JSONException')

    def test_only_validated_engine_facts_enter_context(self):
        self.assertIn("state.confirm(kind,key,entry.getString", SOURCE)
        self.assertRegex(SOURCE, r'if\(!allowedStatus\(kind,status\)\)\s*throw new JSONException')
        self.assertIn('case "npc": return "MET".equals(status)', SOURCE)
        self.assertIn('case "clue": return "FOUND".equals(status)', SOURCE)
        self.assertIn('case "sublocation": return "DISCOVERED".equals(status)', SOURCE)
        self.assertIn("confirmedContext()", SOURCE)


if __name__ == "__main__":
    unittest.main()
