"""Regression contracts for the optional, authoritative schema-1 scene ledger."""
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/infend/skyrimtext"

class SceneLedgerContracts(unittest.TestCase):
    def test_schema_one_is_unchanged_and_ledger_is_optional(self):
        engine = (JAVA / "GameEngine.java").read_text()
        self.assertIn('j.put("schema",1)', engine)
        self.assertIn('public SceneState sceneState = new SceneState();', engine)
        self.assertIn('.put("sceneState",sceneState.toJson())', engine)
        self.assertIn('SceneState.fromJson(j.optJSONObject("sceneState"))', engine)
        self.assertIn('sceneState=new SceneState()', engine)

    def test_only_explicit_confirmation_adds_bounded_facts(self):
        source = (JAVA / "SceneState.java").read_text()
        self.assertIn('MAX_ENTRIES = 32', source)
        self.assertIn('public void confirm(String kind,String key,String title,String status)', source)
        self.assertIn('entries.put(safeId,new JSONObject()', source)
        self.assertIn('!raw.matches(', source)
        self.assertIn('if(data==null)return state', source)
        self.assertIn('catch(JSONException invalid)', source)
        self.assertNotIn('return new SceneState();', source)
        for kind in ('npc', 'clue', 'sublocation'):
            self.assertIn('case "'+kind+'":', source)

    def test_ai_does_not_directly_invent_ledger_entries(self):
        bridge = (JAVA / "AiGameTurn.java").read_text()
        self.assertNotIn('sceneState.confirm(', bridge)

if __name__ == "__main__":
    unittest.main()
