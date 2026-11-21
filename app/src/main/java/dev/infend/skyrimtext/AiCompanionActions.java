package dev.infend.skyrimtext;

import java.util.LinkedHashMap;

/** The model may suggest a native action; only an explicit player selection can execute it.
 * No scripted scene body or scripted choice list is rendered in AI mode. */
final class AiCompanionActions {
    static String code(String raw){
        if(raw==null||raw.length()>64||!raw.matches("(KATH|ALMA|ITACHI|LIVIA):[A-Za-z0-9_:]+"))return "";
        return raw;
    }
    static LinkedHashMap<String,GameEngine.Choice> available(GameEngine g){
        LinkedHashMap<String,GameEngine.Choice> out=new LinkedHashMap<>();
        if(!g.aiNarrativeMode||!g.started||g.enemyHp>0||"COMBAT".equals(g.mode)||g.nativeSceneRequired())return out;
        if(g.katherine.available(g))add(out,g.katherine.scene(g));
        if(g.alma.available(g))add(out,g.alma.scene(g));
        if(g.itachi.available(g))add(out,g.itachi.scene(g));
        if(g.livia.available(g))add(out,g.livia.scene(g));
        return out;
    }
    private static void add(LinkedHashMap<String,GameEngine.Choice> out,GameEngine.Scene scene){
        for(GameEngine.Choice c:scene.choices)if(!code(c.action).isEmpty()&&!c.action.endsWith(":exit")&&!c.action.endsWith(":open"))out.put(c.action,c);
    }
    static boolean allowed(GameEngine g,String code){return available(g).containsKey(code);}
    static String context(GameEngine g,int limit){
        StringBuilder b=new StringBuilder();
        java.util.LinkedHashMap<String,java.util.ArrayDeque<GameEngine.Choice>> groups=new java.util.LinkedHashMap<>();
        for(GameEngine.Choice c:available(g).values()){
            String key=c.action.substring(0,c.action.indexOf(':'));
            groups.computeIfAbsent(key,k->new java.util.ArrayDeque<>()).add(c);
        }
        boolean remaining=true;
        while(remaining){
            remaining=false;
            for(java.util.ArrayDeque<GameEngine.Choice> group:groups.values()){
                if(group.isEmpty())continue;
                remaining=true;
                GameEngine.Choice c=group.removeFirst();
                String line=c.action+"="+c.title+"; ";
                if(b.length()+line.length()<=limit)b.append(line);
            }
        }
        return b.toString();
    }
    /** Critical flags for all four NPCs fit before optional history in the local prompt. */
    static String compactProfiles(GameEngine g){
        return compact("Кэтрин",g.katherine.enabled,g.katherine.present(g),g.katherine.stage,g.katherine.trust,g.katherine.romance)
            +compact("Альма",g.alma.enabled,!g.alma.terminalLoss()&&g.alma.present(g),g.alma.stage,g.alma.trust,g.alma.romance)
            +compact("Итачи",g.itachi.enabled,g.itachi.present(g),g.itachi.stage,g.itachi.trust,g.itachi.romance)
            +compact("Ливия",g.livia.enabled,g.livia.present(g),g.livia.stage,g.livia.trust,g.livia.romance);
    }
    private static String compact(String name,boolean enabled,boolean present,int stage,int trust,boolean romance){
        if(!enabled)return name+": отключена; ";
        return name+": "+(present?"рядом":"отсутствует")+", этап="+stage+", доверие="+trust+", роман="+(romance?"да":"нет")+"; ";
    }
    static String profiles(GameEngine g){
        StringBuilder b=new StringBuilder();
        if(g.katherine.enabled&&g.katherine.present(g))b.append("Кэтрин: гордая, самостоятельная, носитель Нулевого Предела; этап ").append(g.katherine.stage).append(", доверие ").append(g.katherine.trust).append(", роман ").append(g.katherine.romance).append(". ");
        if(g.alma.enabled&&!g.alma.terminalLoss()&&g.alma.present(g))b.append("Альма: независимая бессмертная, маг крови; этап ").append(g.alma.stage).append(", доверие ").append(g.alma.trust).append(", роман ").append(g.alma.romance).append(". ");
        if(g.itachi.enabled&&g.itachi.present(g))b.append("Итачи: сдержанная, ценит свободу; этап ").append(g.itachi.stage).append(", доверие ").append(g.itachi.trust).append(", роман ").append(g.itachi.romance).append(". ");
        if(g.livia.available(g))b.append("Ливия: бывший имперский капитан, ответственна за Хелген; этап ").append(g.livia.stage).append(", доверие ").append(g.livia.trust).append(", роман ").append(g.livia.romance).append(". ");
        return b.toString();
    }
    static String execute(GameEngine g,String raw){
        GameEngine.Choice choice=available(g).get(raw);
        if(choice==null)return "";
        // Invoke the existing validated mechanics without opening their screen.
        if(raw.startsWith("KATH:")){g.mode=KatherineStory.MODE;g.katherine.choose(g,raw);}
        else if(raw.startsWith("ALMA:")){g.mode=AlmaStory.MODE;g.alma.choose(g,raw);}
        else if(raw.startsWith("ITACHI:")){g.mode=ItachiStory.MODE;g.itachi.choose(g,raw);}
        else if(raw.startsWith("LIVIA:")){g.mode=LiviaStory.MODE;g.livia.choose(g,raw);}
        g.setAiNarrativeMode(true);
        g.progress.sync(g);
        String state;
        if(raw.startsWith("KATH:"))state="Кэтрин: "+g.katherine.status()+", этап "+g.katherine.stage+", доверие "+g.katherine.trust+", роман "+g.katherine.romance+", сопровождает "+g.katherine.active()+". "+g.katherine.lastRemark;
        else if(raw.startsWith("ALMA:"))state="Альма: "+g.alma.status()+", этап "+g.alma.stage+", доверие "+g.alma.trust+", роман "+g.alma.romance+", сопровождает "+g.alma.active()+". "+g.alma.lastRemark;
        else if(raw.startsWith("ITACHI:"))state="Итачи: "+g.itachi.relation()+", этап "+g.itachi.stage+", доверие "+g.itachi.trust+", роман "+g.itachi.romance+", сопровождает "+g.itachi.active()+". "+g.itachi.memory;
        else state="Ливия: "+g.livia.status()+", этап "+g.livia.stage+", доверие "+g.livia.trust+", роман "+g.livia.romance+", сопровождает "+g.livia.active()+". "+g.livia.memory;
        return choice.title+" ["+raw+"]. Подтверждённое состояние: "+state
            +("COMBAT".equals(g.mode)?" Начат бой: "+g.enemyName+", здоровье "+g.enemyHp+".":"")
            +" Место: "+g.progress.location+"; золото: "+g.gold+".";
    }
    static boolean protectedNpc(String id,String title){
        String key=id==null?"":id.toLowerCase(java.util.Locale.ROOT);
        return KatherineInitiative.protectedFact(id,title)||key.startsWith("alma")||key.startsWith("itachi")||key.startsWith("livia")
            ||"Альма".equalsIgnoreCase(title)||"Итачи".equalsIgnoreCase(title)||"Alma".equalsIgnoreCase(title)||"Itachi".equalsIgnoreCase(title)||"Livia".equalsIgnoreCase(title)||LiviaStory.matches(title);
    }
}
