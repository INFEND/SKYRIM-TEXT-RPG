package dev.infend.skyrimtext;

/** Stable appearance keys, shared by the builder and schema-1 saves. */
public final class CharacterIdentity {
    public static final String MALE="Мужской", FEMALE="Женский";
    public static final String[] GENDERS={MALE,FEMALE};
    public static final int PORTRAITS_PER_GENDER=3;
    // Same order as RpgContent.RACES; filenames do not depend on translated labels.
    private static final String[] RACE_KEYS={"nord","breton","dunmer","khajiit","imperial",
        "orc","redguard","altmer","bosmer","argonian"};
    private CharacterIdentity(){}
    public static String normalizeGender(String value){return FEMALE.equals(value)?FEMALE:MALE;}
    public static int normalizePortrait(int value){return Math.max(1,Math.min(PORTRAITS_PER_GENDER,value));}
    public static String portraitPath(String race,String gender,int portrait){
        return "portraits/"+RACE_KEYS[RpgContent.raceIndex(race)]+"_"+
            (FEMALE.equals(gender)?"female":"male")+"_"+normalizePortrait(portrait)+".webp";
    }
}
