package dev.infend.skyrimtext;
import org.junit.Test;
import static org.junit.Assert.*;
public class ItemEffectStyleTest {
    @Test public void expandedReferencePalette(){
        assertSame(ItemEffectStyle.CHAOS,ItemEffectStyle.classify("Урон хаосом",""));
        assertSame(ItemEffectStyle.PARALYSIS,ItemEffectStyle.classify("Паралич",""));
        assertSame(ItemEffectStyle.WEIGHT,ItemEffectStyle.classify("Переносимый вес",""));
        assertSame(ItemEffectStyle.CRAFT,ItemEffectStyle.classify("Кузнечное дело",""));
        assertSame(ItemEffectStyle.WATER,ItemEffectStyle.classify("Подводное дыхание",""));
        assertSame(ItemEffectStyle.SPELL_ABSORB,ItemEffectStyle.classify("Поглощение заклинаний",""));
        assertSame(ItemEffectStyle.MANA,ItemEffectStyle.classify("Урон магии",""));
        assertFalse(ItemEffectStyle.damageEnchantment("Сопротивление огню"));
    }
    @Test public void elementalAndResourceColors(){
        assertSame(ItemEffectStyle.FIRE,ItemEffectStyle.classify("Урон огнём: 15",""));
        assertSame(ItemEffectStyle.FROST,ItemEffectStyle.classify("Урон морозом",""));
        assertSame(ItemEffectStyle.SHOCK,ItemEffectStyle.classify("Урон электричеством",""));
        assertSame(ItemEffectStyle.SUN,ItemEffectStyle.classify("Солнечный урон",""));
        assertSame(ItemEffectStyle.POISON,ItemEffectStyle.classify("Ядовитый урон: 50.",""));
        assertSame(ItemEffectStyle.ABSORB_HEALTH,ItemEffectStyle.classify("Поглощение здоровья",""));
        assertSame(ItemEffectStyle.ABSORB_MAGIC,ItemEffectStyle.classify("Поглощение магии",""));
        assertSame(ItemEffectStyle.ABSORB_STAMINA,ItemEffectStyle.classify("Поглощение запаса сил",""));
        assertSame(ItemEffectStyle.SOUL,ItemEffectStyle.classify("Захват душ",""));
        assertSame(ItemEffectStyle.SHOCK,ItemEffectStyle.classify("Уязвимость цели к соответствующему урону: +40%","Неприятие электричества"));
        assertSame(ItemEffectStyle.FROST,ItemEffectStyle.classify("Сопротивление холоду",""));
        assertFalse(ItemEffectStyle.damageEnchantment("Уязвимость к урону огнём"));
        assertTrue(ItemEffectStyle.damageEnchantment("Урон молнией"));
    }
}
