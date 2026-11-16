package dev.infend.skyrimtext;
import java.util.*;

/** Saved, map-addressed follow-up tasks. These are compact text-RPG adaptations, not imported full side quests. */
final class RoadQuests {
    static final class Task {
        final String id,title,place,body,enemy,required,item;final int gold;
        Task(String id,String title,String place,String body,String enemy,String required,int gold,String item){this.id=id;this.title=title;this.place=place;this.body=body;this.enemy=enemy;this.required=required;this.gold=gold;this.item=item;}
        String key(){return "lead_"+id;}
        String combatKey(){return "road_cleared_"+id;}
    }
    static final Task[] TASKS={
        new Task("treasure","Спрятанный клад","Привал Хамвира","Записка приводит к каменным развалинам. Под плитой спрятан небольшой дорожный тайник.","","",80,""),
        new Task("mistwatch","Пленники Туманной заставы","Туманная застава","У крепости слышны голоса разбойников. Освободи пленников, не позволяя охране поднять тревогу.","Главарь разбойников","",90,""),
        new Task("pilgrim","Паломничество к Азуре","Святилище Азуры","Ты находишь святилище и разговариваешь с его служителями о дальнейшей судьбе Звезды Азуры.","","",15,""),
        new Task("afflicted","След Страдающих","Святилище Периайта","У святилища ты узнаёшь о заболевших и двемерских руинах Бтардамза. Это отдельная зацепка, не завершение даэдрического задания.","","",15,""),
        new Task("kynareth","У священного дерева","Вайтран","Ты расспрашиваешь служителей Кинарет о Златолисте. Они благодарят за доставленные вести.","","",20,""),
        new Task("island_hairy_hunter","Стая Морозной Луны","Ущелье Фростмуна","Ты находишь убежище стаи и знакомишься с её обычаями. Охотники не нападают без причины.","","",15,""),
        new Task("island_frossel","Добыча из Фросселя","Фроссель","След разбойника приводит в пещеру. Остатки его банды охраняют спрятанную добычу.","Разбойник Солстейма","",100,""),
        new Task("island_torkild","Родственные связи","Деревня скаалов","Ты передаёшь Вульфу Дикой Крови последние вести о Торкильде. Он благодарит за то, что узнал судьбу брата.","","Письмо Торкильда",75,""),
        new Task("island_torkild_dead","Последнее письмо Торкильда","Деревня скаалов","Вульф получает письмо брата и прощается с ним. Ему больше не нужно ждать возвращения Торкильда.","","Письмо Торкильда",75,""),
        new Task("letter_museum","Приглашение Сила Весула","Данстар","Ты находишь музей и передаёшь приглашение. Сил рассказывает о Мифическом Рассвете и частях бритвы Мерунеса.","","Приглашение в музей Данстара",15,""),
        new Task("letter_brotherhood","Чёрная ладонь","Морфал","Записка и слухи приводят к следу Тёмного Братства. Продолжение доступно в сюжетной линии Братства.","","Таинственная записка Братства",10,""),
        new Task("letter_septimus","Вести от Септимия","Винтерхолд","Ты доставляешь письмо и уточняешь путь к исследователю. Дальнейшее исследование связано с основной историей.","","Письмо Септимия",15,""),
        new Task("hakar","Тайник Хакара","Вайтран","В условленном месте находишь оставленные Стражей Рассвета припасы.","","Распоряжения Хакара",30,"Припасы Стражи Рассвета"),
        new Task("saliah","Северный тайник Салии","Данстар","Сведения позволяют найти скрытую кладовую Стражи Рассвета.","","Распоряжения Салии",30,"Припасы Стражи Рассвета"),
        new Task("lynoit","Западный тайник Линоита","Маркарт","Ты обнаруживаешь скрытую кладовую по распоряжениям погибшего дозорного.","","Распоряжения Линоита",30,"Припасы Стражи Рассвета"),
        new Task("dead_pendant","Возвращение кулона","Вайтран","Ты передаёшь найденный кулон местному караульному, чтобы вещь могла вернуться близким погибшей.","","Кулон погибшей путницы",45,""),
        new Task("island_blackbook","След Чёрной книги","Воронья Скала","В Вороньей Скале ты расспрашиваешь местных исследователей о записи безумца. Полученные сведения сохраняются для будущих поисков.","","Сведения о Чёрной книге",20,""),
        new Task("boethiah","Книга культиста","Святилище Боэтии","Ты находишь святилище и узнаёшь о культе Боэтии. Книга помогла найти место; испытание самой принцессы ещё впереди.","","Испытание Боэтии",15,""),
        new Task("dwemer_children","Переправа Глубинного Народа","Переправа Глубинного Народа","По подсказке детей ты обнаруживаешь древнюю переправу и осматриваешь двемерские развалины.","","",25,""),
        new Task("wolf_escape","След бойцовых волков","Пещера Крегслейн","След приводит к месту незаконных боёв. Оставшийся у входа бандит не хочет пропускать свидетеля.","Бандит Крегслейна","",65,""),
        new Task("headless","Привал Хамвира","Привал Хамвира","Призрачный всадник исчезает у старых могил. Ты осматриваешь захоронения и находишь забытый тайник.","","",45,""),
        new Task("afflicted_ruins","След в Бтардамзе","Бтардамз","Сведения от святилища приводят к заражённым руинам. У входа стоит вооружённый Страдающий; уничтожь угрозу и выясни, откуда пришли больные.","Страдающий","",65,""),
        new Task("afflicted_return","Донесение о Страдающих","Святилище Периайта","Ты сообщаешь о найденном в руинах. Это завершает расследование, но не заменяет испытание Периайта.","","",35,""),
        new Task("mistwatch_return","Судьба освобождённых","Виндхельм","Поговори с караульными о людях, спасённых из Туманной заставы. Их принимают в безопасном месте.","","",40,""),
        new Task("wolf_escape_report","Доклад о бойцовых зверях","Рифтен","Сообщи страже о незаконных боях и оставшихся следах дрессировщиков.","","",35,""),
        new Task("frossel_report","След разбойничьей банды","Воронья Скала","Передай сведения о Фросселе местным стражникам. У них появилось основание проверить северные тропы.","","",30,""),
        new Task("blackbook_research","Исследование Чёрной книги","Тел-Митрин","Покажи запись исследователям Тел-Митрина. Они объясняют, почему обращаться с книгами Апокрифа нужно осторожно.","","Сведения о Чёрной книге",35,""),
        new Task("hakar_report","Доклад Хакара","Форт Стражи Рассвета","Доставь припасы и сведения о засаде. Дозорные благодарят за помощь и проверяют маршрут новобранца.","","Припасы Стражи Рассвета",45,""),
        new Task("saliah_report","Донесение Салии","Форт Стражи Рассвета","Передай сведения о северном тайнике. Дозорные отмечают, что запас найден.","","Припасы Стражи Рассвета",45,""),
        new Task("lynoit_report","Последний дозор Линоита","Форт Стражи Рассвета","Доложи о судьбе Линоита и найденном тайнике. Его товарищи чтут память погибшего.","","Припасы Стражи Рассвета",45,""),
        new Task("assassin_report","След заказного убийства","Вайтран","Покажи приказ караульному. Он предупреждает, что Братство не раскрывает заказчиков, и принимает сведения о нападении.","","Приказ Тёмного Братства",15,""),
        new Task("thalmor_report","Распоряжение юстициаров","Виндхельм","Передай содержание талморского приказа местному военному дозору. Они отмечают угрозу на дорогах.","","Приказ Талмора",25,""),
        new Task("ghost_grave","Потревоженная могила","Привал Хамвира","По следу призрака найди место, где расхитители оставили инструменты. У могильной плиты лежит забытая добыча.","","",45,""),
        new Task("bitten_hunter","След ночной засады","Пруд Глаз Мары","Проверь следы вампирской охоты у пруда. В этой адаптации охотник указывает на опасный участок восточных дорог.","Вампир","",60,""),
        new Task("merchant_attack","Сведения о налёте","Вайтран","Доложи о нападении на торговца. Караульный принимает приметы разбойников и обещает проверить тракт.","","",30,""),
        new Task("island_mutiny","След пиратов Хакнира","Отмель Хакнира","Осмотри лагерь, куда звала пиратка. Оставшиеся разбойники охраняют припасы.","Пират","",75,""),
        new Task("boethiah_report","Возвращение от святилища","Виндхельм","Ты возвращаешься в город с предупреждением о культе. Испытание Боэтии не считается завершённым.","","Испытание Боэтии",15,""),
        new Task("frost_intro","Слово дано","Рифтен","Луи Летруш просит навестить Сибби Чёрный Вереск. Сибби указывает резиденцию, где держат Мороза.","","",0,""),
        new Task("frost_lodge","Документы и Мороз","Резиденция Черный Вереск","Преодолей охрану резиденции, найди родословную Мороза и выведи коня из стойла.","Наёмник Чёрного Вереска","",0,"Родословная Мороза"),
        new Task("frost_keep","Оставить Мороза себе","Рифтен","Ты присваиваешь Мороза. Луи считает сделку сорванной.","","Родословная Мороза",0,""),
        new Task("frost_deliver","Передать Мороза Луи","Рифтен","Луи получает коня, как было обещано.","","Родословная Мороза",150,""),
        new Task("frost_maven","Предупредить Мавен","Рифтен","Ты сообщаешь Мавен о сделке и передаёшь коня Луи.","","Родословная Мороза",150,""),
        new Task("sanguine_intro","Незабываемая ночка","Вайтран","Сэм Гевен приглашает на состязание в выпивке. Следующее утро начинается с провала в памяти.","","",0,""),
        new Task("sanguine_temple","Беспорядок в храме","Маркарт","Сенна рассказывает о вчерашнем происшествии в храме Дибеллы и направляет в Рорикстед.","","",0,""),
        new Task("sanguine_goat","Пропавшая коза","Рорикстед","Эннис просит вернуть козу, проданную великану. Преодолей угрозу и верни животное.","Великан","",0,""),
        new Task("sanguine_ring","Несостоявшаяся свадьба","Вайтран","Изольда вспоминает кольцо и указывает путь к Морвунскару.","","",0,""),
        new Task("sanguine_final","Встреча с Сангвином","Морвунскар","Победи мага и найди портал к Сэму. Он раскрывает свою сущность и вручает Розу Сангвина.","Маг Морвунскара","",0,"Роза Сангвина"),
        new Task("azura_intro","Видение Арании","Святилище Азуры","Арания Иенит рассказывает о видении эльфийского мага в Винтерхолде.","","",0,""),
        new Task("azura_nelacar","Разговор с Нелакаром","Винтерхолд","Нелакар объясняет историю Малина Варена и направляет в Глубины Илиналты.","","",0,""),
        new Task("azura_star","Повреждённая звезда","Глубины Илиналты","Некроманты защищают останки Малина Варена. Добудь повреждённую звезду.","Некромант","",0,"Повреждённая Звезда Азуры"),
        new Task("azura_aranea","Вернуть звезду Азуре","Святилище Азуры","Арания помогает войти в звезду. Уничтожь душу Малина Варена и очисти артефакт.","Малин Варен","Повреждённая Звезда Азуры",0,"Звезда Азуры"),
        new Task("azura_black","Передать звезду Нелакару","Винтерхолд","Нелакар помогает войти в звезду и уничтожить Малина Варена. Артефакт становится Чёрной звездой.","Малин Варен","Повреждённая Звезда Азуры",0,"Звезда Чёрной Азуры"),
        new Task("east_intro","Солнце восходит на востоке","Виндхельм","Ортус Эндарио просит выяснить причины нападений Кровавых хоркеров на корабли.","","",0,""),
        new Task("east_pirates","Пиратское убежище","Каприз Яфета","Победи Халдина на базе пиратов и сорви налёты на торговые суда.","Халдин","",0,""),
        new Task("east_return","Доклад Ортусу","Виндхельм","Восточная имперская компания принимает подтверждение разгрома пиратов.","","",200,""),
        new Task("steward_intro","Новый управитель","Тел-Митрин","Нелот просит найти нового управителя в Вороньей Скале.","","",0,""),
        new Task("steward_recruit","Найти Дроваса","Воронья Скала","Дровас Релви соглашается отправиться к Нелоту, умалчивая о своём долге.","","",0,""),
        new Task("steward_return","Управитель Нелота","Тел-Митрин","Дровас приступает к работе. Нелот платит за помощь; Моргрул требует вернуть долг беглеца.","","",100,""),
        new Task("mogrul_debt","Новый долг","Воронья Скала","Моргрул требует тысячу септимов за Дроваса. Пока долг не оплачен, его люди преследуют должника.","","",0,""),
        new Task("riverwood_rival","Любовный треугольник","Ривервуд","Свен и Фендал соперничают за Камиллу. Выбери, чьё письмо передать.","","",0,""),
        new Task("riverwood_sven","Поддержать Свена","Ривервуд","Ты передаёшь Камилле письмо, компрометирующее Фендала. Свен благодарит.","","",25,""),
        new Task("riverwood_faendal","Поддержать Фендала","Ривервуд","Ты передаёшь Камилле письмо, компрометирующее Свена. Фендал благодарит.","","",25,""),
        new Task("peryite_intro","Единственное лекарство","Святилище Периайта","Кеш просит собрать безупречный рубин, серебряный слиток, ядовитый колокольчик и прах вампира.","","",0,""),
        new Task("peryite_ruby","Добыть рубин","Шорский Камень","Разыщи безупречный рубин среди запасов рудокопов. Это дополнительный способ получить ингредиент в текстовой игре.","","",0,"Безупречный рубин"),
        new Task("peryite_silver","Добыть серебро","Картвастен","Рудокопы предлагают серебряный слиток для ритуала.","","",0,"Серебряный слиток"),
        new Task("peryite_flower","Собрать колокольчик","Морфал","На болотах Морфала растёт ядовитый колокольчик. Собери нужный цветок.","","",0,"Ядовитый колокольчик"),
        new Task("peryite_dust","Добыть прах вампира","Пещера Хеймара","Победи вампира и получи прах для Кеша.","Вампир","",0,"Прах вампира"),
        new Task("peryite_ritual","Ритуал Кеша","Святилище Периайта","Передай четыре ингредиента. После ритуала Периайт поручает уничтожить Оркендора.","","",0,""),
        new Task("peryite_orkendor","Оркендор","Бтардамз","В глубинах руин Оркендор защищает своё убежище. Победи его и вернись к Периайту.","Оркендор","",0,""),
        new Task("peryite_reward","Награда Периайта","Святилище Периайта","Периайт признаёт выполненное поручение и дарует Разрушитель Заклинаний.","","",0,"Разрушитель Заклинаний"),
        new Task("aether_intro","Затерянное в веках","Аркнтамз","Призрак Катрии рассказывает об этерии. Преодолей испытание руин и найди первый осколок.","Двемерский автоматон","",0,"Этериевый осколок"),
        new Task("aether_crossing","Осколок у переправы","Переправа Глубинного Народа","Отыщи второй осколок среди двемерских сооружений.","","",0,"Этериевый осколок"),
        new Task("aether_raldbthar","Осколок Ральдбтхара","Ральдбтхар","Преодолей охрану древних руин и забери третий осколок.","Двемерский автоматон","",0,"Этериевый осколок"),
        new Task("aether_mzulft","Осколок Мзулфта","Мзулфт","В хранилище двемеров обнаруживается последний осколок.","Двемерский автоматон","",0,"Этериевый осколок"),
        new Task("aether_forge","Этериевая кузница","Руины Бталфта","Четыре осколка открывают кузницу. Победи Хозяина кузницы, затем выбери один предмет.","Хозяин кузницы","",0,""),
        new Task("aether_crown","Выковать этериевую корону","Руины Бталфта","Кузница создаёт корону из собранного этерия. Катрия обретает покой.","","",0,"Этериевая корона"),
        new Task("aether_shield","Выковать этериевый щит","Руины Бталфта","Кузница создаёт щит из собранного этерия. Катрия обретает покой.","","",0,"Этериевый щит"),
        new Task("aether_staff","Выковать этериевый посох","Руины Бталфта","Кузница создаёт посох из собранного этерия. Катрия обретает покой.","","",0,"Этериевый посох")
    };
    static Task find(String id){for(Task t:TASKS)if(t.id.equals(id))return t;return null;}
    static String title(String key){for(Task t:TASKS)if(t.key().equals(key))return t.title;return key;}
    static boolean active(GameEngine g,Task t){return "ACTIVE".equals(g.progress.questOutcome(t.key()));}
    static boolean here(GameEngine g,Task t){return t.place.equalsIgnoreCase(g.progress.location);}
    static List<Task> visible(GameEngine g){ArrayList<Task> out=new ArrayList<>();for(Task t:TASKS)if(g.progress.questOutcome(t.key())!=null||(!branchGroup(t.id).isEmpty()&&availableStart(g,t)))out.add(t);return out;}
    private static String next(String id){switch(id){
        case "afflicted":return "afflicted_ruins";
        case "afflicted_ruins":return "afflicted_return";
        case "mistwatch":return "mistwatch_return";
        case "wolf_escape":return "wolf_escape_report";
        case "island_frossel":return "frossel_report";
        case "island_blackbook":return "blackbook_research";
        case "hakar":return "hakar_report";
        case "saliah":return "saliah_report";
        case "lynoit":return "lynoit_report";
        case "boethiah":return "boethiah_report";
        case "peryite_intro":return "peryite_ritual";
        case "peryite_ritual":return "peryite_orkendor";
        case "peryite_orkendor":return "peryite_reward";
        case "afflicted_return":return "peryite_intro";
        case "aether_intro":return "aether_crossing";
        case "aether_crossing":return "aether_raldbthar";
        case "aether_raldbthar":return "aether_mzulft";
        case "aether_mzulft":return "aether_forge";
        case "frost_intro":return "frost_lodge";
        case "sanguine_intro":return "sanguine_temple";
        case "sanguine_temple":return "sanguine_goat";
        case "sanguine_goat":return "sanguine_ring";
        case "sanguine_ring":return "sanguine_final";
        case "azura_intro":return "azura_nelacar";
        case "azura_nelacar":return "azura_star";
        case "pilgrim":return "azura_intro";
        case "east_intro":return "east_pirates";
        case "east_pirates":return "east_return";
        case "steward_intro":return "steward_recruit";
        case "steward_recruit":return "steward_return";
        case "steward_return":return "mogrul_debt";
        default:return "";
    }}
    static void offer(GameEngine g,String id){Task t=find(id);if(t!=null)g.progress.setQuestOutcome(t.key(),"ACTIVE");}
    static boolean done(GameEngine g,String id){return "COMPLETED".equals(g.progress.questOutcome("lead_"+id));}
    private static String branchGroup(String id){switch(id){
        case "frost_keep":return "frost_choice";
        case "frost_deliver":return "frost_choice";
        case "frost_maven":return "frost_choice";
        case "azura_aranea":return "azura_choice";
        case "azura_black":return "azura_choice";
        case "riverwood_sven":return "riverwood_choice";
        case "riverwood_faendal":return "riverwood_choice";
        case "aether_crown":case "aether_shield":case "aether_staff":return "aether_choice";
        default:return "";
    }}
    private static boolean startable(GameEngine g,Task t){return here(g,t)&&availableStart(g,t);}
    static boolean availableStart(GameEngine g,Task t){if(g.progress.questOutcome(t.key())!=null)return false;switch(t.id){
        case "frost_intro":case "sanguine_intro":case "azura_intro":case "east_intro":case "steward_intro":case "riverwood_rival":case "peryite_intro":case "aether_intro":return true;
        case "frost_keep":return done(g,"frost_lodge")&&g.progress.questOutcome("frost_choice")==null;
        case "frost_deliver":return done(g,"frost_lodge")&&g.progress.questOutcome("frost_choice")==null;
        case "frost_maven":return done(g,"frost_lodge")&&g.progress.questOutcome("frost_choice")==null;
        case "azura_aranea":return done(g,"azura_star")&&g.progress.questOutcome("azura_choice")==null;
        case "azura_black":return done(g,"azura_star")&&g.progress.questOutcome("azura_choice")==null;
        case "riverwood_sven":return done(g,"riverwood_rival")&&g.progress.questOutcome("riverwood_choice")==null;
        case "riverwood_faendal":return done(g,"riverwood_rival")&&g.progress.questOutcome("riverwood_choice")==null;
        case "aether_crown":case "aether_shield":case "aether_staff":return done(g,"aether_forge")&&g.progress.questOutcome("aether_choice")==null;
        default:return false;
    }}
    static void choices(GameEngine g,GameEngine.Scene s){for(Task t:TASKS){if(active(g,t)&&here(g,t))s.add("ROADQUEST:"+t.id,t.title,t.body);else if(startable(g,t))s.add("ROADSTART:"+t.id,t.title,"Начать поручение или выбрать развилку");}if(debtActive(g)&&"Воронья Скала".equals(g.progress.location)){s.add("ROADDEBT:pay","Погасить долг Моргрулу","1000 септимов");s.add("ROADDEBT:negotiate","Снизить долг","Красноречие | 500 септимов");}}
    static void start(GameEngine g,String id){Task t=find(id);if(g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode))||t==null||!startable(g,t))return;String group=branchGroup(id);if(!group.isEmpty()){if(!t.required.isEmpty()&&g.progress.quantity(t.required)==0){g.message="Сначала восстанови предмет: "+t.required;return;}if("aether_choice".equals(group)&&g.progress.quantity("Этериевый осколок")<4){g.message="Сначала собери четыре осколка.";return;}g.progress.setQuestOutcome(group,"COMPLETED");}offer(g,id);g.mode="WORLD";g.message="Принято: "+t.title+". "+t.body;g.recordVisualEvent(g.message);}
    static boolean debtActive(GameEngine g){return "ACTIVE".equals(g.progress.questOutcome("lead_mogrul_debt"));}
    static void payDebt(GameEngine g,boolean reduced){if(g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode))||!"Воронья Скала".equals(g.progress.location)||!debtActive(g))return;int cost=reduced?500:1000;if(g.gold<cost){g.message="Не хватает септимов.";g.mode="RESULT";return;}if(reduced&&!g.rollCheck(g.progress.chance(g,2,12,50))){g.message="Моргрул отказывается снизить долг.";g.mode="RESULT";return;}g.gold-=cost;g.progress.setQuestOutcome("lead_mogrul_debt","COMPLETED");g.progress.sync(g);g.message="Долг погашен. Головорезы Моргрула больше не преследуют тебя.";g.mode="RESULT";}
    private static final String[] PERYITE_ITEMS={"Безупречный рубин","Серебряный слиток","Ядовитый колокольчик","Прах вампира"};
    static void choose(GameEngine g,String action){
        if(g.road.pending()||!("WORLD".equals(g.mode)||"RESULT".equals(g.mode)))return;
        Task t=find(action.substring("ROADQUEST:".length()));if(t==null||!active(g,t)||!here(g,t))return;
        if("mogrul_debt".equals(t.id)){g.message="Моргрул требует 1000 септимов. Оплати долг или попробуй договориться.";g.mode="WORLD";return;}
        if("peryite_ritual".equals(t.id))for(String item:PERYITE_ITEMS)if(g.progress.quantity(item)<1){g.message="Для ритуала Кеша требуется: "+item;g.mode="RESULT";return;}
        if(("aether_forge".equals(t.id)||"aether_choice".equals(branchGroup(t.id)))&&g.progress.quantity("Этериевый осколок")<4){g.message="Для кузницы нужны четыре осколка.";g.mode="RESULT";return;}
        if(!t.required.isEmpty()&&g.progress.quantity(t.required)==0){g.message="Не хватает предмета: "+t.required;g.mode="RESULT";return;}
        if(!t.enemy.isEmpty()&&!"COMPLETED".equals(g.progress.questOutcome(t.combatKey()))){g.beginRoadQuestBattle(t.id,t.enemy,t.body);return;}
        if(!t.item.isEmpty()&&!g.progress.addItem(t.item,1)){g.message="Освободи место в сумке для награды.";g.mode="RESULT";return;}
        if("peryite_ritual".equals(t.id))for(String item:PERYITE_ITEMS)g.progress.removeItem(item,1);
        if("aether_choice".equals(branchGroup(t.id)))g.progress.removeItem("Этериевый осколок",4);
        if(t.id.equals("azura_aranea")||t.id.equals("azura_black"))g.progress.removeItem("Повреждённая Звезда Азуры",1);
        if(t.id.equals("frost_keep"))g.progress.setQuestOutcome("road_mount","COMPLETED");
        if(t.id.equals("frost_keep")||t.id.equals("frost_deliver")||t.id.equals("frost_maven"))g.progress.removeItem("Родословная Мороза",1);
        // Evidence retained except explicitly surrendered; terminal state prevents duplicate awards.
        g.gold=GameEngine.nonnegativeAdd(g.gold,t.gold);g.addXp(25);g.progress.setQuestOutcome(t.key(),"COMPLETED");
        g.message=t.body+"\n\nЗадание завершено. +"+t.gold+" септимов, +25 опыта."+(t.item.isEmpty()?"":" Получено: "+t.item+".");
        if("frost_lodge".equals(t.id))g.message+="\nВернись в Рифтен и выбери судьбу Мороза.";if("azura_star".equals(t.id))g.message+="\nВыбери Аранию у святилища или Нелакара в Винтерхолде.";if("riverwood_rival".equals(t.id))g.message+="\nВыбери, чьё письмо передать Камилле.";
        if("peryite_intro".equals(t.id)){for(String step:new String[]{"peryite_ruby","peryite_silver","peryite_flower","peryite_dust"})offer(g,step);g.message+="\nСпособы добыть ингредиенты отмечены в журнале.";}
        if("peryite_ritual".equals(t.id))for(String step:new String[]{"peryite_ruby","peryite_silver","peryite_flower","peryite_dust"})g.progress.setQuestOutcome("lead_"+step,"COMPLETED");
        if("aether_forge".equals(t.id))g.message+="\nВыбери один этериевый предмет через журнал или историю.";
        String nextId=next(t.id);Task nextTask=find(nextId);if(nextTask!=null&&g.progress.questOutcome(nextTask.key())==null){offer(g,nextId);g.message+="\nСледующая цель: "+nextTask.place+" | "+nextTask.title+".";}
        if(t.id.endsWith("_report")&&(t.id.startsWith("hakar")||t.id.startsWith("saliah")||t.id.startsWith("lynoit")))g.factions.roadTrust(6,3);
        g.mode="RESULT";g.progress.sync(g);g.recordVisualEvent(t.title+": завершено");
    }
    static void victory(GameEngine g,String battleKind){
        if(!battleKind.startsWith("ROAD_QUEST:"))return;
        Task t=find(battleKind.substring("ROAD_QUEST:".length()));if(t==null||!active(g,t))return;
        g.progress.setQuestOutcome(t.combatKey(),"COMPLETED");g.message+="\n\nОпасность устранена. Заверши поручение в этой локации через журнал или историю.";
    }
    static boolean item(GameEngine g,String name){if(g.progress.quantity(name)>0)return true;return g.progress.addItem(name,1);}
    static String evidence(GameEngine g,String name){return item(g,name)?" Получено: "+name+".":" Не хватает места для предмета: "+name+".";}
    static String evidenceFor(String id){switch(id){
        case "treasure":return "Записка искателя сокровищ";
        case "hakar":return "Распоряжения Хакара";
        case "saliah":return "Распоряжения Салии";
        case "lynoit":return "Распоряжения Линоита";
        case "island_torkild":case "island_torkild_dead":return "Письмо Торкильда";
        case "letter_museum":return "Приглашение в музей Данстара";
        case "letter_brotherhood":return "Таинственная записка Братства";
        case "letter_septimus":return "Письмо Септимия";
        case "island_blackbook":return "Сведения о Чёрной книге";
        case "boethiah":return "Испытание Боэтии";
        case "dead_pendant":return "Кулон погибшей путницы";
        case "mistwatch":return "Письмо о пленных";
        case "assassin_report":return "Приказ Тёмного Братства";
        case "thalmor_report":return "Приказ Талмора";
        default:return "";
    }}
    static String lead(GameEngine g,String id){
        Task existing=find(id);if(existing!=null&&done(g,id))return "Это поручение уже завершено.";
        String name=evidenceFor(id);
        if(!name.isEmpty()&&!item(g,name))return "Не хватает места для "+name+".";
        offer(g,id);Task t=find(id);return (name.isEmpty()?"":"Получено: "+name+". ")+(t==null?"":"Цель в журнале: "+t.place+".");
    }
}
