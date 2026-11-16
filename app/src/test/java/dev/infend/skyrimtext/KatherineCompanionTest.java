package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class KatherineCompanionTest {
    private GameEngine game(){GameEngine g=new GameEngine();g.start("Герой","Норд","Маг");g.mode=KatherineStory.MODE;g.progress.location="Хижина Анис";g.katherine.stage=8;g.katherine.following=true;g.katherine.magicRank=3;g.katherine.mana=120;g.katherine.trust=95;g.katherine.respect=90;g.katherine.affection=90;g.gold=1000;return g;}
    @Test public void spellsGearAndSave() throws Exception {
        GameEngine g=game();KatherineStory k=g.katherine;
        g.choose("KATH:gear");g.choose("KATH:focus");assertEquals(780,g.gold);assertEquals(1,k.companion.gear);assertEquals(1,k.companion.focus);
        g.choose("KATH:spell");assertEquals(1,k.companion.spell);assertTrue(k.assist(g,1,100)>0);assertEquals(3,k.companion.ongoingDamage(100));
        k.companion.hp=50;int potions=g.potions;g.choose("KATH:heal");assertEquals(potions-1,g.potions);assertEquals(90,k.companion.hp);
        GameEngine next=GameEngine.fromJson(g.toJson().toString());assertEquals(1,next.katherine.companion.spell);assertEquals(90,next.katherine.companion.hp);assertEquals(1,next.katherine.companion.gear);
    }
    @Test public void romanceScheduleAndMemory() throws Exception {
        GameEngine g=game();KatherineStory k=g.katherine;g.choose("KATH:romance");assertTrue(k.romance);
        for(int i=0;i<5;i++){g.day++;g.choose("KATH:date");g.choose("KATH:date");}
        assertEquals(85,k.companion.love);g.choose("KATH:damon");g.choose("KATH:engage");assertTrue(k.companion.engaged);
        g.progress.location="Рифтен";g.choose("KATH:wedding");assertTrue(k.companion.married);
        for(int i=0;i<100;i++)k.remember(g,"Событие "+i);assertTrue(k.memory.length()>40);
        GameEngine next=GameEngine.fromJson(g.toJson().toString());assertTrue(next.katherine.companion.married);assertTrue(next.katherine.memory.length()>40);
        next.choose("KATH:break");assertFalse(next.katherine.companion.married);
    }
    @Test public void independentEnding() {
        GameEngine g=game();KatherineStory k=g.katherine;k.stage=10;g.choose("KATH:ending_free");
        assertFalse(k.following);assertEquals("Свободная дорога",k.companion.ending);
        g.day=3;g.hour=12;g.progress.location="Коллегия Винтерхолда";assertTrue(k.present(g));
        g.progress.location="Рифтен";assertFalse(k.present(g));
    }
    @Test public void suppliesAndIndependentGoals() throws Exception {
        GameEngine g=game();KatherineStory k=g.katherine;g.potions=2;g.manaPotions=2;
        g.choose("KATH:stockhp");g.choose("KATH:stockmana");
        assertEquals(1,g.potions);assertEquals(1,g.manaPotions);assertEquals(1,k.companion.healthPotions);assertEquals(1,k.companion.magicPotions);
        k.companion.hp=10;k.mana=5;assertFalse(k.companion.useSupplies(k).isEmpty());assertEquals(50,k.companion.hp);assertFalse(k.companion.useSupplies(k).isEmpty());assertEquals(50,k.mana);
        k.stage=10;g.choose("KATH:ending_free");g.day=3;k.autonomous(g);assertTrue(k.companion.currentGoal.contains("резонанс"));
        int count=k.memory.length();k.autonomous(g);assertEquals(count,k.memory.length());
        GameEngine next=GameEngine.fromJson(g.toJson().toString());assertEquals(k.companion.currentGoal,next.katherine.companion.currentGoal);
    }
    @Test public void epilogueAndHomePersist() throws Exception {
        GameEngine g=game();KatherineStory k=g.katherine;k.stage=10;g.choose("KATH:ending_alliance");
        g.choose("KATH:epilogue:0");g.choose("KATH:epilogue:1");assertEquals(1,k.companion.epilogueStep);
        g.day++;g.choose("KATH:epilogue:1");g.day++;g.choose("KATH:epilogue:2");assertEquals(3,k.companion.epilogueStep);
        k.romance=true;k.companion.engaged=true;k.companion.married=true;g.progress.location="Рифтен";g.gold=1000;
        g.choose("KATH:home");g.choose("KATH:home");assertEquals(500,g.gold);assertTrue(k.companion.sharedHome);
        k.companion.hp=20;g.choose("KATH:homeEvening");g.choose("KATH:homeEvening");assertEquals(44,k.companion.hp);
        GameEngine next=GameEngine.fromJson(g.toJson().toString());assertTrue(next.katherine.companion.sharedHome);assertEquals(3,next.katherine.companion.epilogueStep);
        next.choose("KATH:break");assertFalse(next.katherine.companion.sharedHome);
    }
    @Test public void independentEventsUseNpcLocationAndDoNotRepeat() {
        GameEngine g=game();KatherineStory k=g.katherine;k.stage=10;g.choose("KATH:ending_free");
        g.day=3;g.hour=12;g.progress.location="Рифтен";k.autonomous(g);int xp=k.companion.xp;assertEquals(12,xp);
        assertTrue(k.memory.optString(k.memory.length()-1).contains("Коллегия Винтерхолда"));
        k.autonomous(g);assertEquals(xp,k.companion.xp);assertEquals(1,k.companion.daysIndependent);
        assertFalse(KatherineInitiative.apply(g,"TALK",true));g.progress.location="Коллегия Винтерхолда";
        assertFalse(KatherineInitiative.apply(g,"GUARD",true));assertTrue(KatherineInitiative.apply(g,"TALK",true));
    }
}
