"""Regression contract for persistent bounty completion history."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
ENGINE = (ROOT / "app/src/main/java/dev/infend/skyrimtext/GameEngine.java").read_text(encoding="utf-8")
UI = (ROOT / "app/src/main/java/dev/infend/skyrimtext/MainActivity.java").read_text(encoding="utf-8")

def test_bounty_claims_record_completion_in_both_paths():
    assert ENGINE.count("sideQuest=0;sideKills=0;boardCompleted++;gold+=120;addXp(65);") == 2

def test_bounty_count_survives_save_and_legacy_load():
    assert '.put("boardCompleted",boardCompleted)' in ENGINE
    assert 'j.optInt("boardCompleted",0)' in ENGINE
    assert 'boardCompleted=0;explored=0;' in ENGINE

def test_bounty_history_is_separate_from_check_history():
    assert 'game.boardCompleted>0' in UI
    assert 'выполнено заказов:' in UI
    assert 'Заказы доски после сдачи не сохраняются' not in UI
