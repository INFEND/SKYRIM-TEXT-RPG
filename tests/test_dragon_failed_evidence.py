"""Regression contract: a failed risky attempt consumes its unique choice, not evidence."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
story = (ROOT / "app/src/main/java/dev/infend/skyrimtext/DragonContinuation.java").read_text()
progress = (ROOT / "app/src/main/java/dev/infend/skyrimtext/RpgProgress.java").read_text()

def test_failed_attempt_does_not_count_as_evidence():
    assert '!g.progress.dragonNextFailed.contains("DRAGON_NEXT:"+stage+":"+i)' in story
    assert 'g.progress.dragonNextFailed.add(action);' in story
    assert 'g.progress.dragonNextUsed.add(action);' in story

def test_failure_record_survives_schema_one_save():
    assert '.put("dragonNextFailed",new JSONArray(dragonNextFailed))' in progress
    assert 'obj.optJSONArray("dragonNextFailed")' in progress
    assert 'p.dragonNextUsed.contains(value)' in progress
    assert 'DRAGON_NEXT:[01]:[357]' in progress

def test_old_saves_have_no_mandatory_new_field():
    assert 'if(nextFailed!=null)' in progress
    assert 'public final HashSet<String> dragonNextFailed=new HashSet<>()' in progress
