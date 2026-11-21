package dev.infend.skyrimtext;

/** MS06Start: the original quest adapted to atlas travel and shared combat. */
final class SolitudeWolf {
    static final String KEY="solitude_wolf", GUARD=KEY+"_guard", RITUAL=KEY+"_ritual";
    static final String TITLE="Мужчина, который кричал «Волки»", CAVE="Волчий Череп", KIND="SOLITUDE_WOLF:";
    static String state(GameEngine g){return g.progress.questOutcome(KEY);}
    static boolean done(GameEngine g,String key){return "COMPLETED".equals(g.progress.questOutcome(key));}
    static boolean active(GameEngine g){return "ACTIVE".equals(state(g));}
    static boolean cleared(GameEngine g){return done(g,RITUAL);}
    static boolean atCave(GameEngine g){return g.started&&g.region==3&&CAVE.equals(g.progress.location);}
    static boolean palace(GameEngine g){
        String status=g.sceneState.npcStatusByName("Фолк Огнебород");
        return SolitudeCity.here(g)&&"SOLITUDE:2".equals(g.mode)&&g.hour>=9&&g.hour<17&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    static int reward(GameEngine g){return g.level>=40?1500:g.level>=30?1200:g.level>=20?1000:g.level>=10?800:500;}
    static String objective(GameEngine g){
        if(done(g,KEY))return "Ритуал прерван; Фолк получил отчёт.";
        if(cleared(g))return "Вернуться к Фолку Огнебороду в Синий дворец.";
        return done(g,GUARD)?"В руинах Волчьего Черепа остановить главу некромантов.":"Добраться до пещеры Волчий Череп и победить её стража.";
    }
    static void palaceChoices(GameEngine g,GameEngine.Scene s){
        if(!palace(g))return;
        if(state(g)==null||state(g).isEmpty())s.add("SOLITUDE:wolf:accept","Выслушать Варния Юния и Фолка","Каноническое задание | Волчий Череп");
        if(active(g)&&cleared(g)&&g.gold<=Integer.MAX_VALUE-reward(g))s.add("SOLITUDE:wolf:finish","Доложить о прерванном ритуале",reward(g)+" септимов");
    }
    static void worldChoices(GameEngine g,GameEngine.Scene s){
        if(!active(g)||cleared(g)||!atCave(g)||g.road.pending()||g.hp<=0)return;
        s.primary(done(g,GUARD)?"WOLF:ritual":"WOLF:guard",done(g,GUARD)?"Спуститься в руины и прервать ритуал":"Войти в пещеру Волчий Череп",objective(g));
    }
    static boolean choosePalace(GameEngine g,String a){
        if(!palace(g))return true;
        if("SOLITUDE:wolf:accept".equals(a)&&(state(g)==null||state(g).isEmpty())){
            g.progress.setQuestOutcome(KEY,"ACTIVE");
            report(g,"Варний Юний просит двор помочь Драконьему мосту: из пещеры Волчий Череп доносятся странные звуки. Фолк поручает тебе проверить пещеру и остановить возможную угрозу.");
        }else if("SOLITUDE:wolf:finish".equals(a)&&active(g)&&cleared(g)){
            int reward=reward(g);if(g.gold>Integer.MAX_VALUE-reward)return true;
            g.progress.setQuestOutcome(KEY,"COMPLETED");g.gold+=reward;g.holdReputation[3]=Math.min(100,g.holdReputation[3]+2);
            report(g,"Фолк благодарит тебя за то, что некроманты не смогли подчинить Потему. Награда: "+reward+" септимов.");
        }
        return true;
    }
    static void chooseWorld(GameEngine g,String a){
        if(!"WORLD".equals(g.mode)||!atCave(g)||!active(g)||cleared(g)||g.road.pending()||g.solitude.custody()||g.hp<=0)return;
        if("WOLF:guard".equals(a)&&!done(g,GUARD))
            g.beginSolitudeWolfBattle("guard","Драугр Волчьего Черепа",65+g.level*5,7+g.level/2,"Из тёмного прохода поднимается драугр, преграждая путь к руинам.");
        else if("WOLF:ritual".equals(a)&&done(g,GUARD))
            g.beginSolitudeWolfBattle("ritual","Глава некромантов",95+g.level*7,9+g.level/2,"На башне некроманты пытаются связать дух Потемы. Нужно прервать обряд, прежде чем он завершится.");
    }
    static void victory(GameEngine g){
        if(!active(g)||!atCave(g)||g.enemyHp!=0)return;
        if((KIND+"guard").equals(g.battleKind)&&!done(g,GUARD)){
            g.progress.setQuestOutcome(GUARD,"COMPLETED");g.message+="\n\nПуть в руины открыт. На башне продолжается ритуал.";
        }else if((KIND+"ritual").equals(g.battleKind)&&done(g,GUARD)&&!cleared(g)){
            g.progress.setQuestOutcome(RITUAL,"COMPLETED");g.message+="\n\nРитуал сорван. Дух Потемы исчезает, но не уничтожен. Вернись к Фолку.";
        }
        g.progress.recent=g.message;g.progress.sync(g);
    }
    private static void report(GameEngine g,String m){g.message=m;g.progress.recent=m;g.recordVisualEvent(m);g.progress.sync(g);}
}
