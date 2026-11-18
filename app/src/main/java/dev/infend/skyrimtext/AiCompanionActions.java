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
        for(GameEngine.Choice c:available(g).values()){
            String line=c.action+"="+c.title+"; ";
            if(b.length()+line.length()>limit)break;
            b.append(line);
        }
        return b.toString();
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
