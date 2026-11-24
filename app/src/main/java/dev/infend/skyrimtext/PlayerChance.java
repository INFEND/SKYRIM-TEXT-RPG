package dev.infend.skyrimtext;

import java.util.Locale;

/** Shared probability calculation. The model supplies the situational base only. */
final class PlayerChance {
    static final class Result {
        final int chance;
        final String explanation;
        Result(int chance,String explanation){this.chance=chance;this.explanation=explanation;}
    }
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace('ё','е').trim();}
    private static String signed(int n){return (n>=0?"+":"")+n+" п.п.";}
    static int inferSkill(String action){
        String s=norm(action);
        if(s.matches(".*(убежд|уговор|торг|переговор|договор|краснореч|разговор|поговор|обман|спрос).*"))return 12;
        if(s.matches(".*(взлом|отмыч|замок).*"))return 10;
        if(s.matches(".*(краст|украст|карман).*"))return 11;
        if(s.matches(".*(скрыт|незамет|крадуч).*"))return 9;
        return -1;
    }
    static boolean routine(String title,String detail){
        String t=norm(title), both=t+" "+norm(detail);
        if(both.matches(".*(враг|вражд|драк|опасн|заперт|угроз|насил|принуд|обман|убий|украст|скрыт|незамет|убед|уговор|шантаж|охраня|запрещ).*"))return false;
        try{if(AiRoutineAction.requiresNoCheck(t))return true;}catch(RuntimeException|LinkageError ignored){}
        return t.matches("^(?:пойти|подойти|подойти поближе|пройти) (?:к|до) [^,;.!?]{1,65} и (?:сесть|присесть)(?: рядом| около нее| возле нее| за стол)?[.!]?$");
    }
    static Result calculate(GameEngine g,int base,int skill,String title,String detail){
        if(routine(title,detail)||base<0)return new Result(-1,"Обычное действие без проверки навыка.\nБазовая выполнимость: 100%.\nИТОГ: 100%\nСогласие NPC и награды зависят от ситуации.");
        if(skill<0)skill=inferSkill(title+" "+detail);
        int level=skill>=0?g.progress.skillLevel(skill):0;
        int bonus=(int)Math.round(level*.55);
        int situation=(int)Math.round((base-50)*.45);
        int reputation=skill==12?Math.max(-5,Math.min(10,g.reputation/10)):0;
        int perks=skill>=0?Math.min(15,Math.max(0,g.progress.branches.chanceBonus(skill)+g.progress.advanced.checkBonus(skill))):0;
        int raw=65+bonus+situation+reputation+perks;
        int chance=Math.max(5,Math.min(100,raw));
        return new Result(chance,"Базовый шанс игрока: 65%\n"+
            "Обстановка (оценка ИИ: "+base+" из 100): "+signed(situation)+"\n"+
            (skill>=0?RpgContent.SKILLS[skill]+" (уровень "+level+" × 0,55): ":"Навык не определён: ")+signed(bonus)+"\n"+
            "Репутация: "+signed(reputation)+"\nТаланты: "+signed(perks)+"\n"+
            "Сумма: "+raw+"%\nОграничение 5–100%: "+chance+"%\nИТОГ: "+chance+"%");
    }
}
