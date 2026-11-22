package dev.infend.skyrimtext;

import org.junit.Test;
import static org.junit.Assert.*;

/** Regression checks for non-combat free-form AI actions. */
public final class AiRoutineActionTest {
    @Test public void routineActionsDoNotNeedArtificialRolls(){
        assertTrue(AiRoutineAction.requiresNoCheck("Поговорить с торговцем"));
        assertTrue(AiRoutineAction.requiresNoCheck("Я спрашиваю дорогу"));
        assertTrue(AiRoutineAction.requiresNoCheck("Подойти к двери"));
        assertTrue(AiRoutineAction.requiresNoCheck("Отойти"));
        assertTrue(AiRoutineAction.requiresNoCheck("Обыскать труп"));
        assertTrue(AiRoutineAction.requiresNoCheck("Осмотреться"));
    }
    @Test public void russianRiskWordsAndCaseNeverBecomeGuaranteedActions(){
        // First access to AiRoutineAction also initializes its regex patterns.
        // On Android, a Java-only inline (?U) flag can throw ExceptionInInitializerError here.
        assertFalse(AiRoutineAction.requiresNoCheck("Поговорить с ДРАКОНОМ"));
        assertFalse(AiRoutineAction.requiresNoCheck("Спросить стражника о тайне"));
        assertFalse(AiRoutineAction.requiresNoCheck("Я подхожу к врагу незаметно"));
        assertFalse(AiRoutineAction.requiresNoCheck("Осмотреться возле ловушки"));
        assertTrue(AiRoutineAction.requiresNoCheck("Я спрашиваю о дороге"));
    }
    @Test public void contestedActionsStillNeedChecks(){
        assertFalse(AiRoutineAction.requiresNoCheck("Уйти из боя"));
        assertFalse(AiRoutineAction.requiresNoCheck("Подойти к врагу незаметно"));
        assertFalse(AiRoutineAction.requiresNoCheck("Поговорить со стражем и украсть ключ"));
        assertFalse(AiRoutineAction.requiresNoCheck("Узнать секрет"));
        assertFalse(AiRoutineAction.requiresNoCheck("Обыскать сундук"));
        assertFalse(AiRoutineAction.requiresNoCheck("Спросить, затем украсть кошелёк"));
        assertFalse(AiRoutineAction.requiresNoCheck(""));
    }
}
