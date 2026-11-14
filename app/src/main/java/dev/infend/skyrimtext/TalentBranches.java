package dev.infend.skyrimtext;
import org.json.JSONArray;

/** Additional independently purchased, mechanically effective specializations.
 * Adapted to the text game's rules, not a literal reproduction of the Skyrim perk tree.
 */
final class TalentBranches {
    static final String[][] NAMES={
        {"Точный клинок","Мастерский удар"},{"Сокрушитель","Стойкость воина"},{"Верный выстрел","Смертоносная точность"},
        {"Стена щитов","Силовой блок"},{"Стальная выучка","Непоколебимость"},{"Проворство","Свобода движений"},
        {"Умелый ремесленник","Мастерская работа"},{"Знаток составов","Чистая эссенция"},{"Рунический мастер","Глубокое понимание"},
        {"Тихая поступь","Тень"},{"Ловкие пальцы","Безупречный взлом"},{"Незаметная рука","Лёгкое касание"},
        {"Выгодная сделка","Искусство убеждения"},{"Стихийная мощь","Глубокое разрушение"},{"Целительные руки","Живительная сила"},
        {"Плотная кожа","Магическая стойкость"},{"Тонкое внушение","Власть над чувствами"},{"Связь с Обливионом","Мастер призыва"}};
    private final int[] rank=new int[NAMES.length];
    int rank(int skill){return skill>=0&&skill<rank.length?rank[skill]:0;}
    boolean canBuy(RpgProgress p,int skill){int r=rank(skill);return skill>=0&&skill<rank.length&&r<2&&p.perkPoints>0&&p.skillLevel(skill)>=(r==0?30:60)&&p.talentRank(skill)>=r+1;}
    boolean buy(RpgProgress p,int skill){if(!canBuy(p,skill))return false;p.perkPoints--;rank[skill]++;return true;}
    int chanceBonus(int skill){return 3*rank(skill);}
    int experienceBonus(int skill,int amount){return amount*rank(skill)/10;}
    JSONArray toJson(){return new JSONArray(rank);}
    static TalentBranches fromJson(JSONArray a){TalentBranches t=new TalentBranches();if(a!=null)for(int i=0;i<t.rank.length;i++)t.rank[i]=Math.max(0,Math.min(2,a.optInt(i,0)));return t;}
    static String effect(int skill){
        switch(skill){case 0:case 1:case 2:return "+5% урона подходящим оружием за талант";
            case 3:return "+2 поглощённого урона при обороне за талант";
            case 4:case 5:return "+2% снижения урона при наличии соответствующей брони за талант";
            case 12:return "+5 процентных пунктов к цене продажи за талант";
            case 13:return "+5% урона боевой магией за талант";
            case 14:return "+5 здоровья при использовании лечебного зелья за талант";
            default:return "+3 к контексту проверок и +10% опыта навыка за талант";}
    }
}
