package dev.infend.skyrimtext;

/** Two original optional adventures. Progress, purchase and endings live in the current save. */
public final class FixShopAdventure {
    static final String[] TITLES={"Печать забытого ярла","Тень лунного серебра"};
    static final String[] FIRST={"Вайтран","Рифтен"}, SECOND={"Ривервуд","Айварстед"};
    static final String[] ENEMIES={"Наёмник Чёрной Печати","Охотник за лунным серебром"};
    static final String[] INTRO={
      "У ворот Вайтрана старый архивариус протягивает тебе свёрток. На воске отпечатана неизвестная печать. Из письма следует, что старый договор был спрятан на юге, у Ривервуда. Архивариус просит разыскать доказательство прежде, чем его уничтожат.",
      "В Рифтене ювелир показывает пустую шкатулку. По его словам, серебряная пластина, на которой вырезаны имена пропавших путников, ушла вместе с обозом к горной дороге. Незнакомец уже ищет её, и торговец боится, что тот сожжёт список."};
    static final String[] DISCOVERY={
      "У Ривервуда ты замечаешь человека в дорожном плаще. Под корнями сосны виднеется медный футляр. Незнакомец выхватывает клинок: ему заплатили за то, чтобы печать исчезла.",
      "На окраине Айварстеда под каменной плитой лежит свёрток. Преследователь в кожаных перчатках появляется между деревьями. Он требует отдать серебро."};
    static final String[] LOOT={
      "В футляре старый договор. Он доказывает, что архивариус скрыл часть истории. Можно передать документ архиву или обнародовать его, чтобы восстановить справедливость.",
      "На серебре выбиты имена путников. Ювелир предлагает заплатить за пластину; однако её можно передать семьям пропавших."};
    static String id(int i){return "fix_adventure_"+i;}
    static int stage(GameEngine g,int i){return i>=0&&i<TITLES.length?g.fixShop.adventureStage[i]:0;}
    static boolean unlock(GameEngine g,int i){if(i<0||i>=TITLES.length||stage(g,i)!=0)return false;g.fixShop.adventureStage[i]=1;g.progress.setQuestOutcome(id(i),"ACTIVE");return true;}
    static String objective(GameEngine g,int i){
      switch(stage(g,i)){case 1:return "Найди заказчика: "+FIRST[i];case 2:return "Исследуй след: "+SECOND[i];case 3:return "Выбери судьбу находки";case 4:return "Завершено: находка передана заказчику";case 5:return "Завершено: правда открыта";default:return "Не приобретено";}
    }
    static boolean safe(GameEngine g){return g.started&&!g.road.pending()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode));}
    static void choices(GameEngine g,GameEngine.Scene scene){
      if(!safe(g))return;
      for(int i=0;i<TITLES.length;i++){int s=stage(g,i);if(s==1&&FIRST[i].equals(g.progress.location))scene.add("FIXQUEST:"+i+":meet",TITLES[i],objective(g,i));else if(s==2&&SECOND[i].equals(g.progress.location))scene.add("FIXQUEST:"+i+":fight","Исследовать след | "+TITLES[i],objective(g,i));else if(s==3)scene.add("FIXQUEST:"+i+":resolve",TITLES[i],objective(g,i));}
    }
    static boolean choose(GameEngine g,String action){
      if(!safe(g)||action==null||!action.matches("FIXQUEST:[01]:(meet|fight|resolve|reward|truth)"))return false;
      int i=action.charAt(9)-'0';String verb=action.substring(11);int s=stage(g,i);
      if(s==1&&"meet".equals(verb)&&FIRST[i].equals(g.progress.location)){g.fixShop.adventureStage[i]=2;g.message=INTRO[i];g.mode="RESULT";g.recordVisualEvent(TITLES[i]+": принят след");return true;}
      if(s==2&&"fight".equals(verb)&&SECOND[i].equals(g.progress.location)){g.beginFixShopBattle(i,ENEMIES[i],DISCOVERY[i]);return true;}
      if(s==3&&"resolve".equals(verb)){g.message=LOOT[i];g.mode="RESULT";return true;}
      if(s==3&&("reward".equals(verb)||"truth".equals(verb))){
          boolean truth="truth".equals(verb);g.fixShop.adventureStage[i]=truth?5:4;
          g.progress.setQuestOutcome(id(i),"COMPLETED");g.gold=GameEngine.nonnegativeAdd(g.gold,truth?100:500);
          if(truth)g.reputation=GameEngine.nonnegativeAdd(g.reputation,15);g.addXp(150);
          g.message=truth?"Ты открываешь правду. +15 репутации, +100 септимов, +150 опыта.":"Заказчик принимает находку. +500 септимов, +150 опыта.";
          g.mode="RESULT";g.progress.sync(g);g.recordVisualEvent(TITLES[i]+": "+objective(g,i));return true;
      }return false;
    }
    static void endings(GameEngine g,GameEngine.Scene scene){if(!safe(g))return;for(int i=0;i<TITLES.length;i++)if(stage(g,i)==3){scene.add("FIXQUEST:"+i+":reward","Передать заказчику | "+TITLES[i],"+500 септимов");scene.add("FIXQUEST:"+i+":truth","Открыть правду | "+TITLES[i],"+15 репутации и +100 септимов");}}
    static boolean rewriteEnding(GameEngine g,int i){
        if(!safe(g)||!g.fixShop.editorEnabled||stage(g,i)<4)return false;
        int old=stage(g,i);g.fixShop.adventureStage[i]=old==4?5:4;
        g.message="Нити судьбы: "+TITLES[i]+". "+objective(g,i)+". Награды не начисляются повторно, уже полученные не изымаются.";g.recordVisualEvent(g.message);return true;
    }
    static void victory(GameEngine g,String kind){for(int i=0;i<TITLES.length;i++)if(kind.equals("FIX_QUEST:"+i)&&stage(g,i)==2){g.fixShop.adventureStage[i]=3;g.message+="\n\n"+LOOT[i];}}
}
