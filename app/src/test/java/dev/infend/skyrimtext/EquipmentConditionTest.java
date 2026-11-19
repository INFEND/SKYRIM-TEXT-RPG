package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class EquipmentConditionTest {
    @Test public void conditionPersistsAndAffectsStat(){
        RpgProgress p=new RpgProgress();ItemCatalog.Item item=ItemCatalog.find("Железный меч");
        p.quantity(item.id,1);
        double full=item.currentStat(p);
        p.wearItem(item,40);
        assertEquals(60,p.conditionPercent(item));
        assertEquals(full*.8,item.currentStat(p),.0001);
        RpgProgress restored=RpgProgress.fromJson(p.toJson());
        assertEquals(60,restored.conditionPercent(item));
        restored.wearItem(item,1000);
        assertEquals(0,restored.conditionPercent(item));
        assertEquals(.5,restored.conditionMultiplier(item),.0001);
        restored.quantity(item.id,0);restored.quantity(item.id,1);
        assertEquals(100,restored.conditionPercent(item));
    }
    @Test public void noChargeRows(){
        assertEquals(1,ItemEffectRow.parse("Заряд зачарования: 75% | Урон огнём: 10 ед.").size());
    }
}
