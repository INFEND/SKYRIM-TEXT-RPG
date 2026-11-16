package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.Locale;

/** AI chooses a permitted NPC initiative. Only native rules change saved state. */
final class KatherineInitiative {
    static final String REST="REST", TALK="TALK", STUDY="STUDY", GUARD="GUARD", ATTACK="ATTACK", BALANCE="BALANCE", SUPPLIES="SUPPLIES", DAMON="DAMON", ALMA="ALMA", BOUNDARIES="BOUNDARIES";
    static ArrayList<String> allowed(GameEngine g){
        ArrayList<String> out=new ArrayList<>();
        KatherineStory k=g.katherine;KatherineCompanion c=k.companion;
        if(!g.started||!k.enabled||k.stage<5||!k.present(g)||!k.companion.aiInitiatives||g.enemyHp>0||"COMBAT".equals(g.mode)||c.lastAiDay==g.day||(k.active()&&k.trust<30))return out;
        out.add(TALK);if(k.active())out.add(BALANCE);
        if(k.trust<45||c.hurt>=25)out.add(BOUNDARIES);
        if(c.hp<c.maxHp()/2||k.mana<k.maxMana()/2||g.hp<g.maxHp/2)out.add(REST);
        if(k.magicRank>0&&k.mana>=12&&c.hp>0)out.add(STUDY);
        if(k.active()&&c.hp>0){out.add(GUARD);if(k.mana>=20)out.add(ATTACK);}
        if(k.active()&&((c.healthPotions>0&&c.hp<c.maxHp()/3)||(c.magicPotions>0&&k.mana<k.maxMana()/4)))out.add(SUPPLIES);
        if(KatherineStory.atHut(g)&&c.damonSafe)out.add(DAMON);
        if(k.active()&&g.alma.active()&&c.lastJoint!=g.day)out.add(ALMA);
        return out;
    }
    static String context(GameEngine g){ArrayList<String> a=allowed(g);return a.isEmpty()?"":"Инициатива Кэтрин (на свой выбор, не обязательна): effects.katherineIntent="+String.join("|",a)+". Только один код; движок проверит условия.";}
    static boolean apply(GameEngine g,String raw,boolean acted){
        if(!acted||raw==null||raw.length()>24)return false;
        String intent=raw.trim().toUpperCase(Locale.ROOT);
        if(!allowed(g).contains(intent))return false;
        KatherineStory k=g.katherine;KatherineCompanion c=k.companion;
        String line;
        switch(intent){
            case REST:c.currentGoal="Найти безопасное место для отдыха";line="Кэтрин: Нам нужен привал. Я не хочу рисковать жизнью из-за усталости.";break;
            case TALK:c.currentGoal="Обсудить совместные планы";line="Кэтрин первой предлагает поговорить о будущем и спрашивает о твоих намерениях.";break;
            case STUDY:c.currentGoal="Исследовать Нулевой Предел";k.mana-=12;c.gain(10);line="Кэтрин самостоятельно проверяет устойчивость магического источника.";break;
            case GUARD:k.stance="DEFENSE";c.currentGoal="Защитить отряд";line="Кэтрин выбирает защитную тактику.";break;
            case ATTACK:k.stance="ATTACK";c.currentGoal="Подготовиться к угрозе";line="Кэтрин решает сосредоточиться на наступательной магии.";break;
            case BALANCE:k.stance="BALANCED";c.currentGoal="Сопровождать героя";line="Кэтрин выбирает сбалансированную тактику.";break;
            case SUPPLIES:line=c.useSupplies(k);if(line.isEmpty())return false;break;
            case DAMON:c.currentGoal="Убедиться в безопасности Деймона";line="Кэтрин навещает Деймона и обсуждает с ним меры предосторожности.";break;
            case ALMA:c.lastJoint=g.day;c.currentGoal="Согласовать действия с Альмой";k.jealousy=KatherineStory.clamp(k.jealousy-3);g.alma.jealousy=AlmaStory.clamp(g.alma.jealousy-3);g.alma.remember("Кэтрин предложила спокойно обсудить совместные планы.");line="Кэтрин сама начинает разговор с Альмой о сотрудничестве.";break;
            case BOUNDARIES:c.currentGoal="Обсудить личные границы";line="Кэтрин: Мне важно, чтобы мои решения уважали. Давай поговорим без давления.";break;
            default:return false;
        }
        c.lastAiDay=g.day;c.lastAiIntent=intent;k.remember(g,line);return true;
    }
    static boolean protectedFact(String id,String title){
        String key=id==null?"":id.toLowerCase(Locale.ROOT);
        String name=title==null?"":title.trim().toLowerCase(Locale.ROOT);
        return key.equals(KatherineStory.ID)||key.startsWith("katherine")||key.equals("kath")||name.equals("кэтрин")||name.equals("katherine");
    }
}
