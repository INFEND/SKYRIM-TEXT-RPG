package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only catalog presentation; no invented magnitudes, timers or mechanics. */
final class ItemEffectRow {
    private static final Pattern DURATION=Pattern.compile("(?:,\\s*|:\\s*)(\\d+\\s*(?:сек\\.?|мин\\.?|ч\\.?))\\.?$");
    private static final Pattern VALUE=Pattern.compile("^(.*?)(?::\\s*|\\s+)([+−-]?\\d+(?:[.,]\\d+)?\\s*(?:%|ед\\.?|/сек\\.?)?)\\.?$");
    final String label,value,duration;
    final int direction;
    ItemEffectRow(String label,String value,String duration,int direction){
        this.label=label;this.value=value;this.duration=duration;this.direction=direction;
    }
    static ArrayList<ItemEffectRow> parse(String raw){
        ArrayList<ItemEffectRow> rows=new ArrayList<>();
        if(raw==null)return rows;
        for(String part:raw.split("\\s*\\|\\s*")){
            String label=part.trim(),value="",duration="";
            if(label.isEmpty())continue;
            Matcher time=DURATION.matcher(label);
            if(time.find()){duration=time.group(1);label=label.substring(0,time.start()).trim();}
            Matcher magnitude=VALUE.matcher(label);
            if(magnitude.matches()){
                label=magnitude.group(1).trim();value=magnitude.group(2).trim().replace('-', '−');
            }
            String e=label.toLowerCase(Locale.ROOT).replace('ё','е');
            int direction=0;
            if(e.contains("уязвим")||e.contains("снижен")||e.contains("опустош")||e.contains("урон регенерац")||e.startsWith("урон здоров")||e.startsWith("урон магии")||e.startsWith("урон запас")||e.startsWith("урон вынослив")||e.contains("паралич")||e.contains("замедлен"))direction=-1;
            else if(e.contains("восстановлен")||e.contains("восстанавлива")||e.contains("сопротивлен")||e.contains("повышен")||e.startsWith("регенерац"))direction=1;
            else if(value.startsWith("+"))direction=1;
            else if(value.startsWith("−"))direction=-1;
            if(!value.isEmpty()&&!value.startsWith("+")&&!value.startsWith("−")){
                if(e.startsWith("восстановление"))value="+"+value;
                else if(e.startsWith("урон здоров")||e.startsWith("урон магии")||e.startsWith("урон запас")||e.startsWith("урон вынослив"))value="−"+value;
            }
            rows.add(new ItemEffectRow(label,value,duration,direction));
        }
        return rows;
    }
}
