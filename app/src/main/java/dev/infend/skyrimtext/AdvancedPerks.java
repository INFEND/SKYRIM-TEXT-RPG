package dev.infend.skyrimtext;
import org.json.JSONArray;

/** Individual Skyrim-inspired perks, with effects adapted to the existing engine. */
final class AdvancedPerks {
    static final int FIGHTING=0,CHAMPION=1,CRITICAL_SHOT=2,SHIELD_WALL=3,WELL_FITTED=4,CUSTOM_FIT=5,STEEL=6,HAGGLING=7,NOVICE_DESTRUCTION=8,RECOVERY=9,PERSUASION=10,PHYSICIAN=11,ENCHANTER=12,STEALTH=13,LOCKSMITH=14,LIGHT_FINGERS=15,MAGE_ARMOR=16,ANIMAGE=17,SUMMONER=18;
    static final String[] NAMES={"Боевая стойка","Стойка чемпиона","Критический выстрел","Стена щитов","Полный комплект тяжёлой брони","Полный комплект лёгкой брони","Стальные доспехи","Торгаш","Новичок школы разрушения","Восстановление магии","Убеждение","Лекарь","Зачарователь","Скрытность","Взломщик","Ловкие пальцы","Магическая броня","Влияние на животных","Призыватель"};
    static final int[] SKILL={0,1,2,3,4,5,6,12,13,14,12,7,8,9,10,11,15,16,17};
    static final int[] NEED={20,20,30,20,30,30,20,20,20,30,50,20,20,20,20,20,30,20,20};
    static final String[] EFFECT={ "Сильный удар одноручным оружием: −3 к расходу выносливости.","Сильный удар двуручным оружием: −3 к расходу выносливости.","При стрельбе: +10 процентных пунктов к шансу критического удара.","При обороне с экипированным щитом: ещё 3 поглощённого урона.","Четыре части тяжёлого комплекта: +5 процентных пунктов к снижению урона.","Четыре части лёгкого комплекта: +5 процентных пунктов к снижению урона.","Улучшение стальных предметов на одну ступень выше, в пределах максимума.","Дополнительный ассортимент: скидка 5 процентных пунктов.","Боевой магический удар: расход 9 вместо 12 магии.","Оборона: дополнительно 3 единицы магии.","Проверки красноречия: +5 к контексту шанса.","Лечебные зелья и зелья запаса сил: +10 единиц восстановления.","Проверки зачарования: +8 к контексту шанса и +20% опыта навыка.","Проверки скрытности: +5 к контексту шанса; на 1 меньше расход выносливости в день пути.","Проверки взлома: +8 к контексту шанса.","Проверки карманных краж: +8 к контексту шанса.","Без защитных доспехов: +5 процентных пунктов к снижению урона.","Проверки иллюзии: +8 к контексту шанса.","Проверки колдовства: +8 к контексту шанса."};
    private final boolean[] owned=new boolean[NAMES.length];
    boolean has(int i){return i>=0&&i<owned.length&&owned[i];}
    boolean canBuy(RpgProgress p,int i){return i>=0&&i<owned.length&&!owned[i]&&p.perkPoints>0&&p.skillLevel(SKILL[i])>=NEED[i]&&p.talentRank(SKILL[i])>=1&&(i!=PERSUASION||has(HAGGLING));}
    boolean buy(RpgProgress p,int i){if(!canBuy(p,i))return false;owned[i]=true;p.perkPoints--;return true;}
    int checkBonus(int skill){switch(skill){case 8:return has(ENCHANTER)?8:0;case 9:return has(STEALTH)?5:0;case 10:return has(LOCKSMITH)?8:0;case 11:return has(LIGHT_FINGERS)?8:0;case 16:return has(ANIMAGE)?8:0;case 17:return has(SUMMONER)?8:0;default:return 0;}}
    JSONArray toJson(){JSONArray a=new JSONArray();for(boolean value:owned)a.put(value);return a;}
    static AdvancedPerks fromJson(JSONArray a){AdvancedPerks p=new AdvancedPerks();if(a!=null)for(int i=0;i<p.owned.length;i++)p.owned[i]=a.optBoolean(i,false);return p;}
}
