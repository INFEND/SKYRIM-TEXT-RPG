package dev.infend.skyrimtext;

/** City workshop adapter: existing inventory, skill, temper and potion systems. */
final class SolitudeCraft {
    private static final String PREFIX="SOLITUDE:craft:";
    private static final String[] OUTPUT={"Железный меч","Железный кинжал","Стальной меч","Кожаные полоски","Железный боевой топор","Железный щит","Железный шлем","Железная кираса","Стальной кинжал","Стальной боевой топор","Стальной щит","Стальной шлем","Стальная кираса"};
    private static final String[] MATERIAL={"Железный слиток","Железный слиток","Стальной слиток","Кожа","Железный слиток","Железный слиток","Железный слиток","Железный слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток","Стальной слиток"};
    private static final int[] METAL={2,1,2,1,2,4,3,5,1,2,4,3,5}, STRIPS={1,1,1,0,2,1,2,3,1,2,1,2,3}, COUNT={1,1,1,4,1,1,1,1,1,1,1,1,1};
    private static boolean forge(GameEngine g){
        return SolitudeCity.here(g)&&SolitudeCourt.ready(g)&&("SOLITUDE:5".equals(g.mode)||"SOLITUDE:store:4".equals(g.mode))
            &&SolitudeShops.open(g,4)&&SolitudeEncounters.availableForWork(g,5,1)&&g.stamina>=5;
    }
    private static boolean lab(GameEngine g){
        return SolitudeCity.here(g)&&SolitudeCourt.ready(g)&&"SOLITUDE:store:1".equals(g.mode)&&SolitudeShops.open(g,1)&&g.hour+1<=20&&g.stamina>=3;
    }
    private static boolean canMake(GameEngine g,int i){
        if(!forge(g)||g.progress.quantity(MATERIAL[i])<METAL[i]||g.progress.quantity("Кожаные полоски")<STRIPS[i])return false;
        if((i==2||i>=8)&&g.progress.skillLevel(6)<20)return false;
        if((i==2&&g.swordUpgraded)||(i==12&&g.armorUpgraded))return false;
        ItemCatalog.Item out=ItemCatalog.find(OUTPUT[i]),mat=ItemCatalog.find(MATERIAL[i]),strip=ItemCatalog.find("Кожаные полоски");
        if(out==null||mat==null||strip==null||g.progress.temperQuality(out.id)>0||g.progress.quantity(out.id)>Integer.MAX_VALUE-COUNT[i])return false;
        return g.progress.carryWeight()-METAL[i]*mat.weight-STRIPS[i]*strip.weight+COUNT[i]*out.weight<=g.progress.carryLimit()+0.00001;
    }
    private static void finish(GameEngine g,String text,int stamina){
        g.stamina-=stamina;g.hour++;g.syncLivingSystems();g.message=text;g.progress.recent=text;g.recordVisualEvent(text);g.progress.sync(g);
    }
    static void choices(GameEngine g,GameEngine.Scene s){
        if(forge(g)){
            for(int i=0;i<OUTPUT.length;i++)if(canMake(g,i))s.add(PREFIX+"make:"+i,"Изготовить: "+OUTPUT[i]+" ×"+COUNT[i],METAL[i]+" × "+MATERIAL[i]+(STRIPS[i]>0?" + "+STRIPS[i]+" × Кожаные полоски":"")+" | 1 ч. | 5 запаса сил");
            for(ItemCatalog.Item item:g.progress.owned("Все","Название"))if(!item.temperMaterial().isEmpty()&&g.progress.temperBlock(item,g).isEmpty())
                s.add(PREFIX+"temper:"+item.id,"Улучшить: "+item.name,"1 × "+item.temperMaterial()+" | "+g.progress.temperTargetName(item)+" | 1 ч.");
        }
        if(lab(g)&&FixShopHome.canBrew(g))s.add(PREFIX+"brew","Сварить лечебное зелье","Пшеница + голубой горноцвет | 1 ч. | 3 запаса сил");
    }
    static boolean choose(GameEngine g,String a){
        if(!a.startsWith(PREFIX))return false;
        if(a.equals(PREFIX+"brew")){
            if(!lab(g)||!FixShopHome.canBrew(g))return true;
            String mode=g.mode;if(FixShopHome.brew(g)){g.mode=mode;finish(g,"В лаборатории Анжелины приготовлено лечебное зелье.",3);}return true;
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
