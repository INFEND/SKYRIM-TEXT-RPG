package dev.infend.skyrimtext;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
public class EquipmentConditionTest {
    @Test public void legacyWearIgnored() throws Exception {
        RpgProgress p=new RpgProgress();ItemCatalog.Item item=ItemCatalog.find("Железный меч");
        p.quantity(item.id,1);double full=item.currentStat(p);
        JSONObject old=p.toJson().put("equipmentCondition",new JSONObject().put(item.id,0));
        RpgProgress restored=RpgProgress.fromJson(old);
        assertEquals(full,item.currentStat(restored),.0001);
        assertFalse(restored.toJson().has("equipmentCondition"));
    }
    @Test public void noChargeOrWearRows(){
        assertEquals(1,ItemEffectRow.parse("Заряд зачарования: 75% | Прочность: 60% | Урон огнём: 10 ед.").size());
    }
    @Test public void poisonApplicationAndSave() {
        GameEngine g=new GameEngine();g.started=true;g.mode="WORLD";
        g.progress.quantity("Железный меч",1);assertTrue(g.progress.equip("Железный меч"));
        g.progress.quantity("Яд паралича",2);assertTrue(g.progress.applyPoison("Яд паралича",g));
        assertFalse(g.progress.applyPoison("Яд паралича",g));assertEquals(1,g.progress.quantity("Яд паралича"));
        RpgProgress p=RpgProgress.fromJson(g.progress.toJson());assertEquals("Яд паралича",p.consumePoison("Железный меч"));assertEquals("",p.consumePoison("Железный меч"));
    }
    @Test public void talosOnlyWhenEquipped(){
        GameEngine g=new GameEngine();g.progress.quantity("Амулет Талоса",1);assertEquals(3,g.shoutRecovery());assertTrue(g.progress.equip("Амулет Талоса"));assertEquals(2,g.shoutRecovery());g.progress.unequip("Амулет Талоса");assertEquals(3,g.shoutRecovery());
    }
    @Test public void gearDoesNotStackOnSync(){
        GameEngine g=new GameEngine();int base=g.maxHp;
        g.progress.quantity("Амулет Аркея",1);assertTrue(g.progress.equip("Амулет Аркея"));
        g.syncGearAttributes();g.syncGearAttributes();assertEquals(base+10,g.maxHp);
        g.progress.unequip("Амулет Аркея");g.syncGearAttributes();assertEquals(base,g.maxHp);
    }
}
