package dev.infend.skyrimtext;
import org.json.JSONArray;
import org.json.JSONObject;
/** Condensed playable adaptation; atlas entry points may stand for nearby dungeons. */
final class FactionStories {
    static final class Chapter {
        final String title,place,story,enemy;final int skill;
        Chapter(String[] x){title=x[0];place=x[1];story=x[2];enemy="-".equals(x[3])?"":x[3];skill=Integer.parseInt(x[4]);}
    }
    private static final String[][][] DATA={{{"К оружию","Вайтран","Кодлак принимает претендента, Вилкас проверяет его выучку.","-","0"},
{"Испытание доблести","Вайтран","Фаркас ведёт героя за обломком Вутрада в древний курган.","Драугр","0"},
{"Серебряная рука","Вайтран","Круг раскрывает тайну оборотней. Герой принимает кровь Эйлы и выступает против охотников.","Охотник Серебряной руки","1"},
{"Кровь и честь","Фолкрит","Кодлак ищет избавления от проклятия, след ведёт к Гленморильским ведьмам.","Гленморильская ведьма","13"},
{"Чистота мести","Данстар","После гибели Кодлака Вилкас требует вернуть части Вутрада.","Воин Серебряной руки","1"},
{"Последний долг","Винтерхолд","В гробнице Исграмора предстоит освободить дух Кодлака.","Волчий дух","0"}},
{{"Первые уроки","Коллегия Винтерхолда","Фаральда проверяет способности героя, Толфдир знакомит его с защитными чарами.","-","15"},
{"В глубинах Саартала","Саартал","Раскопки Толфдира приводят к обнаружению Ока Магнуса.","Йурик Голдурсон","13"},
{"Библиотечные книги","Вайтран","Ураг просит вернуть похищенные книги из крепости Феллглоу.","Враждебный маг","13"},
{"Благие намерения","Коллегия Винтерхолда","Авгур Данлейнский предупреждает об опасности Ока.","-","15"},
{"Открытие невидимого","Виндхельм","В Мзулфте двемерский механизм указывает путь к посоху.","Двемерский автоматон","15"},
{"Ликвидация последствий","Винтерхолд","Око выходит из-под контроля, магические аномалии угрожают городу.","Магическая аномалия","13"},
{"Посох Магнуса","Лабиринтиан","Морокеи хранит посох, способный остановить Око.","Морокеи","13"},
{"Око Магнуса","Коллегия Винтерхолда","Анкано использует Око; посох позволяет лишить его защиты.","Анкано","13"}},
{{"Случайная встреча","Рифтен","Бриньольф предлагает испытание на рынке.","-","11"},
{"Надёжная крыша","Рифтен","Гильдия поручает уладить долги торговцев.","-","12"},
{"Ясность","Золотое поместье","Мерсер направляет героя за сведениями о поместье.","-","9"},
{"Неправильный мёд","Медоварня Хоннинга","Мавен поручает вмешаться в дела конкурирующей медоварни.","-","9"},
{"Каприз негодяя","Солитьюд","След загадочного покупателя ведёт к Гулум-Аю.","-","12"},
{"Разговор с тишиной","Виндхельм","В Снежной Завесе открывается предательство Мерсера.","Драугр","9"},
{"Трудные ответы","Маркарт","Предстоит расшифровать дневник Галла.","-","15"},
{"Преследование","Рифтен","Карлия возвращается; нужно найти планы Мерсера.","-","10"},
{"Возрождённая триада","Рифтен","Карлия предлагает заключить договор с Ноктюрнал.","-","9"},
{"Ослепление","Данстар","В Иркнтанде предстоит остановить Мерсера.","Мерсер Фрей","0"},
{"Возвращение сумерек","Рифтен","Скелетный ключ должен вернуться Ноктюрнал.","-","9"}},
{{"Потерянная невинность","Виндхельм","Просьба Авентуса Аретино привлекает внимание Братства.","-","9"},
{"С такими друзьями…","Морфал","Астрид предлагает вступить в Братство после испытания.","-","9"},
{"Убежище","Святилище Тёмного Братства","Назир знакомит новичка с контрактами.","-","12"},
{"Прощай, любовь","Маркарт","Муири обращается к Братству с поручением.","-","9"},
{"Шёпот во тьме","Святилище Тёмного Братства","Голос Матери Ночи меняет положение героя.","-","16"},
{"Нарушенная тишина","Вайтран","Амонд Мотьер передаёт поручение об императоре.","-","12"},
{"Пока смерть не разлучит нас","Солитьюд","Братство поручает выполнить контракт на свадьбе.","-","9"},
{"Уязвимое место","Драконий Мост","Астрид поручает устранить угрозу Пенитус Окулатус.","-","9"},
{"Лекарство от безумия","Данстар","Цицерон скрывается в старом убежище.","-","9"},
{"Рецепт катастрофы","Маркарт","Нужно установить личность Гурмана.","-","12"},
{"Убить империю","Солитьюд","План покушения оказывается ловушкой.","Агент Пенитус Окулатус","0"},
{"Смерть воплощённая","Святилище Тёмного Братства","Убежище атаковано, судьба Братства меняется.","Агент Пенитус Окулатус","0"},
{"Слава Ситису!","Солитьюд","Последний контракт ведёт на корабль императора.","-","9"}},
{{"Вступление в Легион","Форт Храгстад","Рикке требует очистить форт и доказать готовность служить.","Разбойник","0"},
{"Зубчатая корона","Виндхельм","Имперский отряд ищет древнюю корону в Корваньюнде.","Драугр","0"},
{"Послание для Вайтрана","Вайтран","Герой доставляет послание ярлу Балгруфу.","-","12"},
{"Защита Вайтрана","Вайтран","Легион защищает город от армии Братьев Бури.","Боец Братьев Бури","0"},
{"Воссоединение Скайрима","Данстар","Империя начинает возвращать северные владения.","-","9"},
{"Битва за форт Данстад","Форт Данстад","Легион атакует укреплённую позицию.","Боец Братьев Бури","0"},
{"Принуждение к дани","Рифтен","Нужно добиться поддержки местных властей.","-","12"},
{"Битва за форт Амол","Форт Амол","Легион наступает к Виндхельму.","Боец Братьев Бури","0"},
{"Битва за Виндхельм","Виндхельм","Последний штурм решает судьбу восстания.","Боец Братьев Бури","0"}},
{{"Вступление к Братьям Бури","Виндхельм","Галмар отправляет героя на испытание в северные льды.","Ледяной призрак","1"},
{"Зубчатая корона","Виндхельм","Отряд Галмара стремится вернуть корону из Корваньюнда.","Драугр","1"},
{"Послание для Вайтрана","Вайтран","Герой передаёт ярлу послание Ульфрика.","-","12"},
{"Битва за Вайтран","Вайтран","Братья Бури начинают штурм города.","Имперский солдат","1"},
{"Освобождение Скайрима","Форт Нойград","Восстание разворачивает кампанию за южные владения.","Имперский солдат","1"},
{"Принуждение к дани","Маркарт","Галмар требует добиться поддержки местных властей.","-","12"},
{"Битва за форт Сноухок","Форт Сноухок","Братья Бури наступают на укрепления Хьялмарка.","Имперский солдат","1"},
{"Битва за форт Храгстад","Форт Храгстад","Последняя позиция перед Солитьюдом должна пасть.","Имперский солдат","1"},
{"Битва за Солитьюд","Солитьюд","Восстание идёт на последний штурм столицы.","Имперский солдат","1"}},
{{"Стража Рассвета","Форт Стражи Рассвета","Изран предупреждает об угрозе вампиров.","-","12"},
{"Пробуждение","Морфал","В склепе Димхоллоу герой обнаруживает Серану и Древний свиток.","Вампир","2"},
{"Кровные узы","Солитьюд","Серана возвращается в Волкихар; герой отвергает дар Харкона.","-","12"},
{"Новый порядок","Форт Стражи Рассвета","Изран нуждается в помощи Сорин и Гунмара.","-","12"},
{"Пророк","Драконий Мост","Следы ведут к похищенному жрецу Мотылька.","Вампир","2"},
{"В поисках истины","Данстар","Из северных руин начинается путь в Чёрный Предел. Для пророчества требуются Древние свитки.","Двемерский автоматон","15"},
{"В погоне за прошлым","Солитьюд","Серана ищет путь в лабораторию Валерики.","-","15"},
{"За гранью смерти","Солитьюд","Из лаборатории Валерики открывается проход в Каирн Душ. Предстоит встретить Валерику и преодолеть защитников.","Хранитель Каирна Душ","13"},
{"Невидимые видения","Фолкрит","Свитки нужно прочитать в святилище Предка.","-","15"},
{"Прикосновение к небу","Маркарт","На западе Скайрима начинается путь в Забытую долину; лук Ауриэля скрыт среди святилищ снежных эльфов.","Виртур","2"},
{"Семейный суд","Солитьюд","Стража выступает против Харкона в замке Волкихар.","Харкон","2"}}};
    private final int[] index=new int[DATA.length],phase=new int[DATA.length],attemptDay=new int[DATA.length],preparation=new int[DATA.length],approach=new int[DATA.length];
    private int cicero=0,beast=0;private boolean beastDecision=false;
    FactionStories(){java.util.Arrays.fill(attemptDay,-1);}
    int count(int f){return DATA[f].length;}
    int done(int f){return index[f];}
    boolean started(int f){return index[f]>0||phase[f]>0;}
    boolean completed(int f){return index[f]>=count(f);}
    Chapter chapter(int f){return completed(f)?null:new Chapter(DATA[f][index[f]]);}
    int phase(int f){return phase[f];}
    int preparation(int f){return preparation[f];}
    int approach(int f){return approach[f];}
    String dialogue(int f){
        if(completed(f))return "";
        String line=FactionDialogue.scene(f,index[f]);
        if(f==3&&index[f]>8&&cicero!=0)line+="\n\n"+(cicero==1?"[Событие] Цицерон остался в живых. Твоя милость сохранена в летописи.":"[Событие] Цицерон не пережил встречу. Его судьба записана в летописи.");
        return line;
    }
    int cicero(){return cicero;}
    int beast(){return beast;}
    boolean hasDecision(int f){return !completed(f)&&phase[f]==2&&((f==3&&index[f]==8&&cicero==0)||(f==0&&index[f]==5&&beast==1&&!beastDecision));}
    String[] decisionOptions(int f){return f==3?new String[]{"Пощадить Цицерона","Убить Цицерона"}:new String[]{"Сохранить дар зверя","Исцелиться от ликантропии"};}
    boolean decide(GameEngine g,int f,int choice){
        if(!hasDecision(f)||!block(g,f).isEmpty()||choice<0||choice>1)return false;
        if(f==3){cicero=choice+1;g.progress.setQuestOutcome("cicero_"+(choice==0?"spared":"slain"),"COMPLETED");g.message=choice==0?"Цицерон пощажён.":"Цицерон погиб.";}
        else{beastDecision=true;beast=choice+1;g.progress.setQuestOutcome(choice==0?"werewolf_retained":"werewolf_cured","COMPLETED");g.message=choice==0?"Ты сохранил дар зверя.":"Ты отказался от дара зверя.";}
        g.recordVisualEvent(g.message);return true;
    }
    boolean canRewrite(GameEngine g,int f){return false&&FixShopWorld.safe(g)&&(f==3&&cicero>0&&index[3]>8||f==0&&beastDecision&&index[0]>5);}
    boolean rewrite(GameEngine g,int f){
        if(!canRewrite(g,f))return false;
        if(f==3){cicero=3-cicero;g.progress.questOutcomes.remove("cicero_spared");g.progress.questOutcomes.remove("cicero_slain");g.progress.setQuestOutcome(cicero==1?"cicero_spared":"cicero_slain","COMPLETED");g.message=cicero==1?"Судьба Цицерона изменена: жив.":"Судьба Цицерона изменена: погиб.";}
        else{beast=3-beast;g.progress.questOutcomes.remove("werewolf_retained");g.progress.questOutcomes.remove("werewolf_cured");g.progress.setQuestOutcome(beast==1?"werewolf_retained":"werewolf_cured","COMPLETED");g.message=beast==1?"Дар зверя восстановлен.":"Дар зверя снят.";}
        g.recordVisualEvent(g.message);return true;
    }
    int skill(int f){return approach[f]==2?12:chapter(f).skill;}
    int cost(int f){return approach[f]==1?12:8;}
    // Atlas labels may be renamed while faction chapters retain legacy save-compatible place keys.
    private boolean atChapterPlace(GameEngine g,int f){
        String destination=chapter(f).place;
        if(destination.equalsIgnoreCase(g.progress.location))return true;
        WorldAtlas.Place place=WorldAtlas.find(destination);
        return place!=null&&place.name.equalsIgnoreCase(g.progress.location);
    }
    boolean canChooseApproach(GameEngine g,int f){return !completed(f)&&phase[f]==1&&g.started&&("WORLD".equals(g.mode)||"RESULT".equals(g.mode))&&atChapterPlace(g,f)&&attemptDay[f]!=g.day;}
    boolean setApproach(GameEngine g,int f,int choice){if(!canChooseApproach(g,f)||choice<0||choice>2||(choice==2&&!chapter(f).enemy.isEmpty()))return false;approach[f]=choice;return true;}
    String block(GameEngine g,int f){
        if(!g.started||g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode)))return "Сначала заверши текущую сцену.";
        if(completed(f))return "Кампания завершена.";
        if(f==4&&(started(5)||g.factions.stage(5)>0)||f==5&&(started(4)||g.factions.stage(4)>0))return "Ты выбрал другую сторону гражданской войны.";
        if(!atChapterPlace(g,f))return "Прибудь: "+chapter(f).place;
        if(phase[f]==1&&attemptDay[f]==g.day)return "Следующая попытка в другой игровой день.";
        if(phase[f]==1&&g.stamina<cost(f))return "Нужно "+cost(f)+" выносливости.";
        return "";
    }
    int chance(GameEngine g,int f){
        Chapter c=chapter(f);if(c==null||phase[f]!=1||!c.enemy.isEmpty())return -1;
        // сюжетная проверка: наставники обеспечивают минимальную доступность,
        // повторная подготовка после неудачи улучшает следующую попытку.
        return Math.min(100,Math.max(15,g.progress.chance(g,2,skill(f),20+Math.min(30,index[f]*3)))+preparation[f]+(approach[f]==1?12:0));
    }
    private int skillForReward(Chapter c,int f){return approach[f]==2?12:c.skill;}
    boolean advance(GameEngine g,int f){
        if(!block(g,f).isEmpty())return false;g.lastCheckChance=g.lastCheckRoll=-1;Chapter c=chapter(f);
        if(phase[f]==0){phase[f]=1;g.progress.setQuestOutcome("faction_story_"+f,"ACTIVE");g.message=c.story;
            if(f==6&&index[f]==7)g.road.beginArvak(g);
            if(f==3&&index[f]==2){String prior=g.progress.questOutcome("road_cicero");if("COMPLETED".equals(prior))g.message+="\nЦицерон узнаёт тебя и вспоминает помощь у фермы Лорея.";else if("FAILED".equals(prior))g.message+="\nЦицерон узнаёт тебя и с неприязнью вспоминает, как ты сообщил о нём страже.";}return true;}
        if(phase[f]==2){if(f==3&&index[f]==8&&cicero==0)decide(g,f,0);if(f==0&&index[f]==2&&beast==0)beast=1;if(f==0&&index[f]==5&&hasDecision(f))decide(g,f,0);index[f]++;phase[f]=0;attemptDay[f]=-1;preparation[f]=0;int used=skillForReward(c,f);approach[f]=0;g.progress.skillExperience(g,used,35);g.addXp(30+index[f]*5);g.gold=(int)Math.min(Integer.MAX_VALUE,(long)g.gold+25+index[f]*5);g.progress.sync(g);g.message="Завершено: "+c.title;if(completed(f))g.progress.setQuestOutcome("faction_story_"+f,"COMPLETED");g.recordVisualEvent(g.message);return true;}
        int chance=chance(g,f);g.stamina-=cost(f);attemptDay[f]=g.day;
        if(!c.enemy.isEmpty()){g.beginFactionBattle(f,c.enemy,45+g.level*10+index[f]*7-(approach[f]==1?15:0),6+g.level*2,c.story);return true;}
        if(g.rollCheck(chance)){phase[f]=2;g.message="Испытание пройдено. Заверши главу.";}else{preparation[f]=Math.min(50,preparation[f]+10);g.message="Попытка не удалась. Ты разобрал ошибки с наставником. Подготовка: +"+preparation[f]+" к следующей проверке. Повтори в другой день.";}
        return true;
    }
    void victory(int f){if(f>=0&&f<DATA.length&&!completed(f)&&phase[f]==1&&!chapter(f).enemy.isEmpty())phase[f]=2;}
    JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("index",new JSONArray(index)).put("phase",new JSONArray(phase)).put("attemptDay",new JSONArray(attemptDay)).put("preparation",new JSONArray(preparation)).put("approach",new JSONArray(approach)).put("cicero",cicero).put("beast",beast).put("beastDecision",beastDecision);}catch(Exception ignored){}return o;}
    static FactionStories fromJson(JSONObject o){FactionStories s=new FactionStories();if(o==null)return s;JSONArray a=o.optJSONArray("index"),b=o.optJSONArray("phase"),d=o.optJSONArray("attemptDay"),p=o.optJSONArray("preparation"),r=o.optJSONArray("approach");for(int f=0;f<DATA.length;f++){s.index[f]=a==null?0:Math.max(0,Math.min(s.count(f),a.optInt(f)));s.phase[f]=b==null?0:Math.max(0,Math.min(2,b.optInt(f)));s.attemptDay[f]=d==null?-1:d.optInt(f,-1);s.preparation[f]=p==null?0:Math.max(0,Math.min(50,p.optInt(f,0)));s.approach[f]=r==null?0:Math.max(0,Math.min(2,r.optInt(f,0)));}s.cicero=Math.max(0,Math.min(2,o.optInt("cicero",0)));s.beast=Math.max(0,Math.min(2,o.optInt("beast",s.index[0]>=3?1:0)));s.beastDecision=o.optBoolean("beastDecision",s.index[0]>=6||s.beast==2);return s;}
}
