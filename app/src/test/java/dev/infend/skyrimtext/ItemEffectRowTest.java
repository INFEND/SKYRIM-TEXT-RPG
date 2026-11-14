package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class ItemEffectRowTest {
    @Test public void mixedEffectsAndDuration(){
        java.util.ArrayList<ItemEffectRow> rows=ItemEffectRow.parse("Восстановление здоровья: 45 ед. | Уязвимость к огню: +50%, 45 сек. | Урон запасу сил: 30 ед.");
        assertEquals(3,rows.size());
        assertEquals("+45 ед.",rows.get(0).value);
        assertEquals(1,rows.get(0).direction);
        assertEquals("+50%",rows.get(1).value);
        assertEquals("45 сек.",rows.get(1).duration);
        assertEquals(-1,rows.get(1).direction);
        assertEquals("−30 ед.",rows.get(2).value);
        assertEquals(-1,rows.get(2).direction);
    }
    @Test public void unknownMagnitudeRemainsUnknown(){
        ItemEffectRow r=ItemEffectRow.parse("Восстановление магии").get(0);
        assertEquals("",r.value);assertEquals("",r.duration);
        assertEquals("",ItemEffectRow.parse("Паралич: 5 сек.").get(0).value);
    }
}
