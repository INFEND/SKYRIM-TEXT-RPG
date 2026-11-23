package dev.infend.skyrimtext;

import org.json.JSONArray;
import org.json.JSONObject;

/** Authored daily itineraries for existing Skyrim residents, not exact original AI packages. */
final class SolitudeResidents {
    private SolitudeResidents(){}
    static final String[] NAMES={"Элисиф Прекрасная","Сорекс Виний","Аддвар","Пантея Атейя","Сибилла Стентор","Гисли","Эрикул","Ахтар","Иветта Сан","Илди","Ностер Орлиный Глаз","Гирауд Жиман","Инге Шесть Пальцев","Лисетта","Джала"};
    static final String[] IDS={"elisif","sorex","addvar","pantea","sybille","gisli","erikur","ahtar","evette","illdi","noster","giraud","inge","lisette","jala"};
    private static final String[] LINES={
        "Хаафингар нуждается в спокойствии. В Синем дворце я выслушиваю тех, кто приходит с прошениями.",
        "В трактире всегда найдётся работа. Путники приносят вести из всех владений.",
        "Свежую рыбу лучше выбирать утром. Море не ждёт, пока торговец выспится.",
        "Музыка требует ежедневной практики. Даже знакомую мелодию можно сыграть лучше.",
        "При дворе ценят осторожность. Магия требует не только силы, но и знаний.",
        "В Солитьюде слишком часто судят о человеке по его родне. Мне хотелось бы, чтобы меня знали по собственному имени.",
        "Торговля требует связей и терпения. При дворе полезно знать, с кем имеешь дело.",
        "Служба при тюрьме требует выдержки. Не каждый заключённый одинаково опасен.",
        "Хорошее вино начинается с хороших пряностей. Без них торговля идёт хуже.",
        "Я учусь в Коллегии бардов. Пока мне ещё многое предстоит освоить.",
        "Когда-то я служил в Легионе. Теперь мои дни проходят на улицах столицы.",
        "История Скайрима хранится не только в книгах, но и в песнях.",
        "Я обучаю бардов искусству исполнения. Хорошая история требует терпения.",
        "В трактире путники любят песни о далёких землях.",
        "На рынке я торгую фруктами и овощами."
    };
    private static final String[] TOPICS={
        "Синий дворец открыт для прошений, но решения требуют времени.",
        "Утром я бываю на рынке, затем возвращаюсь к работе в трактире.",
        "У причалов начинается мой день, а на рынке я продаю улов.",
        "В Коллегии бардов учат не только петь, но и слушать.",
        "Магические книги требуют внимательного изучения, а не поспешных выводов.",
        "На рынке можно услышать больше городских новостей, чем за закрытыми дверями.",
        "В Синем дворце обсуждают дела владения, а в гавани — дела купцов.",
        "После службы я предпочитаю отдохнуть в трактире.",
        "На рынке важно знать, когда прибывают грузы из гавани.",
        "Виармо ценит старание учеников, а не только талант.",
        "На площади всегда найдётся кто-нибудь, кто помнит старые времена.",
        "В Коллегии бардов мы изучаем предания и старые стихи.",
        "Ученикам приходится практиковаться каждый день, даже когда хочется отдохнуть.",
        "Публика в Смеющейся крысе меняется каждый вечер.",
        "Свежие припасы лучше покупать до вечера."
    };
    private static final String[] ADVICE={
        "Если ищешь разрешение на дом, обратись к Фолку Огнебороду в Синем дворце.",
        "В трактире можно найти работу; сначала проверь часы смены.",
        "В гавани работа начинается раньше, чем на рынке.",
        "В Коллегии бардов можно практиковаться в музыке.",
        "За алхимическими припасами загляни в Ароматы Анжелины.",
        "На рынке стоит поговорить с торговцами до закрытия лавок.",
        "За торговыми припасами отправляйся в гавань или на рынок.",
        "Если у тебя есть штраф, поговори с дежурным в Мрачном замке.",
        "За делами портовых грузов обращайся к Восточной имперской компании.",
        "Загляни в Коллегию бардов, если хочешь попрактиковаться в сценической речи.",
        "В Храме Богов помогают тем, кому трудно.",
        "Если интересуешься историей, начни с Коллегии бардов.",
        "Пантея может рассказать о музыкальном искусстве.",
        "За комнатой обратись к хозяевам трактира.",
        "За другими припасами загляни в местные лавки."
    };
    // Half-open time windows; -1 means off duty and unavailable for public dialogue.
    static int district(int i,int hour){
        if(hour<0||hour>=24)return -1;
        switch(i){
            case 0:return hour>=9&&hour<17?2:-1;
            case 1:return hour>=8&&hour<10?0:hour>=10&&hour<23?6:-1;
            case 2:return hour>=6&&hour<8?7:hour>=8&&hour<17?0:-1;
            case 3:return hour>=9&&hour<17?3:hour>=17&&hour<20?6:-1;
            case 4:return hour>=10&&hour<18?2:-1;
            case 5:return hour>=10&&hour<15?0:hour>=15&&hour<19?6:-1;
            case 6:return hour>=9&&hour<15?2:hour>=15&&hour<18?7:-1;
            case 7:return hour>=8&&hour<17?1:hour>=18&&hour<21?6:-1;
            case 8:return hour>=8&&hour<18?0:-1;
            case 9:return hour>=9&&hour<17?3:hour>=17&&hour<20?6:-1;
            case 10:return hour>=8&&hour<18?0:-1;
            case 11:return hour>=9&&hour<17?3:-1;
            case 12:return hour>=9&&hour<17?3:hour>=17&&hour<20?6:-1;
            case 13:return hour>=12&&hour<23?6:-1;
            case 14:return hour>=8&&hour<18?0:-1;
            default:return -1;
        }
    }
    static boolean present(GameEngine g,int i,int d){
        if(d<0||i<0||i>=NAMES.length||district(i,g.hour)!=d)return false;
        String status=g.sceneState.npcStatusByName(NAMES[i]);
        return !"DEAD".equals(status)&&!"MISSING".equals(status);
    }
    private static JSONObject fact(GameEngine g,int i){
        JSONArray a=g.sceneState.npcFacts();
        for(int k=0;k<a.length();k++){JSONObject f=a.optJSONObject(k);if(f!=null&&NAMES[i].equalsIgnoreCase(f.optString("title")))return f;}
        return null;
    }
    static void choices(GameEngine g,GameEngine.Scene s,int d){
        for(int i=0;i<NAMES.length;i++)if(present(g,i,d)){
            JSONObject f=fact(g,i);
            if(f!=null&&"HOSTILE".equals(f.optString("status"))){
                s.add("SOLITUDE:resident:reconcile:"+i,"Попытаться примириться: "+NAMES[i],"Авторское действие | 1 час, 10 сил | один переход отношений");
                s.add("SOLITUDE:resident:history:"+i,"История отношений: "+NAMES[i],"Только просмотр");
                continue;
            }
            s.add("SOLITUDE:resident:talk:"+i,"Поговорить: "+NAMES[i],f==null?"Авторский диалог | Знакомство":"Отношение: "+f.optInt("relation",50)+"/100");
            s.add("SOLITUDE:resident:topic:"+i,"Расспросить: "+NAMES[i],"Городская жизнь | авторский диалог");
            if(f!=null&&"MET".equals(f.optString("status")))s.add("SOLITUDE:resident:trust:"+i,"Поделиться впечатлениями: "+NAMES[i],"Авторский диалог | Однократное знакомство ближе");
            if(f!=null&&f.optInt("relation",50)>=60)s.add("SOLITUDE:resident:advice:"+i,"Попросить совета: "+NAMES[i],"Авторский совет | Отношение от 60");
            if(f!=null){s.add("SOLITUDE:resident:memory:"+i,"Вспомнить встречу: "+NAMES[i],"Общая память NPC");s.add("SOLITUDE:resident:history:"+i,"История отношений: "+NAMES[i],"События и изменения отношения | только просмотр");}
            SolitudeFavors.choices(g,s,i);
        }
    }
    /** Read-only timeline; does not update last contact or grant relationship points. */
    private static String relationshipHistory(JSONObject f){
        StringBuilder out=new StringBuilder("Отношение: ").append(f.optInt("relation",50)).append("/100");
        JSONArray events=f.optJSONArray("history");
        if(events==null||events.length()==0)return out.append(" | Событий пока нет.").toString();
        for(int n=Math.max(0,events.length()-8);n<events.length();n++){
            JSONObject e=events.optJSONObject(n);
            if(e==null)continue;
            String status=e.optString("status");
            String label="MET".equals(status)?"Знакомство":"INFORMED".equals(status)?"Доверительный разговор":
                "HELPED".equals(status)?"Помощь":"HOSTILE".equals(status)?"Конфликт":
                "MISSING".equals(status)?"Пропал":"DEAD".equals(status)?"Погиб":status;
            int delta=e.optInt("delta",0);
            out.append("\n• День ").append(e.optInt("day",0)).append(", ").append(e.optInt("hour",0)).append(":00: ").append(label);
            if(delta!=0)out.append(" (").append(delta>0?"+":"").append(delta).append(")");
            String location=e.optString("location");
            if(!location.isEmpty())out.append(" | ").append(location);
        }
        return out.toString();
    }
    static boolean choose(GameEngine g,int d,String a){
        if(SolitudeFavors.choose(g,d,a))return true;
        if(!a.startsWith("SOLITUDE:resident:"))return false;
        for(int i=0;i<NAMES.length;i++)if(present(g,i,d)){
            boolean reconcile=a.equals("SOLITUDE:resident:reconcile:"+i);
            boolean memory=a.equals("SOLITUDE:resident:memory:"+i);
            boolean history=a.equals("SOLITUDE:resident:history:"+i);
            boolean topic=a.equals("SOLITUDE:resident:topic:"+i);
            boolean trust=a.equals("SOLITUDE:resident:trust:"+i);
            boolean advice=a.equals("SOLITUDE:resident:advice:"+i);
            if(!reconcile&&!memory&&!history&&!topic&&!trust&&!advice&&!a.equals("SOLITUDE:resident:talk:"+i))continue;
            JSONObject f=fact(g,i);if((memory||history)&&f==null)return true;
            if(reconcile){
                if(f==null||!"HOSTILE".equals(f.optString("status")))return true;
                if(g.hour>=23||g.stamina<10||district(i,g.hour+1)!=d){g.message="Для примирения нужны один час, 10 единиц запаса сил и присутствие жителя до конца разговора.";return true;}
                try{
                    g.sceneState.setContext(g.day,g.hour,"Солитьюд");
                    g.sceneState.confirm("npc",f.optString("id"),NAMES[i],"INFORMED");
                    // Reconciliation restores neutral standing, as for city merchants.
                    // Status alone can leave the NPC at hostile-level relation.
                    JSONObject reconciled=fact(g,i);
                    if(reconciled!=null&&reconciled.optInt("relation",50)<50)
                        g.sceneState.fixNpc(f.optString("id"),50,false);
                }catch(org.json.JSONException ignored){return true;}
                g.stamina-=10;g.hour++;g.syncLivingSystems();
                g.message=NAMES[i]+": Конфликт улажен. Мы можем снова разговаривать.";
                g.progress.recent=g.message;g.recordVisualEvent(g.message);g.progress.sync(g);return true;
            }
            if(f!=null&&"HOSTILE".equals(f.optString("status"))&&!history)return true;
            if(trust&&(f==null||!"MET".equals(f.optString("status"))))return true;
            if(advice&&(f==null||f.optInt("relation",50)<60))return true;
            try{
                g.sceneState.setContext(g.day,g.hour,"Солитьюд");
                if(f==null)g.sceneState.confirm("npc","solitude_"+IDS[i],NAMES[i],"MET");
                else if(trust)g.sceneState.confirm("npc",f.optString("id"),NAMES[i],"INFORMED");
                else if(!memory&&!history)g.sceneState.revisitNpc(f.optString("id"));
            }catch(org.json.JSONException ignored){return true;}
            g.message=NAMES[i]+": "+(history?relationshipHistory(f):memory?"Отношение: "+f.optInt("relation",50)+"/100 | Последняя встреча: день "+f.optInt("lastDay")+SolitudeFavors.memory(g,i):(f==null?"":"Снова встретились. ")+(trust?"Спасибо за рассказ. Теперь мы немного лучше знакомы.":advice?ADVICE[i]:topic?TOPICS[i]:LINES[i]));
            g.progress.recent=g.message;g.recordVisualEvent(g.message);g.progress.sync(g);return true;
        }
        return true;
    }
}
