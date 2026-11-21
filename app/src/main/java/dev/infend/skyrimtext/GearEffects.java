package dev.infend.skyrimtext;
/** Explicit, supported gameplay data, rather than interpreting arbitrary narrative descriptions. */
final class GearEffects {
    static int poisonDamage(String id){
        if("Проклятие ледяного привидения".equals(id))return 50;
        if("Экстракт лотоса".equals(id))return 60;
        if("Яд обморожения".equals(id))return 20;
        return 0;
    }
    static int paralysis(String id){
        if(id==null)return 0;
        switch(id){
            case "Слабый яд паралича":case "Яд паралича":return 1;
            case "Сильный яд паралича":case "Вирулентный яд паралича":return 2;
            case "Смертельный яд паралича":return 3;
            default:return 0;
        }
    }
    static boolean knownEnchantment(String id){return id!=null&&(id.startsWith("Амулет Талоса")||id.startsWith("Амулет Зенитара")||java.util.Arrays.asList("Амулет Аркея","Амулет Андурса","Амулет Джулианоса","Амулет Кинарет","Амулет Акатоша","Амулет Дибеллы","Амулет Стендарра","Амулет Саартала","Одеяние культиста","Кольцо чистейших смесей","Очарованное ожерелье","Зачарованное кольцо").contains(id));}
    static boolean supportedPoison(String id){return poisonDamage(id)>0||paralysis(id)>0;}
}
