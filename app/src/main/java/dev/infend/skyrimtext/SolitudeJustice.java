package dev.infend.skyrimtext;

/** Guard desk and prison use the existing regional bounty ledger and sentence rules. */
final class SolitudeJustice {
    static final String MODE="SOLITUDE:jail", CELL="SOLITUDE:cell";
    private int total,remaining,lastTheftDay; private boolean attempted, detained, exercised;
    boolean detained(){return detained;}
    void theftChoice(GameEngine g,GameEngine.Scene s){if(lastTheftDay<g.day&&g.hour>=8&&g.hour<20&&g.stamina>=5)s.add("SOLITUDE:crime:steal","Попытаться украсть кольцо","Карманные кражи | 5 запаса сил | риск задержания");}
    boolean theft(GameEngine g,String action){
        if(!"SOLITUDE:crime:steal".equals(action))return false;
        if(!"SOLITUDE:0".equals(g.mode)||lastTheftDay>=g.day||g.hour<8||g.hour>=20||g.stamina<5)return true;
        lastTheftDay=g.day;g.stamina-=5;
        if(g.rollCheck(g.progress.chance(g,2,11,25))){
            if(g.progress.addItem("Похищенное кольцо",1)){g.progress.skillExperience(g,11,15);g.message="Ты незаметно прячешь кольцо. Никто не заметил кражу.";}else g.message="В сумке нет места. Ты оставляешь кольцо на прилавке.";
        }else{g.road.addCityBounty(3,25);detained=true;g.mode=MODE;g.message="Торговец замечает твою руку и зовёт стражу. Штраф за попытку кражи: 25 септимов.";} 
        g.progress.recent=g.message;g.progress.sync(g);g.recordVisualEvent(g.message);return true;
    }
    boolean confined(){return remaining>0&&total>=remaining&&total<=7;}
    void begin(GameEngine g){
        int fine=g.road.bounty(3);if(!SolitudeCity.here(g)||g.road.pending()||fine<=0||confined()||g.hp<=0)return;
        if(!MODE.equals(g.mode)&&!"WORLD".equals(g.mode)&&!"RESULT".equals(g.mode))return;
        int days=RoadEvents.jailDays(fine);if(g.day>Integer.MAX_VALUE-days)return;
        total=remaining=days;attempted=false;detained=false;exercised=false;
        int stolen=g.progress.quantity("Похищенное кольцо");if(stolen>0)g.progress.removeItem("Похищенное кольцо",stolen);
        g.mode=CELL;g.message="Стража запирает камеру. Срок: "+days+" дн."+(stolen>0?" Украденные кольца конфискованы.":"");g.progress.sync(g);g.recordVisualEvent(g.message);
    }
    GameEngine.Scene cell(GameEngine g){
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | ЗАКЛЮЧЕНИЕ","Камера Мрачного замка","Капли падают с каменного свода. За решёткой слышны шаги караула.\n\nОсталось: "+remaining+" из "+total+" дн. | Штраф: "+g.road.bounty(3)+" септимов.\n\n"+g.message);
        s.primary("SOLITUDE:cell:day","Отбыть один день","24 ч. | тюремный паёк и сон");
        if(!exercised&&g.stamina>=5)s.add("SOLITUDE:cell:exercise","Размяться в камере","5 запаса сил | один раз за день заключения");
        if(!attempted&&g.stamina>=10&&g.progress.quantity("Отмычка")>0)s.add("SOLITUDE:cell:escape","Попытаться сбежать","Взлом | 1 отмычка | 10 запаса сил | риск дополнительного штрафа");
        return s;
    }
    void cellAction(GameEngine g,String action){
        if(!confined()||!CELL.equals(g.mode)||!SolitudeCity.here(g))return;
        if("SOLITUDE:cell:day".equals(action)){
            if(!g.restPrisonDay())return;
            g.needs.eat();g.needs.drink();remaining--;attempted=false;exercised=false;
            if(remaining==0){g.road.finishCitySentence(g,3,total);total=0;g.mode=MODE;}
            else {g.message="Прошёл день. Стражник приносит воду и скудный паёк.";g.recordVisualEvent(g.message);}
        }else if("SOLITUDE:cell:exercise".equals(action)&&!exercised&&g.stamina>=5){
            exercised=true;g.stamina-=5;g.message="Ты разминаешь затёкшие мышцы в тесной камере. На сегодня достаточно.";g.recordVisualEvent(g.message);
        }else if("SOLITUDE:cell:escape".equals(action)&&!attempted&&g.stamina>=10&&g.progress.removeItem("Отмычка",1)){
            attempted=true;g.stamina-=10;boolean success=g.rollCheck(Math.min(85,20+g.progress.skillLevel(10)/2));
            g.road.addCityBounty(3,success?100:50);
            if(success){total=remaining=0;attempted=false;g.mode="WORLD";g.message="Ты выбираешься из подземелья. Штраф остался, за побег добавлено 100 септимов.";}
            else g.message="Стража замечает попытку побега. Отмычка потеряна, штраф увеличен на 50 септимов. Следующая попытка возможна после дня заключения.";
            g.recordVisualEvent(g.message);
        }else return;
        g.progress.recent=g.message;g.progress.sync(g);
    }
    org.json.JSONObject save(){org.json.JSONObject o=new org.json.JSONObject();try{o.put("total",total).put("remaining",remaining).put("attempted",attempted).put("lastTheftDay",lastTheftDay).put("detained",detained).put("exercised",exercised);}catch(Exception ignored){}return o;}
    static SolitudeJustice restore(org.json.JSONObject o){SolitudeJustice j=new SolitudeJustice();if(o!=null){j.total=Math.max(0,Math.min(7,o.optInt("total")));j.remaining=Math.max(0,Math.min(j.total,o.optInt("remaining")));j.attempted=j.remaining>0&&o.optBoolean("attempted");j.lastTheftDay=Math.max(0,o.optInt("lastTheftDay"));j.detained=j.remaining==0&&o.optBoolean("detained");j.exercised=j.remaining>0&&o.optBoolean("exercised");}return j;}
    static void choices(GameEngine g,GameEngine.Scene s){
        s.add(MODE,"Подземелье Мрачного замка","Стража | Штраф Хаафингара: "+g.road.bounty(3)+" септимов");
    }
    GameEngine.Scene scene(GameEngine g){
        int fine=g.road.bounty(3);
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | СТРАЖА","Подземелье Мрачного замка",
            "За решётками сыро и холодно. Дежурный стражник сверяет записи.\n\n"+(fine>0?"Твой штраф: "+fine+" септимов.":"За тобой не числится штрафов в Хаафингаре.")+"\n"+g.message);
        if(fine>0){
            if(g.gold>=fine)s.add("SOLITUDE:justice:pay","Оплатить штраф",fine+" септимов");
            s.add("SOLITUDE:justice:serve","Сдаться страже","Наказание: "+RoadEvents.jailDays(fine)+" дн. | Потеря прогресса навыков и конфискация украденных колец");
        }
        if(detained&&fine>0)return s;
        return s.add("SOLITUDE:back","На улицы Солитьюда","Покинуть подземелье").add("GO_WORLD","Завершить прогулку","");
    }
    boolean choose(GameEngine g,String action){
        if(!SolitudeCity.here(g)||g.road.pending()||g.hp<=0)return false;
        if(MODE.equals(action)&&"SOLITUDE:1".equals(g.mode)){g.mode=MODE;g.message="";return true;}
        if(!MODE.equals(g.mode))return false;
        if(!action.equals("SOLITUDE:justice:pay")&&!action.equals("SOLITUDE:justice:serve"))return false;
        if(g.road.bounty(3)<=0)return true;
        if(action.endsWith(":serve")){begin(g);return true;}
        g.mode="WORLD";g.road.payFine(g,"ROADFINE:3");if(g.road.bounty(3)==0){detained=false;int stolen=g.progress.quantity("Похищенное кольцо");if(stolen>0){g.progress.removeItem("Похищенное кольцо",stolen);g.progress.sync(g);g.message+=" Украденные кольца конфискованы.";}}g.mode=MODE;return true;
    }
}
