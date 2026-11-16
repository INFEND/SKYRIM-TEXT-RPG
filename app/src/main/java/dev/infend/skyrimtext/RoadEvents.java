package dev.infend.skyrimtext;
import java.util.*;
import org.json.JSONObject;

/** Saved, weighted road scenes. The validated journey is resumed exactly once. */
final class RoadEvents {
    static final String MODE="ROAD";
    private static final class Event {
        final String id,title,body,enemy,rule; final int level,weight;
        Event(String id,String title,String body,String enemy,int level,int weight,String rule){this.id=id;this.title=title;this.body=body;this.enemy=enemy;this.level=level;this.weight=weight;this.rule=rule;}
    }
    private static final Event[] EVENTS={
        new Event("giant","Великан у фермы Пелагио","У посевов великан размахивает дубиной. Фаркас удерживает его внимание, Рия ищет возможность ударить сбоку, Эйла натягивает тетиву. За их спинами видны стены Вайтрана.","Великан",1,1,"unique"),
        new Event("cicero","Запоздалые похороны","У обочины стоит повозка со сломанным колесом. Шут Цицерон тревожно охраняет гроб своей матери и просит уговорить Ванту Лорея помочь с ремонтом. Фермер подозревает неладное.","",1,1,"unique"),
        new Event("robber","Грабитель на тракте","Из-за камней выходит вооружённый путник и требует дорожную плату. Его рука лежит на рукояти клинка.","Грабитель",1,9,""),
        new Event("marauder","Мародёры","Несколько вооружённых людей обыскивают перевёрнутую телегу. Заметив тебя, они бросают добычу и перекрывают дорогу.","Главарь мародёров",1,7,""),
        new Event("thief","Вор","Незнакомец преграждает путь и требует отдать кошелёк. По его лёгким доспехам видно, что он привык действовать быстро.","Вор",1,8,""),
        new Event("bandits","Бандитская засада","На узком участке тракта из укрытия показываются разбойники. Один из них уже держит лук наготове.","Главарь бандитов",1,10,""),
        new Event("imperial_prisoner","Имперский конвой","Трое легионеров ведут связанного бойца Братьев Бури. Пленный смотрит на тебя, конвой не останавливается.","Имперский конвой",1,7,"escort"),
        new Event("storm_prisoner","Конвой Братьев Бури","Мятежники сопровождают пленного имперского солдата. Он с трудом поспевает за конвоем.","Конвой Братьев Бури",1,7,"escort"),
        new Event("thalmor_prisoner","Талморские юстициары","Талморский юстициар и двое сопровождающих ведут человека, обвинённого в почитании Талоса. Охрана холодно оценивает путника.","Талморский юстициар",1,6,"escort"),
        new Event("assassin","Заказ на твою жизнь","Из тени выходит человек в тёмной броне с кинжалом. На его поясе виден свёрнутый приказ об убийстве.","Ассасин Тёмного Братства",1,4,"assassin"),
        new Event("assassin_other","Чужой контракт","По обочине движется скрытный человек в броне Тёмного Братства. Он высматривает другого путника и не проявляет к тебе враждебности.","Ассасин Тёмного Братства",1,3,""),
        new Event("assassin_mistake","Ошибка в контракте","Ассасин преграждает дорогу и называет чужое имя. Описание его цели подозрительно похоже на тебя.","Ассасин Тёмного Братства",1,3,"assassin"),
        new Event("false_imperials","Ложные легионеры","Люди в имперских доспехах требуют пошлину. Чуть дальше лежат тела настоящих солдат.","Бандит в имперской броне",1,5,""),
        new Event("merchant_attack","Нападение на торговца","Двое бандитов окружили торговца и его лошадь. Тот тщетно прикрывает голову руками.","Бандиты",1,6,"rescue"),
        new Event("bandit_battle","Разборки разбойников","Трое бандитов загнали своего бывшего товарища к скале. Они спорят о добыче.","Главарь бандитов",1,4,"rescue"),
        new Event("scavenger","Падальщик","На месте недавней стычки вооружённый мародёр снимает с погибших доспехи. Он следит за тобой.","Мародёр",1,4,""),
        new Event("old_orc","Достойная смерть","Пожилой орк стоит возле убитых саблезубов. Он ищет противника, способного подарить ему достойную смерть.","Старый орк",1,3,"duel"),
        new Event("fugitive","Беглец и охотник","Беглец просит спрятать украденную вещь. Следом появляется её владелец и спрашивает, куда тот убежал.","",1,4,"fugitive"),
        new Event("maiq","М'Айк Лжец","Каджит в дорожном одеянии останавливается у камня и с загадочной улыбкой готов поделиться наблюдениями о Скайриме.","",1,4,"talk"),
        new Event("bard","Талсгар Странник","Бард несёт лютню и направляется к ближайшей таверне. Он готов поговорить и исполнить песню.","",1,5,"talk"),
        new Event("bard_attack","Бард в беде","Талсгар Странник отбивается от разбойников, пытаясь не выпустить лютню.","Бандит",1,4,"rescue"),
        new Event("courier","Курьер","Запыхавшийся гонец спешит в следующее поселение, прижимая сумку с письмами.","",1,6,"talk"),
        new Event("imperial_patrol","Имперский патруль","Имперские солдаты идут к своему лагерю. Старший дозорный наблюдает за трактом.","",1,5,"talk"),
        new Event("storm_patrol","Патруль Братьев Бури","Отряд Братьев Бури возвращается с дозора. Солдаты обсуждают движение легиона.","",1,5,"talk"),
        new Event("civil_battle","Гражданская война","На дороге столкнулись имперцы и Братья Бури. Между группами летят стрелы.","Солдаты противника",1,4,"civil"),
        new Event("hunter","Охотники","Охотники преследуют оленя, а после выстрела собираются разделать добычу.","",1,7,"talk"),
        new Event("wolves","Волки на охоте","Два волка гонят оленей вдоль дороги. Один хищник замечает тебя.","Волки",1,7,""),
        new Event("bandit_animal","Разбойник и зверь","Разбойник отбивается от дикого зверя, отступая к деревьям.","Дикий зверь",1,4,"rescue"),
        new Event("vigilants","Дозорные Стендарра","Двое дозорных направляются к своему залу и предупреждают об опасности даэдра.","",1,5,"talk"),
        new Event("vigilant_skeletons","Дозорный против скелетов","Дозорный Стендарра отбивается от оживших костей на обочине.","Скелеты",1,4,"rescue"),
        new Event("vigilant_vampire","Ночная схватка","Дозорные Стендарра сражаются с вампиром у тёмной рощи.","Вампир",1,4,"night"),
        new Event("vigilant_atronach","Дозорные против атронаха","Три дозорных пытаются уничтожить призванного атронаха.","Атронах",1,4,"rescue"),
        new Event("mage","Неудачливый маг","Молодой заклинатель тщетно упражняется с посохом. Он охотно рассказывает о своих неудачах.","",1,4,"talk"),
        new Event("college","Отказ Коллегии","Расстроенный маг рассказывает, что не смог поступить в Коллегию Винтерхолда.","",15,3,"talk"),
        new Event("atronach","Сбежавший атронах","Вдали бродит призванный атронах, оставшийся без хозяина.","Атронах",1,4,""),
        new Event("witch","Ведьма и атронах","Ведьма пытается справиться с вышедшим из-под контроля атронахом.","Атронах",5,3,"rescue"),
        new Event("warlock","Колдун и разбойники","Трое бандитов теснят колдуна. В воздухе пахнет озоном.","Главарь бандитов",5,3,"rescue"),
        new Event("recruit_empire","Доброволец легиона","Данмер-фермер направляется в Солитьюд, чтобы записаться в Имперский легион.","",1,4,"talk"),
        new Event("recruit_storm","Доброволец Братьев Бури","Имперец оставил хозяйство и идёт в Виндхельм вступить в ряды мятежников.","",1,4,"talk"),
        new Event("pilgrim","Фалдрус","Данмер-паломник направляется к святилищу Азуры и делится причиной своего путешествия.","",1,4,"talk"),
        new Event("drunk_orc","Пьяный орк","С дороги доносится грубый смех. Пьяный орк ищет повод для драки.","Пьяный орк",1,4,""),
        new Event("bitten_hunter","Укушенный охотник","Охотник жалуется на укус вампира и просит помощи, пока болезнь не зашла слишком далеко.","",1,4,"heal"),
        new Event("dead_pendant","Похищенный кулон","У дороги лежит погибшая женщина. Следы ведут к разбойникам, забравшим её кулон.","Бандиты",1,4,"rescue"),
        new Event("treasure","Погибший искатель сокровищ","У дорожного камня лежит тело путешественника с запиской о спрятанном сокровище.","",1,4,"loot"),
        new Event("revelers","Весельчаки","Трое путников устроили маленький пир на обочине. Они приветствуют тебя и предлагают присоединиться.","",1,4,"talk"),
        new Event("giant_cow","Дар великану","Фермер ведёт раскрашенную корову, надеясь задобрить великанов.","",1,4,"plains"),
        new Event("giant_bandits","Великаны против бандитов","Банда пытается напасть на великанов и мамонта. Земля сотрясается от ударов дубин.","Бандиты",1,3,"plains"),
        new Event("sabre_mammoth","Саблезубы и мамонт","Два саблезуба атакуют отбившегося от стада мамонта.","Саблезубы",1,3,"plains"),
        new Event("forsworn_orcs","Изгои и орочьи охотники","В ущелье Предела изгои окружают орков-охотников.","Изгой",1,4,"reach"),
        new Event("forsworn_merchant","Изгои напали на торговца","Изгои остановили торговца на горной дороге Предела.","Изгой",1,4,"reach"),
        new Event("forsworn_soldiers","Изгои и легион","Воины Изгоев устроили засаду имперскому отряду.","Изгой",1,4,"reach"),
        new Event("spriggan","Спригганы против охотников","Корни шевелятся у ног охотников: два сприггана атакуют нарушителей лесного покоя.","Спригган",8,3,"forest"),
        new Event("spriggan_troll","Спригганы и тролль","Вдали спригганы бьются с троллем.","Тролль",12,3,"north"),
        new Event("dragon_giant","Дракон и великан","Дракон обрушивается на великана; тот отвечает взмахом дубины.","Дракон",1,2,"dragonplains"),
        new Event("dragon_flight","Тень дракона","Высоко над трактом пролетает дракон, не замечая тебя.","Дракон",1,4,"dragon"),
        new Event("dragon_attack","Нападение дракона","Рёв с неба заглушает все звуки. Дракон снижается к дороге.","Дракон",1,3,"dragon"),
        new Event("dragon_duel","Дуэль драконов","Два дракона сцепились над долиной. Их крики эхом отражаются от склонов.","Дракон",1,2,"dragon"),
        new Event("dragon_emissary","Посланник Алдуина","Дракон кружит над дальним горным логовом, словно выискивая что-то.","Дракон",1,2,"dragon"),
        new Event("skeevers","Злокрысы и собаки","Стая злокрысов напала на бродячих собак возле придорожных кустов.","Злокрысы",1,4,"rescue"),
        new Event("orc_hunters","Орочьи охотники","Двое орков идут по следу оленя и обсуждают добычу.","",1,4,"talk"),
        new Event("madwoman","Странная женщина","Путница бормочет о Синем дворце и просит странной услуги.","",1,3,"talk"),
        new Event("balbus","Балбус ищет Гурмана","Путник с мешочком редких ингредиентов рассказывает о своём любимом поваре.","",1,3,"talk"),
        new Event("thalmor_storm","Талмор против Братьев Бури","Талморский отряд столкнулся с бойцами мятежников.","Талморский юстициар",1,3,"rescue"),
        new Event("wedding","Гости на свадьбу","Нарядные путники с телохранителем следуют в Солитьюд на свадьбу Виттории Вичи.","",1,3,"wedding"),
        new Event("wedding_lost","Заблудившиеся гости","Нарядные путники возвращаются из Солитьюда и не могут найти дорогу.","",1,3,"afterwedding"),
        new Event("afflicted","Страдающий","Больной бретонец рассказывает о странном недуге и святилище Периайта.","",1,3,"talk"),
        new Event("alikir","Алик'рские воины","Двое алик'рцев расспрашивают редгардку, приняв её за разыскиваемую женщину.","Алик'рский воин",1,3,""),
        new Event("farmer_refugee","Напуганная путница","Женщина в панике просит указать дорогу к безопасному поселению.","",1,4,"talk"),
        new Event("adventurer","Искатель приключений","Вооружённый путешественник делится слухом о близлежащих руинах.","",1,4,"talk"),
        new Event("mercenary","Наёмник","Наёмник направляется к месту очередного заказа и не спешит раскрывать его детали.","",1,4,"talk"),
        new Event("stray_dog","Бродячий пёс","Одинокий пёс идёт следом, принюхиваясь к твоей сумке.","",1,4,"talk"),
        new Event("werewolf","Оборотень","На дороге видны следы когтей. Из темноты появляется оборотень.","Оборотень",10,3,"night"),
        new Event("vampire","Вампир на дороге","Под покровом ночи путник внезапно обнажает клыки.","Вампир",5,4,"night"),
        new Event("cultists","Культисты Мирака","Люди в костяных масках спрашивают, действительно ли ты Довакин.","Культист Мирака",1,2,"cult"),
        new Event("solstheim_reavers","Пепельные разбойники","На пепельной тропе Солстейма разбойники выходят из укрытия.","Разбойник Солстейма",1,6,"island"),
        new Event("solstheim_ash","Порождения пепла","Из серой земли поднимаются пепельные существа.","Порождение пепла",1,5,"island")
    };
    String id="",destination="",outcome="",last="";
    int serial;
    boolean resuming;
    private static Event find(String id){for(Event e:EVENTS)if(e.id.equals(id))return e;return null;}
    boolean pending(){return !destination.isEmpty()&&find(id)!=null;}
    static boolean pair(String a,String b,String x,String y){return (a.equals(x)&&b.equals(y))||(a.equals(y)&&b.equals(x));}
    static String unique(GameEngine g,TravelPlanner.Route route){
        for(int i=1;i<route.stops.size();i++){
            String a=route.stops.get(i-1),b=route.stops.get(i);
            if(g.progress.questOutcome("road_giant")==null&&g.factions.stage(0)==0&&!g.factionStories.started(0)&&
                (pair(a,b,"Ривервуд","Вайтран")||a.equals("Ферма Пелагио")||b.equals("Ферма Пелагио")))return "giant";
            if(g.progress.questOutcome("road_cicero")==null&&g.factions.stage(3)==0&&!g.factionStories.started(3)&&
                (a.equals("Ферма Лорея")||b.equals("Ферма Лорея")||pair(a,b,"Вайтран","Данстар")))return "cicero";
        }
        return "";
    }
    static boolean eligible(GameEngine g,Event e,int region){
        if(g.level<e.level||"unique".equals(e.rule))return false;
        if(region==9)return "island".equals(e.rule);
        if("island".equals(e.rule))return false;
        boolean dragons=g.canonicalStage>6||g.progress.dragonStage>=4;
        boolean night=g.hour>=22||g.hour<5;
        switch(e.rule){
            case "reach":return region==4;
            case "plains":return region==0||region==7;
            case "forest":return region==1||region==5||region==0||region==8;
            case "north":return region==7||region==6||region==2;
            case "dragonplains":return dragons&&(region==0||region==7);
            case "dragon":return dragons;
            case "cult":return g.canonicalStage>=9;
            case "night":return night;
            case "wedding":return g.progress.questOutcome("db_wedding")==null;
            case "afterwedding":return "COMPLETED".equals(g.progress.questOutcome("db_wedding"));
            default:return !"assassin".equals(e.id)||(!g.factionStories.started(3)&&g.factions.stage(3)==0);
        }
    }
    boolean begin(GameEngine g,TravelPlanner.Route route,WorldAtlas.Place target){return begin(g,route,target,new Random());}
    boolean begin(GameEngine g,TravelPlanner.Route route,WorldAtlas.Place target,Random rng){
        if(pending()||route==null||!route.allowed||route.days<=0||route.fare>0)return false;
        String forced=unique(g,route);
        if(!forced.isEmpty())id=forced;
        else{
            if(rng.nextInt(100)>=Math.min(60,25+route.days*8))return false;
            ArrayList<Event> pool=new ArrayList<>();int total=0;
            int region=g.region==target.region?g.region:target.region;
            for(Event e:EVENTS)if(!last.equals(e.id)&&eligible(g,e,region)){pool.add(e);total+=e.weight;}
            if(total==0)return false;
            int roll=rng.nextInt(total);
            for(Event e:pool){roll-=e.weight;if(roll<0){id=e.id;break;}}
        }
        if(find(id)==null)return false;
        destination=target.name;outcome="";serial=serial==Integer.MAX_VALUE?1:serial+1;
        g.mode=MODE;g.message=find(id).body;return true;
    }
    private String action(String verb){return "ROAD:"+serial+":"+id+":"+verb;}
    GameEngine.Scene scene(GameEngine g){
        Event e=find(id);
        if(e==null)return new GameEngine.Scene("В ДОРОГЕ","Переход","Событие недоступно.").primary("GO_WORLD","Продолжить путь","");
        GameEngine.Scene s=new GameEngine.Scene("СЛУЧАЙНАЯ ВСТРЕЧА | В ДОРОГЕ",e.title,e.body+"\n\nМаршрут: "+g.progress.location+" → "+destination+".");
        if("giant".equals(id))return s.primary(action("fight"),"Помочь Соратникам","Вступить в бой с великаном").add(action("watch"),"Наблюдать за боем","Поговорить с Эйлой после схватки").add(action("leave"),"Пройти мимо","Не вмешиваться");
        if("cicero".equals(id))return s.primary(action("help"),"Убедить Ванту Лорея","Помочь Цицерону починить повозку").add(action("report"),"Доложить страже","Поддержать фермера").add(action("leave"),"Пройти мимо","Вернуться к этому позже");
        if("escort".equals(e.rule))return s.primary(action("leave"),"Пропустить конвой","Не вмешиваться").add(action("talk"),"Расспросить о пленнике","Узнать причину задержания").add(action("free"),"Освободить пленника","Вступить в бой с охраной");
        if("civil".equals(e.rule))return s.primary(action("leave"),"Обойти сражение","Не вмешиваться").add(action("empire"),"Помочь имперцам","Сразиться с Братьями Бури").add(action("storm"),"Помочь Братьям Бури","Сразиться с имперцами");
        if("fugitive".equals(e.rule))return s.primary(action("return"),"Вернуть вещь охотнику","Рассказать о беглеце").add(action("keep"),"Оставить вещь себе","Скрыть беглеца").add(action("leave"),"Не вмешиваться","");
        if("heal".equals(e.rule))return s.primary(action("help"),"Отдать лечебное зелье","Если есть в сумке").add(action("talk"),"Спросить о вампирах","Узнать о нападении").add(action("leave"),"Продолжить путь","");
        if("loot".equals(e.rule))return s.primary(action("search"),"Изучить записку","Поискать полезные сведения").add(action("leave"),"Не трогать тело","");
        if("robber".equals(id)||"thief".equals(id)||"false_imperials".equals(id))s.primary(action("pay"),"Заплатить","До 25 септимов").add(action("talk"),"Попытаться договориться","Проверка красноречия");
        else if("assassin_mistake".equals(id))s.primary(action("talk"),"Объяснить ошибку","Проверка красноречия");
        else if(e.enemy.isEmpty()||"assassin_other".equals(id))s.primary(action("talk"),"Заговорить","Узнать больше");
        if(!e.enemy.isEmpty())s.add(action("fight"),"rescue".equals(e.rule)?"Вмешаться и помочь":"Вступить в бой","Реальная пошаговая схватка");
        if("assassin".equals(id)||"dragon_attack".equals(id)||"bandits".equals(id))s.add(action("sneak"),"Попытаться скрыться","Проверка скрытности");
        else s.add(action("leave"),"Продолжить путь","Не вмешиваться");
        return s;
    }
    private void fact(GameEngine g,String key,String title,String status){try{g.sceneState.setContext(g.day,g.hour,g.progress.location);g.sceneState.confirm("npc",key,title,status);}catch(Exception ignored){}}
    private void uniqueOutcome(GameEngine g,String state){g.progress.setQuestOutcome("road_"+id,state);}
    private void finish(GameEngine g,String text){outcome=text;last=id;g.message=text;g.mode="RESULT";g.progress.sync(g);g.recordVisualEvent(find(id).title+": "+text);}
    private void fight(GameEngine g,String enemy){Event e=find(id);last=id;outcome="Встреча: "+e.title;g.recordVisualEvent(outcome);g.beginRoadBattle(enemy,("giant".equals(id)?100:enemy.contains("Дракон")?130:55)+g.level*5,("giant".equals(id)?11:enemy.contains("Дракон")?13:6)+g.level/3,e.body);}
    void choose(GameEngine g,String action){
        if(!pending()||!MODE.equals(g.mode))return;
        Event e=find(id);String verb=null;
        for(GameEngine.Choice c:scene(g).choices)if(c.action.equals(action)){verb=action.substring(action.lastIndexOf(':')+1);break;}
        if(verb==null)return;
        if("giant".equals(id)){
            uniqueOutcome(g,"COMPLETED");
            if("fight".equals(verb)){fact(g,"road_aela","Эйла Охотница","MET");fight(g,e.enemy);}
            else {if("watch".equals(verb))fact(g,"road_aela","Эйла Охотница","INFORMED");finish(g,"watch".equals(verb)?"Соратники одолели великана. Эйла предлагает встретиться с Кодлаком Белой Гривой в Йоррваскре.":"Ты оставляешь Соратников позади.");}
            return;
        }
        if("cicero".equals(id)){
            if("leave".equals(verb)){finish(g,"Ты оставляешь шута у повозки. Ещё можно вернуться.");return;}
            if("help".equals(verb)){
                uniqueOutcome(g,"COMPLETED");fact(g,"road_cicero","Цицерон","HELPED");g.gold=GameEngine.nonnegativeAdd(g.gold,75+g.level*5);g.progress.skillExperience(g,12,20);
                finish(g,"Ты убеждаешь Лорея помочь. Цицерон благодарит тебя и платит "+(75+g.level*5)+" септимов.");return;
            }
            uniqueOutcome(g,"FAILED");fact(g,"road_cicero","Цицерон","HOSTILE");g.gold=GameEngine.nonnegativeAdd(g.gold,50);
            finish(g,"Стражник задерживает Цицерона. Лорей платит 50 септимов. Шут запомнил твой выбор.");return;
        }
        switch(verb){
            case "fight":fight(g,e.enemy);return;
            case "free":fight(g,e.enemy);return;
            case "empire":fight(g,"Братья Бури");return;
            case "storm":fight(g,"Имперские солдаты");return;
            case "pay":if(g.gold<25){fight(g,e.enemy);return;}g.gold-=25;finish(g,"Ты отдаёшь 25 септимов и получаешь возможность пройти.");return;
            case "talk":
                if("escort".equals(e.rule)){finish(g,"Охрана сообщает, что пленник задержан по приказу начальства. Конвой продолжает путь.");return;}
                if(!e.enemy.isEmpty()&&!"assassin_other".equals(id)){
                    if(g.rollCheck(g.progress.chance(g,2,12,25))){g.progress.skillExperience(g,12,15);finish(g,"Твои доводы убеждают собеседника. Удаётся избежать столкновения.");}
                    else fight(g,e.enemy);return;
                }
                g.progress.skillExperience(g,12,8);finish(g,"Ты беседуешь с путником и прощаешься. Новые сведения останутся в хронике.");return;
            case "sneak":if(g.rollCheck(g.progress.chance(g,2,9,25))){g.progress.skillExperience(g,9,15);finish(g,"Ты скрываешься и обходишь опасность.");}else fight(g,e.enemy);return;
            case "help":if(g.potions>0){g.potions--;g.reputation++;finish(g,"Ты делишься лечебным зельем. Охотник благодарит за помощь; лечение болезни требует особого средства.");}else finish(g,"У тебя нет лечебного зелья, но охотник получает совет обратиться за помощью.");return;
            case "return":g.reputation++;finish(g,"Ты возвращаешь вещь владельцу и объясняешь, куда ушёл беглец.");return;
            case "keep":g.gold=GameEngine.nonnegativeAdd(g.gold,15);finish(g,"Ты оставляешь вещь себе и позже получаешь за неё 15 септимов.");return;
            case "search":boolean added=g.progress.addItem("Отмычка",1);finish(g,"Ты изучаешь записку."+ (added?" Найдена отмычка.":" Сумка не позволяет взять находку."));return;
            default:finish(g,"Ты не вмешиваешься и продолжаешь дорогу.");
        }
    }
    void resume(GameEngine g){
        if(!pending())return;
        String target=destination,previous=id;
        if(outcome.startsWith("Встреча: ")&&"WILD".equals(g.battleKind)&&"RESULT".equals(g.mode)&&g.enemyHp==0){
            outcome=g.message;
            if("giant".equals(previous)){fact(g,"road_aela","Эйла Охотница","HELPED");outcome+="\nЭйла приглашает тебя в Йоррваскр, к Кодлаку Белой Гриве.";}
            else if("escort".equals(find(previous).rule))outcome+="\nПленник освобождён.";
        }else if(outcome.startsWith("Встреча: "))outcome=g.message;
        destination="";id="";last=previous;resuming=true;
        try{g.choose("LOC:"+target);}finally{resuming=false;outcome="";}
    }
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("id",id).put("destination",destination).put("outcome",outcome).put("last",last).put("serial",serial);}catch(Exception ignored){}return j;}
    static RoadEvents restore(JSONObject j){RoadEvents r=new RoadEvents();if(j!=null){r.id=j.optString("id","");r.destination=j.optString("destination","");r.outcome=j.optString("outcome","");r.last=j.optString("last","");r.serial=Math.max(0,j.optInt("serial"));if(!r.pending()){r.id="";r.destination="";}}return r;}
}
