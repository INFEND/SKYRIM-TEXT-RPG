package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Eight optional, authored requests from established Solitude residents.
 * Inventory and questOutcomes are the authority: an item is consumed only once,
 * delivery is stored separately from the one-time choice of final reward.
 * No original Skyrim quest is replaced or claimed to be reproduced here.
 */
final class SolitudeFavors {
    private SolitudeFavors(){}
    private static final String PREFIX="SOLITUDE:favor:";
    // Indices point into SolitudeResidents, not the global NPC directory.
    private static final int[] RESIDENT={1,2,3,8,9,10,11,13};
    private static final String[] KEY={"sorex","addvar","pantea","evette","illdi","noster","giraud","lisette"};
    private static final String[] TITLE={
        "Припасы для трактира","Соль для улова","Цветы для коллегии","Ягоды для вина",
        "Травы для ученицы","Кожа для старого снаряжения","Переплёт для архива","Свет для вечерней песни"
    };
    private static final String[] ITEM={
        "Пшеница","Соль","Лаванда","Снежные ягоды",
        "Голубой горноцвет","Кожа","Кожаные полоски","Факел"
    };
    private static final int[] AMOUNT={3,2,2,2,2,2,3,2};
    private static final int[] PAYMENT={45,32,38,38,28,42,40,35};
    private static final String[] OFFER={
        "Сорекс просит достать пшеницу для завтрашней кухни «Смеющейся крысы».",
        "Аддвар ищет соль для сохранения свежего улова.",
        "Пантея собирает лаванду для оформления зала перед занятиями.",
        "Иветте Сан нужны снежные ягоды для пряного напитка.",
        "Илди просит голубой горноцвет для учебных нужд коллегии.",
        "Ностер хочет подлатать старое снаряжение и ищет кожу.",
        "Гирауду нужны кожаные полоски для переплёта архивных книг.",
        "Лисетта просит факелы для освещения вечернего выступления."
    };
    static int count(){return KEY.length;}
    static int resident(int i){return RESIDENT[i];}
    static String item(int i){return ITEM[i];}
    static int amount(int i){return AMOUNT[i];}
    private static String key(int i){return "solitude_favor_"+KEY[i];}
    private static String deliveredKey(int i){return key(i)+"_delivered";}
    private static boolean delivered(GameEngine g,int i){
        return "COMPLETED".equals(g.progress.questOutcome(deliveredKey(i)));
    }
    private static boolean completed(GameEngine g,int i){
        return "COMPLETED".equals(g.progress.questOutcome(key(i)));
    }
    private static boolean active(GameEngine g,int i){
        return "ACTIVE".equals(g.progress.questOutcome(key(i)));
    }
    private static boolean canDeliver(GameEngine g,int i,int d){
        int resident=RESIDENT[i];
        return active(g,i)&&!delivered(g,i)&&g.stamina>=2&&g.hour<23
            &&SolitudeResidents.present(g,resident,d)
            &&SolitudeResidents.district(resident,g.hour+1)==d
            &&g.progress.quantity(ITEM[i])>=AMOUNT[i];
    }
    static String memory(GameEngine g,int resident){
        for(int i=0;i<count();i++)if(RESIDENT[i]==resident){
            if(completed(g,i))return " | Помнит помощь: "+TITLE[i];
            if(delivered(g,i))return " | Получил припасы: "+TITLE[i];
            if(active(g,i))return " | Ожидает: "+AMOUNT[i]+" × "+ITEM[i];
        }
        return "";
    }
    static void choices(GameEngine g,GameEngine.Scene scene,int resident){
        for(int i=0;i<count();i++)if(RESIDENT[i]==resident){
            if(completed(g,i))continue;
            if(!active(g,i))scene.add(PREFIX+i+":accept","Помочь: "+TITLE[i],
                "Авторское поручение | "+OFFER[i]);
            else if(!delivered(g,i)){
                String detail="Требуется "+AMOUNT[i]+" × "+ITEM[i]+" | В сумке: "+g.progress.quantity(ITEM[i]);
                if(canDeliver(g,i,SolitudeResidents.district(resident,g.hour)))
                    scene.add(PREFIX+i+":deliver","Передать припасы",detail+" | 1 ч., −2 запаса сил");
                else scene.add(PREFIX+i+":status","Проверить поручение",detail+" | 1 ч. до окончания рабочего времени");
            }else{
                if(g.gold<=Integer.MAX_VALUE-PAYMENT[i])
                    scene.add(PREFIX+i+":pay","Получить плату: "+PAYMENT[i]+" септимов","Помощь жителю | Отношение улучшится");
                scene.add(PREFIX+i+":gift","Оставить помощь безвозмездной","Без септимов | дополнительная репутация");
            }
        }
    }
    private static void report(GameEngine g,String message){
        g.message=message;g.progress.recent=message;g.recordVisualEvent(message);g.progress.sync(g);
    }
    private static void remember(GameEngine g,int resident){
        try{
            g.sceneState.setContext(g.day,g.hour,"Солитьюд");
            String key="solitude_"+SolitudeResidents.IDS[resident];
            JSONArray facts=g.sceneState.npcFacts();
            for(int x=0;x<facts.length();x++){
                JSONObject f=facts.optJSONObject(x);
                if(f!=null&&SolitudeResidents.NAMES[resident].equalsIgnoreCase(f.optString("title"))){
                    key=f.optString("id");break;
                }
            }
            String previous=g.sceneState.status("npc",key);
            if("HELPED".equals(previous))g.sceneState.revisitNpc(key);
            else g.sceneState.confirm("npc",key,SolitudeResidents.NAMES[resident],"HELPED");
        }catch(org.json.JSONException ignored){}
    }
    static boolean choose(GameEngine g,int district,String action){
        if(!action.startsWith(PREFIX))return false;
        for(int i=0;i<count();i++){
            String prefix=PREFIX+i+":";
            if(!action.startsWith(prefix))continue;
            int resident=RESIDENT[i];
            if(!SolitudeResidents.present(g,resident,district))return true;
            if(action.equals(prefix+"accept")&&g.progress.questOutcome(key(i))==null){
                g.progress.setQuestOutcome(key(i),"ACTIVE");
                report(g,"Авторское поручение: "+TITLE[i]+". "+OFFER[i]
                    +"\nЦель: принести "+AMOUNT[i]+" × "+ITEM[i]+" персонажу "+SolitudeResidents.NAMES[resident]+".");
                return true;
            }
            if(action.equals(prefix+"status")&&active(g,i)&&!delivered(g,i)){
                report(g,"Авторское поручение: "+TITLE[i]+". Принеси "+AMOUNT[i]+" × "+ITEM[i]
                    +". Сейчас в сумке: "+g.progress.quantity(ITEM[i])+". Передача займёт один час.");
                return true;
            }
            if(action.equals(prefix+"deliver")&&canDeliver(g,i,district)){
                // Remove first; changing the quest state is never allowed to duplicate an item.
                if(!g.progress.removeItem(ITEM[i],AMOUNT[i]))return true;
                g.stamina-=2;g.hour++;g.syncLivingSystems();
                g.progress.setQuestOutcome(deliveredKey(i),"COMPLETED");
                report(g,SolitudeResidents.NAMES[resident]+" принимает припасы. "
                    +"Теперь выбери оплату или помощь безвозмездно.");
                return true;
            }
            boolean gift=action.equals(prefix+"gift");
            if((gift||action.equals(prefix+"pay"))&&active(g,i)&&delivered(g,i)){
                if(!gift&&g.gold>Integer.MAX_VALUE-PAYMENT[i])return true;
                // Award the reward exactly once, guarded by the terminal quest state.
                g.progress.setQuestOutcome(key(i),"COMPLETED");
                if(!gift)g.gold+=PAYMENT[i];
                g.holdReputation[3]=Math.min(100,g.holdReputation[3]+(gift?3:1));
                g.addXp(30);remember(g,resident);
                report(g,"Авторское поручение завершено: "+TITLE[i]+". "
                    +(gift?"Ты отказываешься от оплаты. Репутация +3.":"Получено "+PAYMENT[i]+" септимов. Репутация +1.")
                    +" Отношение жителя улучшилось. Опыт +30.");
                return true;
            }
            return true;
        }
        return true;
    }
}
