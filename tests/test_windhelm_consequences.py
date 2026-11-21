"""Regression guard: Windhelm outcomes must change gameplay, not only journal prose."""
from pathlib import Path
import re

SOURCE = Path("app/src/main/java/dev/infend/skyrimtext/DragonContinuation.java").read_text(encoding="utf-8")

def test_windhelm_outcomes_have_distinct_rewards():
    assert "if(stage==2){" in SOURCE
    for condition in ("proved(g,2,1)&&proved(g,2,4)&&proved(g,2,7)",
                      "proved(g,2,2)&&proved(g,2,6)",
                      "proved(g,2,5)&&proved(g,2,4)"):
        assert condition in SOURCE
    assert "g.reputation+5" in SOURCE
    assert "g.reputation+4" in SOURCE
    assert "g.progress.dragonInsight+3" in SOURCE
    assert "g.reputation+1" in SOURCE

def test_rewards_only_in_completion_branch():
    completion = SOURCE.index("if(choice==0){")
    guard = SOURCE.index("if(evidenceCount(g,stage)<3)", completion)
    finish = SOURCE.index("g.progress.dragonNextStage++", guard)
    reward = SOURCE.index("if(stage==2){", finish)
    exit_branch = SOURCE.index("if(g.progress.dragonNextUsed.contains(action))return;", reward)
    assert completion < guard < finish < reward < exit_branch
    assert SOURCE.count("if(stage==2){") == 1
    assert "g.progress.setQuestOutcome(questId(stage),\"COMPLETED\");" in SOURCE[guard:reward]
