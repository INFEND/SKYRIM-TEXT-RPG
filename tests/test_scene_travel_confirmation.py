"""Regression contract: only completed atlas travel confirms visited scene facts."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
engine = (ROOT / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")
state = (ROOT / "app/src/main/java/dev/infend/skyrimtext/SceneState.java").read_text(encoding="utf-8")

def test_confirm_after_route_authorization_and_location_update():
    section = engine.split('if(action.startsWith("LOC:")){', 1)[1].split('if(action.startsWith("REGION:")){', 1)[0]
    assert section.index("if(!route.allowed)") < section.index("sceneState.confirm(")
    assert section.index("if(route.days==0)") < section.index("sceneState.confirm(")
    assert section.index("progress.location=target.name;") < section.index("sceneState.confirm(")
    assert '"sublocation"' in section and '"VISITED"' in section
    assert 'Integer.toHexString(target.name.hashCode())' in section

def test_scene_ledger_remains_optional_and_ai_read_only():
    assert 'sceneState=SceneState.fromJson(j.optJSONObject("sceneState"))' in engine
    assert 'put("sceneState",sceneState.toJson())' in engine
    assert 'public String confirmedContext()' in state
    assert 'entries.put(safeId' in state

if __name__ == "__main__":
    test_confirm_after_route_authorization_and_location_update()
    test_scene_ledger_remains_optional_and_ai_read_only()
