package dev.infend.skyrimtext;
import java.util.Locale;
import java.util.LinkedHashMap;

/** Named characters use curated identities rather than guesses based on their name. */
final class NpcProfiles {
    static final class Profile {
        final String name,race,gender,role;final int variant;final boolean dragon;
        Profile(String n,String r,String g,String role,int v,boolean d){name=n;race=r;gender=g;this.role=role;variant=v;dragon=d;}
    }
    private static final LinkedHashMap<String,Profile> INDEX=new LinkedHashMap<>();
    private static String key(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replace('ё','е').trim();}
    private static void add(String n,String r,String g,String role,int v,String... aliases){
        Profile p=new Profile(n,r,g,role,v,"Дракон".equals(r));INDEX.put(key(n),p);for(String alias:aliases)INDEX.put(key(alias),p);
    }
    static {
        add("Ралоф","Норд","Мужской","Брат Бури",1);
        add("Хадвар","Норд","Мужской","Имперский солдат",2);
        add("Гердур","Норд","Женский","Хозяйка лесопилки Ривервуда",1);
        add("Алвор","Норд","Мужской","Кузнец Ривервуда",3);
        add("Ярл Балгруф","Норд","Мужской","Ярл Вайтрана",3,"Балгруф","Балгруф Старший");
        add("Айрилет","Данмер","Женский","Хускарл ярла Вайтрана",1,"Ирилет");
        add("Фаренгар","Норд","Мужской","Придворный маг Вайтрана",2,"Фаренгар Тайный Огонь");
        add("Арнгейр","Норд","Мужской","Наставник Седобородых",3);
        add("Дельфина","Бретонец","Женский","Член Клинков",2,"Дельфин");
        add("Эсберн","Норд","Мужской","Знаток драконов, член Клинков",1);
        add("Малборн","Босмер","Мужской","Помощник в Талморском посольстве",1);
        add("Эленвен","Альтмер","Женский","Посол Талмора",2);
        add("Бриньольф","Норд","Мужской","Член Гильдии воров",2);
        add("Фаральда","Альтмер","Женский","Маг Коллегии Винтерхолда",1);
        add("Ураг гро-Шуб","Орк","Мужской","Хранитель Арканеума",1);
        add("Септимий Сегоний","Имперец","Мужской","Исследователь Древних свитков",3,"Септимий");
        add("Генерал Туллий","Имперец","Мужской","Командующий Имперским легионом",2,"Туллий");
        add("Ульфрик Буревестник","Норд","Мужской","Предводитель Братьев Бури",2,"Ульфрик");
        add("Галмар Каменный Кулак","Норд","Мужской","Советник Ульфрика",3,"Галмар");
        add("Тсун","Норд","Мужской","Страж Совнгарда",3);
        add("Партурнакс","Дракон","Мужской","Наставник Голоса",1);
        add("Одавинг","Дракон","Мужской","Дракон",2);
        add("Алдуин","Дракон","Мужской","Пожиратель мира",3);
        add("Мирмулнир","Дракон","Мужской","Дракон у Вайтрана",1);
        add("Салокнир","Дракон","Мужской","Дракон Рощи Кин",2);
    }
    static Profile find(String name){return INDEX.get(key(name));}
    static java.util.LinkedHashSet<String> speakers(String raw,String hero){
        java.util.LinkedHashSet<String> result=new java.util.LinkedHashSet<>();
        if(raw==null)return result;
        String[] lines=raw.replace("\r","").split("\n");
        for(int i=0;i<lines.length&&result.size()<8;i++){
            String name=lines[i].trim().replace("**","").trim();
            if(!name.endsWith(":")||name.length()>60||name.startsWith("["))continue;
            name=name.substring(0,name.length()-1).trim();
            if(name.isEmpty()||name.equalsIgnoreCase("ты")||name.equalsIgnoreCase(hero))continue;
            int next=i+1;while(next<lines.length&&lines[next].trim().isEmpty())next++;
            if(next>=lines.length)continue;
            String reply=lines[next].trim().replace("**","").replaceFirst("^>\\s*","");
            if(reply.startsWith("— ")||reply.startsWith("- "))result.add(name);
        }return result;
    }
}
