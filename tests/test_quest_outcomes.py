"""Regression contract: persistent quest ledger is not a skill-check log."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
progress = (ROOT / "app/src/main/java/dev/infend/skyrimtext/RpgProgress.java").read_text()
ui = (ROOT / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text()

def test_quest_outcomes_persist():
    assert 'put("questOutcomes",new JSONObject(questOutcomes))' in progress
    assert 'obj.optJSONObject("questOutcomes")' in progress
    assert 'setQuestOutcome("ash_seals","ACTIVE")' in progress
    assert 'setQuestOutcome("ash_seals","COMPLETED")' in progress

def test_terminal_outcomes_and_separate_journal():
    assert 'if("COMPLETED".equals(old)||"FAILED".equals(old))return;' in progress
    assert 'game.progress.questOutcomes.entrySet()' in ui
    assert '"FAILED".equals(entry.getValue())' in ui
    assert 'ИСТОРИЯ НЕУДАЧНЫХ ПРОВЕРОК' in ui
