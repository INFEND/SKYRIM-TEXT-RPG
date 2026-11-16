package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class KatherineInitiativeTest {
    private GameEngine game(){GameEngine g=new GameEngine();g.start("Герой","Норд","Маг");g.mode="WORLD";g.progress.location="Хижина Анис";g.katherine.stage=5;g.katherine.following=true;g.katherine.trust=70;g.katherine.magicRank=2;g.katherine.mana=100;return g;}
    @Test public void allowedAndReplaySafe() throws Exception {
        GameEngine g=game();KatherineStory k=g.katherine;
        assertFalse(KatherineInitiative.apply(g,"STUDY",false));assertFalse(KatherineInitiative.apply(g,"kill",true));
        assertTrue(KatherineInitiative.apply(g,"STUDY",true));assertEquals(88,k.mana);assertEquals(10,k.companion.xp);
        assertFalse(KatherineInitiative.apply(g,"STUDY",true));assertEquals(88,k.mana);
        GameEngine next=GameEngine.fromJson(g.toJson().toString());assertEquals("STUDY",next.katherine.companion.lastAiIntent);assertFalse(KatherineInitiative.apply(next,"STUDY",true));
        next.day++;assertTrue(KatherineInitiative.apply(next,"GUARD",true));assertEquals("DEFENSE",next.katherine.stance);
    }
    @Test public void guardsAndToggle(){GameEngine g=game();KatherineStory k=g.katherine;k.companion.aiInitiatives=false;assertFalse(KatherineInitiative.apply(g,"TALK",true));k.companion.aiInitiatives=true;g.enemyHp=10;assertFalse(KatherineInitiative.apply(g,"TALK",true));g.enemyHp=0;k.following=false;g.progress.location="Ривервуд";assertFalse(KatherineInitiative.apply(g,"TALK",true));g.progress.location="Хижина Анис";assertFalse(KatherineInitiative.apply(g,"GUARD",true));k.following=true;g.mode="COMBAT";g.battleKind="KATHERINE";assertFalse(k.setEnabled(g,false));assertTrue(k.enabled);}
    @Test public void protectedFacts(){assertTrue(KatherineInitiative.protectedFact("katherine_zero_limit","Иное имя"));assertTrue(KatherineInitiative.protectedFact("other","Кэтрин"));assertFalse(KatherineInitiative.protectedFact("hadvar","Хадвар"));}
}
