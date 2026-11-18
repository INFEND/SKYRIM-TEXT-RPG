package dev.infend.skyrimtext;

/** Offline adaptation of Angeline's original quest, using the shared quest ledger. */
final class SolitudeCanonical {
    static final String KEY="solitude_angeline_news", STEP=KEY+"_aldis";
    static final String TITLE="Сведения о дочери Анжелины";
    static String state(GameEngine g){return g.progress.questOutcome(KEY);}
    static boolean informed(GameEngine g){return "COMPLETED".equals(g.progress.questOutcome(STEP));}
    static String objective(GameEngine g){
        if("COMPLETED".equals(state(g)))return "Анжелина узнала о судьбе дочери.";
        return informed(g)?"Вернуться к Анжелине Моррар":"Расспросить капитана Алдиса в Мрачном замке";
    }
    private static boolean aldis(GameEngine g){
        String status=g.sceneState.npcStatusByName("Капитан Алдис");
        return g.hour>=8&&g.hour<18&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    static void shopChoices(GameEngine g,GameEngine.Scene s){
        if(!SolitudeShops.open(g,1))return;
        if(state(g)==null||state(g).isEmpty())s.add("SOLITUDE:angeline:accept","Спросить о дочери","Каноническое поручение | адаптация");
        else if("ACTIVE".equals(state(g))&&informed(g))s.add("SOLITUDE:angeline:finish","Рассказать о Фуре","Сообщить печальную весть");
    }
    static void castleChoices(GameEngine g,GameEngine.Scene s){
        if(aldis(g)&&"ACTIVE".equals(state(g))&&!informed(g))s.add("SOLITUDE:angeline:aldis","Расспросить капитана Алдиса","Судьба Фуры");
    }
    static boolean choose(GameEngine g,String a){
        if(!a.startsWith("SOLITUDE:angeline:"))return false;
        if(!SolitudeCity.here(g)||g.solitude.custody()||g.road.pending()||g.hp<=0)return true;
        String state=state(g);
        if((SolitudeShops.MODE+1).equals(g.mode)&&SolitudeShops.open(g,1)){
            if(a.equals("SOLITUDE:angeline:accept")&&(state==null||state.isEmpty())){
                g.progress.setQuestOutcome(KEY,"ACTIVE");
                report(g,"Анжелина рассказывает, что её дочь Фура служит в Имперском легионе. От неё давно нет вестей. Анжелина просит поговорить с капитаном Алдисом.");
            }else if(a.equals("SOLITUDE:angeline:finish")&&"ACTIVE".equals(state)&&informed(g)){
                g.progress.setQuestOutcome(KEY,"COMPLETED");
                g.holdReputation[3]=Math.min(100,g.holdReputation[3]+1);
                report(g,"Анжелина молча выслушивает известие о гибели Фуры. Ей тяжело, но теперь она знает правду. Репутация Хаафингара +1 (адаптация).");
            }
        }else if("SOLITUDE:1".equals(g.mode)&&aldis(g)&&"ACTIVE".equals(state)&&!informed(g)&&a.equals("SOLITUDE:angeline:aldis")){
            g.progress.setQuestOutcome(STEP,"COMPLETED");
            report(g,"Капитан Алдис после расспросов признаётся: Фура погибла в бою. Он просит передать Анжелине соболезнования.");
        }
        return true;
    }
    private static void report(GameEngine g,String m){g.message=m;g.progress.recent=m;g.recordVisualEvent(m);g.progress.sync(g);}
}
