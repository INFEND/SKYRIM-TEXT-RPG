"""Regression contracts for schema-1 optional SceneState restoration and irreversible facts."""
from pathlib import Path
import unittest

SOURCE=(Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext/SceneState.java").read_text(encoding="utf-8")
ENGINE=(Path(__file__).resolve().parents[1]/"app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")

class SceneStateIntegrityContracts(unittest.TestCase):
    def test_restore_reuses_validated_confirmation(self):
        self.assertIn('state.confirm(kind,key,entry.getString("title"),entry.getString("status"))',SOURCE)
        self.assertIn('catch(JSONException invalid)',SOURCE)
        self.assertIn('Math.min(arr.length(),4096)',SOURCE)
        self.assertIn('if(state.contains(kind,key))continue;',SOURCE)
        self.assertIn('sceneState=SceneState.fromJson(j.optJSONObject("sceneState"))',ENGINE)

    def test_statuses_are_enumerated_in_single_authoritative_validator(self):
        self.assertIn('if(!allowedStatus(kind,status))',SOURCE)
        self.assertNotIn('status.matches("[A-Z_]{2,24}")',SOURCE)
        for status in ("MET","INFORMED","HELPED","HOSTILE","DEAD","MISSING",
                       "FOUND","EXAMINED","USED","DISCOVERED","VISITED","CLEARED"):
            self.assertIn('"'+status+'"',SOURCE)

    def test_irreversible_facts_cannot_roll_back(self):
        self.assertIn('if("DEAD".equals(previous) && !"DEAD".equals(status))',SOURCE)
        self.assertIn('newRank<oldRank',SOURCE)
        self.assertIn('factRank(kind,status)<factRank(kind,previous)',SOURCE)
        for status in ("FOUND","EXAMINED","USED","DISCOVERED","VISITED","CLEARED"):
            self.assertIn('case "'+status+'":return',SOURCE)
        self.assertLess(SOURCE.index('if(entries.containsKey(safeId)) {'),
                        SOURCE.index('entries.put(safeId,new JSONObject()'))

if __name__=="__main__":
    unittest.main()
