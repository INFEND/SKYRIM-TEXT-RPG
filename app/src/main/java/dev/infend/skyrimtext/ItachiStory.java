package dev.infend.skyrimtext;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Locale;

/** Optional crossover arc; all choices and combat effects are engine-owned. */
final class ItachiStory {
    static final String MODE="ITACHI", TITLE="Алая Ворона", NAME="Итачи";
    static final int FINAL=6, MAX_ENERGY=100;
    boolean enabled=true, following=false, romance=false;
    int stage=0, trust=0, respect=0, affection=0, energy=MAX_ENERGY, victories=0, lastTalk=-1, lastDate=-1;
    String ending="", memory="", lastPower="";
    private static final String[] TITLES={"Чёрное перо","Глаза незнакомки","Отголоски иного мира","Цена силы","Наследие воронов","Выбор собственной судьбы"};
    private static final String[] BODY={
        "На дороге у Ривервуда ворон слетает с ветки. Под деревом стоит женщина в чёрном одеянии с красными облаками. Она замечает тебя раньше, чем ты успеваешь заговорить.\n\nИтачи:\n— Не бойся. Я не ищу здесь врагов.",
        "У костра Итачи на мгновение открывает алые глаза. Она объясняет, что видит движения и распознаёт обман, но не читает любую мысль по желанию.\n\nИтачи:\n— Сила не делает человека правым.",
        "Итачи рассказывает о мире, откуда пришла. В её воспоминаниях есть война, долг и решения, которые невозможно отменить. На твою карту она наносит знак неизвестной печати.\n\nИтачи:\n— Мне нужно узнать, почему я оказалась здесь.",
        "Во время упражнения её глаза тускнеют. Высшие техники требуют энергии и оставляют боль. Итачи прекращает тренировку по собственной воле.\n\nИтачи:\n— Я умею побеждать. Но не всегда умею останавливаться.",
        "В записях Коллегии обнаруживается описание нестабильной границы миров. Печать реагирует на духовную энергию Итачи; открывать её без подготовки опасно.\n\nИтачи:\n— Не стоит жертвовать этим миром ради возвращения в мой.",
        "Итачи закрывает печать, сохранив записи о её природе. Теперь она сама решает, оставаться ли странницей или принять новый дом.\n\nИтачи:\n— Я не хочу, чтобы прошлое выбирало за меня."
    };
    private static int clamp(int x){return Math.max(0,Math.min(100,x));}
    boolean active(){return enabled&&following&&stage==FINAL;}
    String location(GameEngine g){if(active())return g.progress.location;return stage==4?"Винтерхолд":"Ривервуд";}
    boolean present(GameEngine g){return enabled&&(active()||location(g).equalsIgnoreCase(g.progress.location)||g.progress.location.toLowerCase(Locale.ROOT).contains(location(g).toLowerCase(Locale.ROOT)));}
    boolean available(GameEngine g){return enabled&&g.started&&!g.road.pending()&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&present(g);}
    boolean setEnabled(GameEngine g,boolean on){if("COMBAT".equals(g.mode))return false;enabled=on;if(!on&&MODE.equals(g.mode))g.mode="WORLD";return true;}
    String relation(){return romance?"Возлюбленная":active()?"Спутница":trust>=60?"Доверяет":trust>=25?"Знакомая":"Незнакомка";}
    String objective(GameEngine g){return TITLE+" | "+(stage<FINAL?TITLES[stage]+" | "+location(g):ending+" | "+relation());}
    String aiContext(GameEngine g){if(!enabled)return "Итачи: дополнение отключено, не вводить её в сцены.";if(stage==0)return "Итачи ещё не знакома с героем. Не вводить её вне встречи движка.";return "Итачи: "+ItachiProfile.CHARACTER+" Этап "+stage+"/"+FINAL+", место="+location(g)+", рядом="+active()+", доверие="+trust+", роман="+romance+", энергия="+energy+". ИИ не меняет флаги и не придумывает применение способностей.";}
    GameEngine.Scene scene(GameEngine g){
        GameEngine.Scene s;
        if(stage<FINAL){
            s=new GameEngine.Scene(TITLE,TITLES[stage],BODY[stage]);
            if(stage==FINAL-1)s.primary("ITACHI:stay","Предложить ей остаться","Новый дом по её собственному выбору").add("ITACHI:free","Поддержать её свободу","Она останется независимой странницей");
            else s.primary("ITACHI:help","Выслушать и помочь","Доверие и уважение").add("ITACHI:caution","Сохранить дистанцию","Уважать её границы");
        }else{
            s=new GameEngine.Scene(TITLE,relation(),"Итачи спокойно смотрит на тебя.\n\nИтачи:\n— "+(romance?"Рядом с тобой я могу позволить себе тишину.":"У каждого из нас свой путь. Иногда они совпадают.")+"\n\nДоверие "+trust+"/100 | Уважение "+respect+"/100 | Симпатия "+affection+"/100\nЭнергия "+energy+"/100\n"+memory);
            if(!following&&trust>=35)s.add("ITACHI:join","Пригласить в отряд","Иллюзии, огонь и защита");
            if(following)s.add("ITACHI:leave","Отпустить спутницу","Вернётся в Ривервуд");
            if(lastTalk!=g.day)s.add("ITACHI:talk","Поговорить у костра","Доверие и симпатия").add("ITACHI:train","Тренироваться вместе","Уважение и восстановление энергии");
            if(!romance&&trust>=75&&respect>=65&&affection>=55)s.add("ITACHI:romance","Признаться в чувствах","Взаимное согласие");
            if(romance){if(lastDate!=g.day)s.add("ITACHI:date","Провести вечер вдвоём","Разговор о будущем");s.add("ITACHI:break","Прекратить роман","Без принуждения");}
        }
        return s.add("ITACHI:exit","Продолжить путь","Вернуться к приключению");
    }
    void choose(GameEngine g,String action){
        if("ITACHI:open".equals(action)){if(available(g))g.mode=MODE;return;}
        if(!MODE.equals(g.mode)||!present(g))return;
        if("ITACHI:exit".equals(action)){g.mode="WORLD";return;}
        if(stage<FINAL-1){
            if(!"ITACHI:help".equals(action)&&!"ITACHI:caution".equals(action))return;
            boolean help="ITACHI:help".equals(action);trust=clamp(trust+(help?12:4));respect=clamp(respect+9);affection=clamp(affection+(help?7:2));stage++;
            if(!present(g)){g.mode="WORLD";g.message="Следующая встреча: "+location(g);}
            return;
        }
        if(stage==FINAL-1){
            if(!"ITACHI:stay".equals(action)&&!"ITACHI:free".equals(action))return;
            stage=FINAL;ending="ITACHI:stay".equals(action)?"Новый дом":"Свободная дорога";trust=clamp(trust+12);respect=clamp(respect+10);affection=clamp(affection+8);g.addXp(100);return;
        }
        switch(action){
            case "ITACHI:join":if(!following&&trust>=35){following=true;memory="Итачи согласилась путешествовать с тобой.";}break;
            case "ITACHI:leave":if(following){following=false;g.mode="WORLD";}break;
            case "ITACHI:talk":if(lastTalk!=g.day){lastTalk=g.day;trust=clamp(trust+6);respect=clamp(respect+4);affection=clamp(affection+7);memory="Итачи рассказала о том, что ценит свободу выбора.";}break;
            case "ITACHI:train":if(lastTalk!=g.day){lastTalk=g.day;respect=clamp(respect+7);trust=clamp(trust+4);energy=clamp(energy+20);memory="Вы разобрали тактику и отработали защиту.";}break;
            case "ITACHI:romance":if(!romance&&trust>=75&&respect>=65&&affection>=55){romance=true;memory="Итачи ответила взаимностью. Ваш союз основан на свободном выборе.";}break;
            case "ITACHI:date":if(romance&&lastDate!=g.day){lastDate=g.day;trust=clamp(trust+3);affection=clamp(affection+5);memory="Вы провели тихий вечер вместе, обсуждая планы.";}break;
            case "ITACHI:break":if(romance){romance=false;memory="Вы сохранили уважение и завершили роман.";}break;
        }
    }
    void rest(int hours){if(active())energy=clamp(energy+Math.max(0,Math.min(24,hours))*6);}
    int assist(int turn,int remaining){lastPower="";if(!active()||remaining<=0||turn%2!=0)return 0;
        int cost=4,damage=7+Math.min(8,victories/4);lastPower="Кунаи";
        if(energy>=35&&turn%8==0){cost=35;damage+=17;lastPower="Сусаноо / Клинок Тоцука";}
        else if(energy>=24&&turn%6==0){cost=24;damage+=12;lastPower="Аматэрасу";}
        else if(energy>=12&&turn%4==0){cost=12;damage+=5;lastPower="Гендзюцу / Цукуёми";}
        else if(energy>=4){lastPower="Шаринган / точный удар";}
        else cost=0;
        energy-=cost;return Math.min(remaining,damage);
    }
    int protect(int incoming){if(!active()||incoming<=1||energy<5)return 0;int n=Math.min(incoming-1,4+Math.min(5,victories/5));energy-=5;return n;}
    void victory(){if(active())victories=Math.min(1000,victories+1);}
    JSONObject save() throws JSONException{return new JSONObject().put("enabled",enabled).put("following",following).put("romance",romance).put("stage",stage).put("trust",trust).put("respect",respect).put("affection",affection).put("energy",energy).put("victories",victories).put("lastTalk",lastTalk).put("lastDate",lastDate).put("ending",ending).put("memory",memory);}
    static ItachiStory restore(JSONObject j){ItachiStory s=new ItachiStory();if(j==null)return s;s.enabled=j.optBoolean("enabled",true);s.stage=Math.max(0,Math.min(FINAL,j.optInt("stage")));s.trust=clamp(j.optInt("trust"));s.respect=clamp(j.optInt("respect"));s.affection=clamp(j.optInt("affection"));s.energy=clamp(j.optInt("energy",100));s.victories=Math.max(0,Math.min(1000,j.optInt("victories")));s.lastTalk=j.optInt("lastTalk",-1);s.lastDate=j.optInt("lastDate",-1);s.ending=j.optString("ending");s.memory=j.optString("memory");s.following=s.stage==FINAL&&j.optBoolean("following");s.romance=s.stage==FINAL&&j.optBoolean("romance");return s;}
}
