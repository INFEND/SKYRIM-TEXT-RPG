package dev.infend.skyrimtext;

/** MS06 and SolitudeFreeform07, backed by the shared quest ledger and combat. */
final class SolitudeCourt {
    static final String TRIBUTE="solitude_elisif_tribute", OFFER=TRIBUTE+"_offered";
    static final String TRIBUTE_TITLE="Подношение Элисиф", HORN="Боевой рог Торуга", SHRINE="Святилище Талоса к северо-востоку от Вайтрана";
    static final String QUEEN="solitude_potema", KEY=QUEEN+"_key", GUARD=QUEEN+"_guard", BOSS=QUEEN+"_boss", LOOT=QUEEN+"_skull", BLESS=QUEEN+"_bless";
    static final String QUEEN_TITLE="Пробуждение Королевы-Волчицы", SKULL="Череп Потемы", SHIELD="Щит Солитьюда", LETTER=QUEEN+"_letter";
    static String state(GameEngine g,String id){return g.progress.questOutcome(id);}
    static boolean done(GameEngine g,String id){return "COMPLETED".equals(state(g,id));}
    static boolean active(GameEngine g,String id){return "ACTIVE".equals(state(g,id));}
    static boolean absent(GameEngine g,String id){return state(g,id)==null||state(g,id).isEmpty();}
    static boolean available(GameEngine g,String name,int start,int end){
        String status=g.sceneState.npcStatusByName(name);
        return g.hour>=start&&g.hour<end&&!"DEAD".equals(status)&&!"MISSING".equals(status)&&!"HOSTILE".equals(status);
    }
    static boolean ready(GameEngine g){return g.started&&g.hp>0&&!g.road.pending()&&!g.solitude.custody();}
    static final String THANE="solitude_thane", BLADE_REWARD=THANE+"_blade", BLADE="Клинок Хаафингара";
    static int favors(GameEngine g){
        int count=0;
        for(String id:new String[]{SolitudeCanonical.KEY,SolitudeCanonical.RUM,SolitudeCanonical.WINE,SolitudeCanonical.ATTIRE,SolitudeCanonical.GRACE})if(done(g,id))count++;
        return count;
    }
    static boolean thaneEligible(GameEngine g){return permission(g)&&SolitudeHome.owns(g)&&favors(g)>=5;}
    static boolean permission(GameEngine g){return done(g,TRIBUTE);}
    static boolean protect(RpgProgress p,String id){
        return (HORN.equals(id)&&"ACTIVE".equals(p.questOutcome(TRIBUTE)))||(SKULL.equals(id)&&"ACTIVE".equals(p.questOutcome(QUEEN)));
    }
    static String tributeObjective(GameEngine g){
        return done(g,TRIBUTE)?"Элисиф разрешила купить Высокий шпиль.":done(g,OFFER)?"Вернуться к Элисиф в Синий дворец.":"Отнести рог Торуга к святилищу Талоса к северо-востоку от Вайтрана.";
    }
    static String queenObjective(GameEngine g){
        if(done(g,QUEEN))return "Останки освящены; Солитьюд защищён.";
        if(done(g,BLESS))return "Доложить Фолку в Синем дворце.";
        if(done(g,LOOT))return "Отдать череп Потемы Стирру в Зале мёртвых.";
        if(done(g,BOSS))return "Забрать череп Потемы с трона.";
        if(done(g,KEY))return "Войти в катакомбы Потемы через Храм Богов и победить Королеву-Волчицу.";
        return "Поговорить со Стирром в Зале мёртвых.";
    }
    static boolean invitation(GameEngine g){return SolitudeWolf.done(g,SolitudeWolf.KEY)&&g.level>=10&&absent(g,QUEEN);}
    static void worldChoices(GameEngine g,GameEngine.Scene s){
        if(!ready(g))return;
        if(invitation(g)&&!done(g,LETTER))s.add("COURT:letter","Получить послание Фолка","Срочная весть из Солитьюда");
        if(g.region==0&&SHRINE.equals(g.progress.location)&&active(g,TRIBUTE)&&!done(g,OFFER)&&g.progress.quantity(HORN)>0)
            s.primary("COURT:offer","Возложить рог Торуга к алтарю Талоса","Подношение Элисиф");
    }
    static void chooseWorld(GameEngine g,String a){
        if(!ready(g)||!"WORLD".equals(g.mode))return;
        if("COURT:letter".equals(a)&&invitation(g)&&!done(g,LETTER)){
            g.progress.setQuestOutcome(LETTER,"COMPLETED");report(g,"Гонец передаёт срочное письмо Фолка: последствия ритуала в Волчьем Черепе оказались опаснее, чем ожидалось.");
        }else if("COURT:offer".equals(a)&&g.region==0&&SHRINE.equals(g.progress.location)&&active(g,TRIBUTE)&&!done(g,OFFER)&&g.progress.quantity(HORN)>0){
            g.progress.setQuestOutcome(OFFER,"COMPLETED");g.progress.quantity(HORN,0);report(g,"Ты кладёшь боевой рог Торуга перед алтарём Талоса. Подношение сделано. Вернись к Элисиф.");
        }
    }
    static void districtChoices(GameEngine g,GameEngine.Scene s,int d){
        if(!ready(g))return;
        if(d==2&&available(g,"Элисиф Прекрасная",9,17)&&SolitudeWolf.done(g,SolitudeWolf.KEY)){
            if(done(g,THANE)&&!done(g,BLADE_REWARD))s.add("SOLITUDE:court:blade","Получить клинок Хаафингара","Награда тана");
            if(!done(g,THANE)&&thaneEligible(g))s.add("SOLITUDE:court:thane","Просить титул тана Хаафингара","Поместье и помощь пяти жителям");
            if(absent(g,TRIBUTE))s.add("SOLITUDE:court:tribute","Выслушать личную просьбу Элисиф","Каноническое поручение");
            if(active(g,TRIBUTE)&&done(g,OFFER))s.add("SOLITUDE:court:tribute_finish","Сообщить о подношении","Право купить Высокий шпиль");
        }
        if(d==2&&SolitudeWolf.palace(g)){
            if(invitation(g)&&done(g,LETTER))s.add("SOLITUDE:court:queen","Обсудить письмо Фолка","Пробуждение Королевы-Волчицы");
            if(active(g,QUEEN)&&done(g,BLESS)&&g.gold<=Integer.MAX_VALUE-SolitudeWolf.reward(g))s.add("SOLITUDE:court:queen_finish","Доложить о гибели Потемы","Щит Солитьюда и золото");
        }
        if(d==4&&active(g,QUEEN)&&done(g,KEY)){
            if(!done(g,GUARD))s.add("SOLITUDE:court:guard","Открыть вход в катакомбы Потемы","Сражение с драугром");
            else if(!done(g,BOSS))s.add("SOLITUDE:court:boss","Войти в святилище Потемы","Сражение с духом Королевы-Волчицы");
            else if(!done(g,LOOT))s.add("SOLITUDE:court:skull","Забрать череп Потемы","Квестовый предмет");
        }
    }
    static void styrrChoices(GameEngine g,GameEngine.Scene s){
        if(!ready(g)||!active(g,QUEEN)||!available(g,"Стирр",8,20))return;
        if(!done(g,KEY))s.add("SOLITUDE:court:styrr","Поговорить со Стирром","Получить ключ от катакомб Потемы");
        else if(done(g,LOOT)&&!done(g,BLESS)&&g.progress.quantity(SKULL)>0)s.add("SOLITUDE:court:bless","Передать череп Стирру","Освящение останков");
    }
    static boolean chooseCity(GameEngine g,String a){
        if(!ready(g)||!SolitudeCity.here(g))return true;
        if("SOLITUDE:2".equals(g.mode)&&available(g,"Элисиф Прекрасная",9,17)&&SolitudeWolf.done(g,SolitudeWolf.KEY)){
            if("SOLITUDE:court:blade".equals(a)&&done(g,THANE)&&!done(g,BLADE_REWARD)){
                if(!g.progress.addItem(BLADE,1)){report(g,"Освободи место для наградного оружия.");return true;}
                g.progress.setQuestOutcome(BLADE_REWARD,"COMPLETED");report(g,"Элисиф вручает тебе клинок Хаафингара.");return true;
            }
            if("SOLITUDE:court:thane".equals(a)&&!done(g,THANE)&&thaneEligible(g)){
                g.progress.setQuestOutcome(THANE,"COMPLETED");report(g,"Элисиф признаёт твою помощь Хаафингару и дарует титул тана. Ты заслужил уважение жителей.");return true;
            }
            if("SOLITUDE:court:tribute".equals(a)&&absent(g,TRIBUTE)){
                if(g.progress.quantity(HORN)==0&&!g.progress.addItem(HORN,1))return true;
                g.progress.setQuestOutcome(TRIBUTE,"ACTIVE");report(g,"Элисиф просит почтить память Торуга: из-за запрета поклонения Талосу она не может сама отнести боевой рог мужа к его святилищу.");return true;
            }
            if("SOLITUDE:court:tribute_finish".equals(a)&&active(g,TRIBUTE)&&done(g,OFFER)){
                g.progress.setQuestOutcome(TRIBUTE,"COMPLETED");g.holdReputation[3]=Math.min(100,g.holdReputation[3]+1);
                report(g,"Элисиф благодарит тебя и разрешает купить Высокий шпиль у Фолка Огнеборода.");return true;
            }
        }
        if(SolitudeWolf.palace(g)){
            if("SOLITUDE:court:queen".equals(a)&&invitation(g)&&done(g,LETTER)){
                g.progress.setQuestOutcome(QUEEN,"ACTIVE");report(g,"Фолк сообщает: дух Потемы вырвался из ритуала. Найди жреца Аркея Стирра в Зале мёртвых.");return true;
            }
            if("SOLITUDE:court:queen_finish".equals(a)&&active(g,QUEEN)&&done(g,BLESS)){
                int gold=SolitudeWolf.reward(g);if(g.gold>Integer.MAX_VALUE-gold)return true;
                if(g.progress.quantity(SHIELD)==0&&!g.progress.addItem(SHIELD,1)){report(g,"Освободи место для Щита Солитьюда.");return true;}
                g.progress.setQuestOutcome(QUEEN,"COMPLETED");g.gold+=gold;g.holdReputation[3]=Math.min(100,g.holdReputation[3]+3);
                report(g,"Фолк объявляет тебя защитником Солитьюда и вручает Щит Солитьюда. Награда: "+gold+" септимов.");return true;
            }
        }
        if(!active(g,QUEEN))return true;
        if(SolitudeCanonical.CATACOMBS.equals(g.mode)&&available(g,"Стирр",8,20)){
            if("SOLITUDE:court:styrr".equals(a)&&!done(g,KEY)){
                g.progress.setQuestOutcome(KEY,"COMPLETED");report(g,"Стирр передаёт ключ от катакомб Потемы. Вход находится под Храмом Богов.");
            }else if("SOLITUDE:court:bless".equals(a)&&done(g,LOOT)&&!done(g,BLESS)&&g.progress.quantity(SKULL)>0){
                g.progress.setQuestOutcome(BLESS,"COMPLETED");g.progress.quantity(SKULL,0);report(g,"Стирр принимает череп Потемы и обещает освятить останки. Доложи Фолку.");
            }
        }else if("SOLITUDE:4".equals(g.mode)&&done(g,KEY)){
            if("SOLITUDE:court:guard".equals(a)&&!done(g,GUARD))
                g.beginSolitudeWolfBattle("potema_guard","Драугр катакомб Потемы",90+g.level*5,9+g.level/2,"Под Храмом Богов пробуждается страж катакомб.");
            else if("SOLITUDE:court:boss".equals(a)&&done(g,GUARD)&&!done(g,BOSS))
                g.beginSolitudeWolfBattle("potema_boss","Дух Потемы",130+g.level*7,12+g.level/2,"В святилище Королевы-Волчицы сгущается призрачная магия.");
            else if("SOLITUDE:court:skull".equals(a)&&done(g,BOSS)&&!done(g,LOOT)){
                if(g.progress.quantity(SKULL)==0&&!g.progress.addItem(SKULL,1))return true;
                g.progress.setQuestOutcome(LOOT,"COMPLETED");report(g,"На троне ты находишь череп Потемы. Отнеси его Стирру.");
            }
        }
        return true;
    }
    static boolean victory(GameEngine g){
        if(!SolitudeCity.here(g)||!active(g,QUEEN)||g.enemyHp!=0)return false;
        if((SolitudeWolf.KIND+"potema_guard").equals(g.battleKind)&&done(g,KEY)&&!done(g,GUARD)){
            g.progress.setQuestOutcome(GUARD,"COMPLETED");g.message+="\n\nПуть в святилище Потемы открыт.";
        }else if((SolitudeWolf.KIND+"potema_boss").equals(g.battleKind)&&done(g,GUARD)&&!done(g,BOSS)){
            g.progress.setQuestOutcome(BOSS,"COMPLETED");g.message+="\n\nДух Потемы повержен. Вернись в Храм Богов и забери череп с трона.";
        }else return false;
        g.progress.recent=g.message;g.progress.sync(g);return true;
    }
    private static void report(GameEngine g,String m){g.message=m;g.progress.recent=m;g.recordVisualEvent(m);g.progress.sync(g);}
}
