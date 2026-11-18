package dev.infend.skyrimtext;

/** Guard desk and prison use the existing regional bounty ledger and sentence rules. */
final class SolitudeJustice {
    static final String MODE="SOLITUDE:jail";
    static void choices(GameEngine g,GameEngine.Scene s){
        s.add(MODE,"Подземелье Мрачного замка","Стража | Штраф Хаафингара: "+g.road.bounty(3)+" септимов");
    }
    static GameEngine.Scene scene(GameEngine g){
        int fine=g.road.bounty(3);
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | СТРАЖА","Подземелье Мрачного замка",
            "За решётками сыро и холодно. Дежурный стражник сверяет записи.\n\n"+(fine>0?"Твой штраф: "+fine+" септимов.":"За тобой не числится штрафов в Хаафингаре.")+"\n"+g.message);
        if(fine>0){
            if(g.gold>=fine)s.add("SOLITUDE:justice:pay","Оплатить штраф",fine+" септимов");
            s.add("SOLITUDE:justice:serve","Сдаться страже","Наказание: "+RoadEvents.jailDays(fine)+" дн. | Потеря прогресса навыков и конфискация украденных колец");
        }
        return s.add("SOLITUDE:back","На улицы Солитьюда","Покинуть подземелье").add("GO_WORLD","Завершить прогулку","");
    }
    static boolean choose(GameEngine g,String action){
        if(!SolitudeCity.here(g)||g.road.pending()||g.hp<=0)return false;
        if(MODE.equals(action)&&"SOLITUDE:1".equals(g.mode)){g.mode=MODE;g.message="";return true;}
        if(!MODE.equals(g.mode))return false;
        if(!action.equals("SOLITUDE:justice:pay")&&!action.equals("SOLITUDE:justice:serve"))return false;
        if(g.road.bounty(3)<=0)return true;
        g.mode="WORLD";
        if(action.endsWith(":pay"))g.road.payFine(g,"ROADFINE:3");else g.road.serveSentence(g,"ROADJAIL:3");
        g.mode=MODE;return true;
    }
}
