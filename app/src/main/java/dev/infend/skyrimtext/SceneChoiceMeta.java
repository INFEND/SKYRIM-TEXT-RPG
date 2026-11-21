package dev.infend.skyrimtext;

/** Labels only the actual skill used by a check; percentages come from RpgProgress. */
public final class SceneChoiceMeta {
    private SceneChoiceMeta(){}
    private static final int[] GENERAL_SKILLS={9,12,0,14,10,15,2,13,3,9};
    private static final int[] DRAGON_SKILLS={9,14,9,15,12,3,10,12};
    public static String skill(String action){
        if(action==null)return "";
        if(action.matches("ACT:[0-9]"))
            return RpgContent.SKILLS[GENERAL_SKILLS[action.charAt(4)-'0']]+" | Проверка навыка";
        if(action.matches("DRAGON:[0-5]:[1-8]"))
            return RpgContent.SKILLS[DRAGON_SKILLS[action.charAt(action.length()-1)-'1']]+" | Сюжетное испытание";
        switch(action){
            case "HEAVY":return RpgContent.SKILLS[1]+" | Выносливость";
            case "FLEE":return RpgContent.SKILLS[9]+" | Отступление";
            case "EVENT_HELP":return RpgContent.SKILLS[14]+" | Помощь";
            case "EVENT_SEARCH":return RpgContent.SKILLS[9]+" | Исследование";
            default:return "Проверка навыка";
        }
    }
    public static String skillName(String action){
        String name=skill(action);
        int suffix=name.indexOf(" | ");
        if(suffix<0)suffix=name.indexOf(" · "); // Legacy labels in imported saves
        return suffix<0?name:name.substring(0,suffix);
    }
    public static int chanceColor(int chance){
        if(chance>=75)return 0xff55c982;
        if(chance>=45)return 0xffff9a57;
        return 0xffe36a69;
    }
}
