"""Regression checks for the three continuation quests in the journal."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ui = (ROOT / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")
story = (ROOT / "app/src/main/java/dev/infend/skyrimtext/DragonContinuation.java").read_text(encoding="utf-8")

def test_active_objective_is_shown():
    assert "DragonContinuation.questObjective(game)" in ui
    assert "DragonContinuation.status(game)" in ui
    assert 'game.progress.dragonStage>=DragonArc.FINAL_STAGE' in ui

def test_completed_investigations_are_listed_separately():
    for name in ("Письмо под пеплом", "Совет за закрытыми дверями", "След пепельного гонца"):
        assert name in ui
    assert 'Math.min(3,game.progress.dragonNextStage)' in ui
    assert "ЗАВЕРШЁННЫЕ ЗАДАНИЯ" in ui
    assert "ИСТОРИЯ УСПЕШНЫХ ПРОВЕРОК" in ui

def test_continuation_stays_in_existing_save_schema():
    assert "dragonNextStage" in story
    assert "dragonNextUsed" in story
    assert "schema" not in story
