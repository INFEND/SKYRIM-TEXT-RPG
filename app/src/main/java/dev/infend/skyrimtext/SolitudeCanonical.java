package dev.infend.skyrimtext;

/** Offline adaptation of Angeline's original quest, using the shared quest ledger. */
final class SolitudeCanonical {
    static final String KEY="solitude_angeline_news", STEP=KEY+"_aldis";
    static final String TITLE="Сведения о дочери Анжелины";
    static final String RUM="solitude_sorex_rum", RUM_TITLE="Доставка (Сорекс)";
    static final String ATTIRE="solitude_taarie_attire", ATTIRE_STEP=ATTIRE+"_elisif";
    static final String ATTIRE_TITLE="Одежда, достойная ярла", OUTFIT="Наряд из «Сияющих одежд»";
    static String attireState(GameEngine g){return g.progress.questOutcome(ATTIRE);}
    static boolean shown(GameEngine g){return "COMPLETED".equals(g.progress.questOutcome(ATTIRE_STEP));}
    static String attireObjective(GameEngine g){
        if("COMPLETED".equals(attireState(g)))return "Таари получила известие о заказе из дворца.";
        return shown(g)?"Вернуться к Таари в «Сияющие одежды».":"Надеть наряд из «Сияющих одежд» и показать его Элисиф в Синем дворце.";
    }
    static void attireChoices(GameEngine g,GameEngine.Scene s){
        if(!SolitudeShops.open(g,0))return;
        String state=attireState(g);
        if(state==null||state.isEmpty())s.add("SOLITUDE:attire:accept","Обсудить одежду для двора","Каноническое поручение | наряд Таари");
        else if("ACTIVE".equals(state)&&shown(g)&&g.gold<=Integer.MAX_VALUE-rumReward(g))s.add("SOLITUDE:attire:finish","Передать отзыв Элисиф",rumReward(g)+" септимов | наряд остаётся у тебя");
    }
    private static boolean chooseAttire(GameEngine g,String a){
        String state=attireState(g);
        if((SolitudeShops.MODE+0).equals(g.mode)&&SolitudeShops.open(g,0)){
            if("SOLITUDE:attire:accept".equals(a)&&(state==null||state.isEmpty())){
                if(g.progress.quantity(OUTFIT)==0&&!g.progress.addItem(OUTFIT,1)){report(g,"Освободи место для наряда Таари.");return true;}
                g.progress.setQuestOutcome(ATTIRE,"ACTIVE");
                report(g,"Таари просит показать её наряд ярлу Элисиф Прекрасной и рассказать о лавке. Надень полученную одежду перед визитом в Синий дворец.");
            }else if("SOLITUDE:attire:finish".equals(a)&&"ACTIVE".equals(state)&&shown(g)){
                int reward=rumReward(g);if(g.gold>Integer.MAX_VALUE-reward)return true;
                g.progress.setQuestOutcome(ATTIRE,"COMPLETED");g.gold+=reward;g.holdReputation[3]=Math.min(100,g.holdReputation[3]+1);
                report(g,"Таари рада известию о заказе из дворца. Наряд остаётся у тебя. Награда: "+reward+" септимов. Репутация Хаафингара +1 (адаптация).");
            }
        }else if("SOLITUDE:2".equals(g.mode)&&available(g,"Элисиф Прекрасная",9,17)&&"SOLITUDE:attire:show".equals(a)&&"ACTIVE".equals(state)&&!shown(g)){
            if(!g.progress.equipped(OUTFIT)){report(g,"Сначала надень наряд Таари.");return true;}
            g.progress.setQuestOutcome(ATTIRE_STEP,"COMPLETED");
            report(g,"Элисиф одобряет наряд и обещает заказать новые платья в «Сияющих одеждах». Вернись к Таари с её отзывом.");
        }
        return true;
    }
    static String rumState(GameEngine g){return g.progress.questOutcome(RUM);}
    static String rumObjective(GameEngine g){return "COMPLETED".equals(rumState(g))?"Ром доставлен Фолку Огнебороду.":"Доставить ром из Строс-М’Кая Фолку Огнебороду в Синий дворец.";}
    private static boolean available(GameEngine g,String name,int start,int end){
        String status=g.sceneState.npcStatusByName(name);
        return g.hour>=start&&g.hour<end&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    static int rumReward(GameEngine g){return g.level>=40?750:g.level>=30?600:g.level>=20?500:g.level>=10?400:250;}
    static void districtChoices(GameEngine g,GameEngine.Scene s,int district){
        if(district==2&&"ACTIVE".equals(attireState(g))&&!shown(g)&&available(g,"Элисиф Прекрасная",9,17))
            s.add("SOLITUDE:attire:show","Показать наряд Элисиф",g.progress.equipped(OUTFIT)?"Рассказать о лавке":"Сначала надень наряд в инвентаре");
        String state=rumState(g);
        if(district==6&&(state==null||state.isEmpty())&&available(g,"Сорекс Виний",6,23))
            s.add("SOLITUDE:rum:accept","Поговорить с Сорексом Винием","Каноническое поручение | доставка рома");
        if(district==2&&"ACTIVE".equals(state)&&available(g,"Фолк Огнебород",9,17)&&g.gold<=Integer.MAX_VALUE-rumReward(g))
            s.add("SOLITUDE:rum:deliver","Передать ром Фолку",rumReward(g)+" септимов | +1 репутации Хаафингара");
    }
    private static boolean chooseRum(GameEngine g,String a){
        String state=rumState(g);
        if("SOLITUDE:6".equals(g.mode)&&available(g,"Сорекс Виний",6,23)&&"SOLITUDE:rum:accept".equals(a)&&(state==null||state.isEmpty())){
            g.progress.setQuestOutcome(RUM,"ACTIVE");
            report(g,"Сорекс Виний просит отнести Фолку Огнебороду бутылку рома из Строс-М’Кая. Посылка отмечена в журнале как квестовый груз; её нельзя выпить, продать или потерять.");
        }else if("SOLITUDE:2".equals(g.mode)&&available(g,"Фолк Огнебород",9,17)&&"SOLITUDE:rum:deliver".equals(a)&&"ACTIVE".equals(state)){
            int reward=rumReward(g);if(g.gold>Integer.MAX_VALUE-reward)return true;
            g.progress.setQuestOutcome(RUM,"COMPLETED");
            g.gold+=reward;g.holdReputation[3]=Math.min(100,g.holdReputation[3]+1);
            report(g,"Фолк принимает бутылку рома и благодарит за доставку. Награда: "+reward+" септимов. Репутация Хаафингара +1 (адаптация).");
        }
        return true;
    }
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
        if(!a.startsWith("SOLITUDE:angeline:")&&!a.startsWith("SOLITUDE:rum:")&&!a.startsWith("SOLITUDE:attire:"))return false;
        if(!SolitudeCity.here(g)||g.solitude.custody()||g.road.pending()||g.hp<=0)return true;
        if(a.startsWith("SOLITUDE:attire:"))return chooseAttire(g,a);
        if(a.startsWith("SOLITUDE:rum:"))return chooseRum(g,a);
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
