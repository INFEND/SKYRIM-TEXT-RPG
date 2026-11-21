package dev.infend.skyrimtext;

/** Offline adaptation of Angeline's original quest, using the shared quest ledger. */
final class SolitudeCanonical {
    static final String GRACE="solitude_greta_grace", GRACE_TALK=GRACE+"_greta", GRACE_LOOT=GRACE+"_amulet";
    static final String GRACE_TITLE="Вернуть милость богов", GRACE_ITEM="Амулет Талоса Роггвира";
    static final String ADDVAR="SOLITUDE:addvar", CATACOMBS="SOLITUDE:catacombs";
    static String graceState(GameEngine g){return g.progress.questOutcome(GRACE);}
    static boolean graceTalked(GameEngine g){return "COMPLETED".equals(g.progress.questOutcome(GRACE_TALK));}
    static boolean graceLooted(GameEngine g){return "COMPLETED".equals(g.progress.questOutcome(GRACE_LOOT));}
    static boolean graceMode(String mode){return ADDVAR.equals(mode)||CATACOMBS.equals(mode);}
    static String graceObjective(GameEngine g){
        if("COMPLETED".equals(graceState(g)))return "Амулет возвращён Грете; она готова снова посещать храм.";
        if(!graceTalked(g))return "Поговорить с Гретой в доме Аддвара.";
        return graceLooted(g)?"Вернуть амулет Роггвира Грете.":"Найти амулет Роггвира в катакомбах Солитьюда.";
    }
    static GameEngine.Scene graceScene(GameEngine g){
        boolean home=ADDVAR.equals(g.mode),active="ACTIVE".equals(graceState(g));
        GameEngine.Scene s=new GameEngine.Scene("СОЛИТЬЮД | "+g.hour+":00",home?"Дом Аддвара":"Катакомбы Солитьюда",
            (home?"В доме торговца рыбой тихо. Семья ещё скорбит о Роггвире.":"В холодных нишах покоятся умершие жители столицы. Среди каменных гробов находится могила Роггвира.")+"\n\n"+g.message);
        if(home&&active&&available(g,"Грета",8,20)){
            if(!graceTalked(g))s.add("SOLITUDE:grace:talk","Поговорить с Гретой","Почему она оставила храм?");
            else if(graceLooted(g)&&g.progress.quantity(GRACE_ITEM)>0&&g.gold<=Integer.MAX_VALUE-rumReward(g))
                s.add("SOLITUDE:grace:finish","Вернуть амулет Роггвира",rumReward(g)+" септимов");
        }
        if(!home)SolitudeCourt.styrrChoices(g,s);
        if(!home&&active&&graceTalked(g)&&!graceLooted(g))s.add("SOLITUDE:grace:loot","Найти амулет в гробу Роггвира","Квестовый предмет");
        return s.add("SOLITUDE:back","На улицы Солитьюда","Вернуться к выбору районов").add("GO_WORLD","Завершить прогулку","Вернуться к приключению");
    }
    private static boolean chooseGrace(GameEngine g,String a){
        String state=graceState(g);
        if("SOLITUDE:0".equals(g.mode)&&available(g,"Сварри",9,20)&&"SOLITUDE:grace:accept".equals(a)&&(state==null||state.isEmpty())){
            g.progress.setQuestOutcome(GRACE,"ACTIVE");
            report(g,"Сварри рассказывает, что после казни дяди Роггвира её мать Грета перестала ходить в Храм Богов. Девочка просит поговорить с матерью в доме Аддвара.");
        }else if(ADDVAR.equals(g.mode)&&available(g,"Грета",8,20)&&"ACTIVE".equals(state)){
            if("SOLITUDE:grace:talk".equals(a)&&!graceTalked(g)){
                g.progress.setQuestOutcome(GRACE_TALK,"COMPLETED");
                report(g,"Грета хочет вернуться в храм, но ей не хватает связи с Талосом. Она просит найти амулет брата в катакомбах Солитьюда.");
            }else if("SOLITUDE:grace:finish".equals(a)&&graceTalked(g)&&graceLooted(g)&&g.progress.quantity(GRACE_ITEM)>0){
                int reward=rumReward(g);if(g.gold>Integer.MAX_VALUE-reward)return true;
                g.progress.setQuestOutcome(GRACE,"COMPLETED");g.progress.quantity(GRACE_ITEM,0);
                g.gold+=reward;g.holdReputation[3]=Math.min(100,g.holdReputation[3]+1);
                report(g,"Грета бережно принимает амулет брата и обещает вновь посещать храм. Награда: "+reward+" септимов.");
            }
        }else if(CATACOMBS.equals(g.mode)&&"ACTIVE".equals(state)&&graceTalked(g)&&!graceLooted(g)&&"SOLITUDE:grace:loot".equals(a)){
            if(g.progress.quantity(GRACE_ITEM)==0&&!g.progress.addItem(GRACE_ITEM,1))return true;
            g.progress.setQuestOutcome(GRACE_LOOT,"COMPLETED");report(g,"В гробу Роггвира ты находишь его амулет Талоса. Верни его Грете.");
        }
        return true;
    }
    static final String KEY="solitude_angeline_news", STEP=KEY+"_aldis";
    static final String TITLE="Сведения о дочери Анжелины";
    static final String RUM="solitude_sorex_rum", RUM_TITLE="Доставка (Сорекс)";
    static final String WINE="solitude_evete_wine", WINE_STEP=WINE+"_release", WINE_TITLE="Пряное вино", WINE_ITEM="Пряное вино";
    static String wineState(GameEngine g){return g.progress.questOutcome(WINE);}
    static boolean wineReleased(GameEngine g){return "COMPLETED".equals(g.progress.questOutcome(WINE_STEP));}
    static String wineObjective(GameEngine g){
        if("COMPLETED".equals(wineState(g)))return "Партия специй освобождена, Иветта получила известие.";
        return wineReleased(g)?"Вернуться к Иветте Сан на рынок.":"Договориться с Витторией Вичи в гавани о выдаче партии специй.";
    }
    private static boolean vittoriaAlive(GameEngine g){
        String status=g.sceneState.npcStatusByName("Виттория Вичи");
        return !"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    static int wineBribe(GameEngine g){return Math.max(200,Math.min(600,200+5*g.level));}
    static boolean winePersuade(GameEngine g){return g.progress.skillLevel(12)>=50;}
    private static void wineChoices(GameEngine g,GameEngine.Scene s,int district){
        String state=wineState(g);
        if(district==0&&available(g,"Иветта Сан",8,18)){
            if((state==null||state.isEmpty())&&vittoriaAlive(g))s.add("SOLITUDE:wine:accept","Расспросить Иветту Сан о поставке","Каноническое поручение | Пряное вино");
            if("ACTIVE".equals(state)&&wineReleased(g))s.add("SOLITUDE:wine:finish","Сообщить об освобождении груза","2 бутылки пряного вина");
        }
        if(district==7&&"ACTIVE".equals(state)&&!wineReleased(g)&&available(g,"Виттория Вичи",8,18)){
            if(winePersuade(g))s.add("SOLITUDE:wine:persuade","Убедить Витторию снять арест","Красноречие 50 | без оплаты");
            if(g.gold>=wineBribe(g))s.add("SOLITUDE:wine:bribe","Дать взятку",wineBribe(g)+" септимов");
            if(g.gold>=2000)s.add("SOLITUDE:wine:tariff","Оплатить пошлину","2000 септимов");
        }
    }
    private static boolean chooseWine(GameEngine g,String a){
        String state=wineState(g);
        if("SOLITUDE:0".equals(g.mode)&&available(g,"Иветта Сан",8,18)){
            if("SOLITUDE:wine:accept".equals(a)&&(state==null||state.isEmpty())&&vittoriaAlive(g)){
                g.progress.setQuestOutcome(WINE,"ACTIVE");report(g,"Иветта Сан объясняет: Восточная имперская компания задержала её специи. В гавани Виттория Вичи требует уплатить пошлину в 2000 септимов.");
            }else if("SOLITUDE:wine:finish".equals(a)&&"ACTIVE".equals(state)&&wineReleased(g)){
                if(!g.progress.addItem(WINE_ITEM,2)){report(g,"Освободи место для двух бутылок пряного вина.");return true;}
                g.progress.setQuestOutcome(WINE,"COMPLETED");g.holdReputation[3]=Math.min(100,g.holdReputation[3]+1);
                report(g,"Иветта радуется освобождению груза и дарит две бутылки пряного вина. Репутация Хаафингара +1 (адаптация).");
            }
        }else if("SOLITUDE:7".equals(g.mode)&&"ACTIVE".equals(state)&&!wineReleased(g)&&available(g,"Виттория Вичи",8,18)){
            int cost;
            if("SOLITUDE:wine:persuade".equals(a)&&winePersuade(g))cost=0;
            else if("SOLITUDE:wine:bribe".equals(a))cost=wineBribe(g);
            else if("SOLITUDE:wine:tariff".equals(a))cost=2000;
            else return true;
            if(g.gold<cost)return true;
            g.gold-=cost;g.progress.setQuestOutcome(WINE_STEP,"COMPLETED");
            report(g,"Виттория распоряжается освободить партию специй."+ (cost>0?" Потрачено "+cost+" септимов.":" Удалось договориться без оплаты.")+" Вернись к Иветте.");
        }
        return true;
    }
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
        SolitudeCourt.districtChoices(g,s,district);
        if(district==2)SolitudeWolf.palaceChoices(g,s);
        wineChoices(g,s,district);
        if(district==0&&available(g,"Сварри",9,20)&&(graceState(g)==null||graceState(g).isEmpty()))s.add("SOLITUDE:grace:accept","Поговорить со Сварри","Каноническое поручение | Вернуть милость богов");
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
        if(!a.startsWith("SOLITUDE:angeline:")&&!a.startsWith("SOLITUDE:rum:")&&!a.startsWith("SOLITUDE:attire:")&&!a.startsWith("SOLITUDE:wine:")&&!a.startsWith("SOLITUDE:grace:")&&!a.startsWith("SOLITUDE:wolf:")&&!a.startsWith("SOLITUDE:court:"))return false;
        if(!SolitudeCity.here(g)||g.solitude.custody()||g.road.pending()||g.hp<=0)return true;
        if(a.startsWith("SOLITUDE:court:"))return SolitudeCourt.chooseCity(g,a);
        if(a.startsWith("SOLITUDE:wolf:"))return SolitudeWolf.choosePalace(g,a);
        if(a.startsWith("SOLITUDE:grace:"))return chooseGrace(g,a);
        if(a.startsWith("SOLITUDE:wine:"))return chooseWine(g,a);
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
