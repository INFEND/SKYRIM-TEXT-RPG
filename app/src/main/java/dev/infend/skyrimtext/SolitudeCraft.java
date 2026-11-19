package dev.infend.skyrimtext;

/** City workshop adapter: existing inventory, skill, temper and potion systems. */
final class SolitudeCraft {
    private static final String PREFIX="SOLITUDE:craft:";
    private static final String[] OUTPUT={"Железный меч","Железный кинжал","Стальной меч","Кожаные полоски","Железный боевой топор","Железный щит","Железный шлем","Железная кираса","Стальной кинжал","Стальной боевой топор","Стальной щит","Стальной шлем","Стальная кираса","Железная булава","Железный двуручный меч","Железная секира","Железный боевой молот","Железные перчатки","Железные сапоги","Стальная булава","Стальной двуручный меч","Стальная секира","Стальной боевой молот","Стальные перчатки","Стальные сапоги"};
    private static final String[] MATERIAL={"Железный слиток","Железный слиток","Стальной слиток","Кожа","Железный слиток","Железный слиток","Железный слиток","Железный слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток","Железный слиток","Железный слиток","Железный слиток","Железный слиток","Железный слиток","Железный слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток"};
    private static final int[] METAL={2,1,2,1,2,4,3,5,1,2,4,3,5,3,4,4,4,2,3,3,4,4,4,2,3}, STRIPS={1,1,1,0,2,1,2,3,1,2,1,2,3,1,2,2,3,2,2,1,2,2,3,2,2}, COUNT={1,1,1,4,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
    private static boolean forge(GameEngine g){
        return SolitudeCity.here(g)&&SolitudeCourt.ready(g)&&("SOLITUDE:5".equals(g.mode)||"SOLITUDE:store:4".equals(g.mode))
            &&SolitudeShops.open(g,4)&&SolitudeEncounters.availableForWork(g,5,1)&&g.stamina>=5;
    }
    private static boolean lab(GameEngine g){
        return SolitudeCity.here(g)&&SolitudeCourt.ready(g)&&g.stamina>=3&&g.hour<23&&(g.solitude.homeLaboratory(g)||("SOLITUDE:store:1".equals(g.mode)&&SolitudeShops.open(g,1)&&g.hour+1<=20));
    }
    private static boolean canMake(GameEngine g,int i){
        if(!forge(g)||g.progress.quantity(MATERIAL[i])<METAL[i]||g.progress.quantity("Кожаные полоски")<STRIPS[i])return false;
        if("Стальной слиток".equals(MATERIAL[i])&&g.progress.skillLevel(6)<20)return false;
        if((i==2&&g.swordUpgraded)||(i==12&&g.armorUpgraded))return false;
        ItemCatalog.Item out=ItemCatalog.find(OUTPUT[i]),mat=ItemCatalog.find(MATERIAL[i]),strip=ItemCatalog.find("Кожаные полоски");
        if(out==null||mat==null||strip==null||g.progress.temperQuality(out.id)>0||g.progress.quantity(out.id)>Integer.MAX_VALUE-COUNT[i])return false;
        return g.progress.carryWeight()-METAL[i]*mat.weight-STRIPS[i]*strip.weight+COUNT[i]*out.weight<=g.progress.carryLimit()+0.00001;
    }
    private static final String[] ALCHEMY_A={"Красный горноцвет","Пурпурный горноцвет"}, ALCHEMY_B={"Снежные ягоды","Лаванда"};
    private static final String[] ALCHEMY_OUT={"Зелье магии","Зелье запаса сил"};
    private static boolean canMix(GameEngine g,int i){
        if(!lab(g)||g.progress.quantity(ALCHEMY_A[i])<1||g.progress.quantity(ALCHEMY_B[i])<1)return false;
        if(i==0&&g.manaPotions==Integer.MAX_VALUE)return false;
        if(g.progress.quantity(ALCHEMY_OUT[i])==Integer.MAX_VALUE)return false;
        return g.progress.carryWeight()+0.3<=g.progress.carryLimit()+0.00001;
    }
    private static void mix(GameEngine g,int i){
        if(!canMix(g,i))return;
        if(!g.progress.removeItem(ALCHEMY_A[i],1))return;
        if(!g.progress.removeItem(ALCHEMY_B[i],1)){g.progress.addItem(ALCHEMY_A[i],1);return;}
        if(i==0)g.manaPotions++;
        else if(!g.progress.addItem(ALCHEMY_OUT[i],1)){
            g.progress.addItem(ALCHEMY_A[i],1);g.progress.addItem(ALCHEMY_B[i],1);return;
        }
        g.progress.skillExperience(g,7,25);finish(g,(g.solitude.homeLaboratory(g)?"В Высоком шпиле приготовлено: ":"В лаборатории Анжелины приготовлено: ")+ALCHEMY_OUT[i],3);
    }
    private static void finish(GameEngine g,String text,int stamina){
        g.stamina-=stamina;g.hour++;g.syncLivingSystems();g.message=text;g.progress.recent=text;g.recordVisualEvent(text);g.progress.sync(g);
    }
    static final String ENCHANT_RING="Серебряное кольцо грузоподъёмности", SOUL_GEM="Малый камень душ (заполненный)", BASE_RING="Кольцо серебряное";
    static final String BASE_NECK="Серебряное ожерелье", ENCHANT_NECK="Серебряное ожерелье защиты";
    static final String BASE_SWORD="Железный меч", ENCHANT_SWORD="Зачарованный железный меч силы";
    private static boolean canEnchant(GameEngine g,String base,String out){
        ItemCatalog.Item source=ItemCatalog.find(base),result=ItemCatalog.find(out),gem=ItemCatalog.find(SOUL_GEM);
        if(source==null||result==null||gem==null||g.progress.temperQuality(base)>0)return false;
        return SolitudeCity.here(g)&&SolitudeCourt.ready(g)&&g.solitude.homeEnchanting(g)&&g.hour<23&&g.stamina>=3
            &&g.progress.quantity(base)>0&&!g.progress.equipped(base)&&g.progress.quantity(SOUL_GEM)>0
            &&g.progress.quantity(out)<Integer.MAX_VALUE
            &&g.progress.carryWeight()-source.weight-gem.weight+result.weight<=g.progress.carryLimit()+0.00001;
    }
    private static void enchant(GameEngine g,String base,String out){
        if(!canEnchant(g,base,out))return;
        int rings=g.progress.quantity(base),gems=g.progress.quantity(SOUL_GEM);
        if(!g.progress.removeItem(base,1))return;
        if(!g.progress.removeItem(SOUL_GEM,1)||!g.progress.addItem(out,1)){
            g.progress.quantity(base,rings);g.progress.quantity(SOUL_GEM,gems);return;
        }
        g.progress.skillExperience(g,8,25);finish(g,"В Высоком шпиле зачаровано: "+out,3);
    }
    static void choices(GameEngine g,GameEngine.Scene s){
        if(canEnchant(g,BASE_SWORD,ENCHANT_SWORD))s.add(PREFIX+"enchant:sword","Зачаровать железный меч","Авторский рецепт | +3 базового урона | заполненный малый камень душ | 1 ч.");
        if(canEnchant(g,BASE_NECK,ENCHANT_NECK))s.add(PREFIX+"enchant:neck","Зачаровать серебряное ожерелье","Авторский рецепт | +15 брони при ношении | заполненный малый камень душ | 1 ч.");
        if(canEnchant(g,BASE_RING,ENCHANT_RING))s.add(PREFIX+"enchant:ring","Зачаровать серебряное кольцо","Авторский рецепт | +10 кг при ношении | заполненный малый камень душ | 1 ч.");
        if(forge(g)){
            for(int i=0;i<OUTPUT.length;i++)if(canMake(g,i))s.add(PREFIX+"make:"+i,"Изготовить: "+OUTPUT[i]+" ×"+COUNT[i],METAL[i]+" × "+MATERIAL[i]+(STRIPS[i]>0?" + "+STRIPS[i]+" × Кожаные полоски":"")+" | 1 ч. | 5 запаса сил");
            for(ItemCatalog.Item item:g.progress.owned("Все","Название"))if(!item.temperMaterial().isEmpty()&&g.progress.temperBlock(item,g).isEmpty())
                s.add(PREFIX+"temper:"+item.id,"Улучшить: "+item.name,"1 × "+item.temperMaterial()+" | "+g.progress.temperTargetName(item)+" | 1 ч.");
        }
        for(int i=0;i<ALCHEMY_OUT.length;i++)if(canMix(g,i))s.add(PREFIX+"mix:"+i,"Сварить: "+ALCHEMY_OUT[i],ALCHEMY_A[i]+" + "+ALCHEMY_B[i]+" | Адаптированный рецепт | 1 ч. | 3 запаса сил");
        if(lab(g)&&FixShopHome.canBrew(g))s.add(PREFIX+"brew","Сварить лечебное зелье","Пшеница + голубой горноцвет | 1 ч. | 3 запаса сил");
    }
    static boolean choose(GameEngine g,String a){
        if(!a.startsWith(PREFIX))return false;
        if(a.equals(PREFIX+"enchant:sword")){enchant(g,BASE_SWORD,ENCHANT_SWORD);return true;}
        if(a.equals(PREFIX+"enchant:ring")){enchant(g,BASE_RING,ENCHANT_RING);return true;}
        if(a.equals(PREFIX+"enchant:neck")){enchant(g,BASE_NECK,ENCHANT_NECK);return true;}
        for(int i=0;i<ALCHEMY_OUT.length;i++)if(a.equals(PREFIX+"mix:"+i)){mix(g,i);return true;}
        if(a.equals(PREFIX+"brew")){
            if(!lab(g)||!FixShopHome.canBrew(g))return true;
            String mode=g.mode;if(FixShopHome.brew(g)){g.mode=mode;finish(g,g.solitude.homeLaboratory(g)?"В Высоком шпиле приготовлено лечебное зелье.":"В лаборатории Анжелины приготовлено лечебное зелье.",3);}return true;
        }
        if(!forge(g))return true;
        if(a.startsWith(PREFIX+"temper:")){
            ItemCatalog.Item item=ItemCatalog.find(a.substring((PREFIX+"temper:").length()));
            if(item!=null&&g.progress.temper(item,g))finish(g,"У Бейранда улучшено: "+item.name+" | "+g.progress.temperQualityName(item.id),5);
            return true;
        }
        for(int i=0;i<OUTPUT.length;i++)if(a.equals(PREFIX+"make:"+i)){
            if(!canMake(g,i))return true;
            int oldMat=g.progress.quantity(MATERIAL[i]),oldStrip=g.progress.quantity("Кожаные полоски");
            if(!g.progress.removeItem(MATERIAL[i],METAL[i]))return true;
            if((STRIPS[i]>0&&!g.progress.removeItem("Кожаные полоски",STRIPS[i]))||!g.progress.addItem(OUTPUT[i],COUNT[i])){
                g.progress.quantity(MATERIAL[i],oldMat);g.progress.quantity("Кожаные полоски",oldStrip);return true;
            }
            g.progress.skillExperience(g,6,20);finish(g,"У Бейранда изготовлено: "+OUTPUT[i]+" ×"+COUNT[i],5);return true;
        }
        return true;
    }
}
