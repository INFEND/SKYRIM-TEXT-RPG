package dev.infend.skyrimtext;

import java.util.*;
import java.util.regex.*;

/** Exact whole-name matches, canonical identity, longest alias first. No guessed identities. */
final class NpcNameMatcher {
    static final class Match {
        final int start,end; final String name;
        Match(int start,int end,String name){this.start=start;this.end=end;this.name=name;}
    }
    private final Map<String,String> names;
    private final Pattern pattern;
    static String key(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replace('ё','е').replace('’','\'').replace("\u0301","").trim();}
    NpcNameMatcher(Map<String,String> source){
        names=new LinkedHashMap<>();
        source.forEach((alias,name)->{String k=key(alias);if(!k.isEmpty()&&k.length()<=100&&name!=null&&!name.isEmpty())names.putIfAbsent(k,name);});
        ArrayList<String> sorted=new ArrayList<>(names.keySet());
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        StringJoiner alternatives=new StringJoiner("|");
        for(String alias:sorted)alternatives.add(Pattern.quote(alias));
        pattern=sorted.isEmpty()?null:Pattern.compile("(?<![\\p{L}\\p{N}_])(?:"+alternatives+")(?![\\p{L}\\p{N}_])");
    }
    List<Match> find(CharSequence source){
        ArrayList<Match> result=new ArrayList<>();
        if(pattern==null||source==null||source.length()==0)return result;
        // Lowercase character-by-character: offsets stay identical to the displayed UTF-16 text.
        StringBuilder normalized=new StringBuilder(source.length());
        int[] offsets=new int[source.length()];
        for(int i=0;i<source.length();i++){
            char c=Character.toLowerCase(source.charAt(i));if(c=='\u0301')continue;
            offsets[normalized.length()]=i;normalized.append(c=='ё'?'е':c=='’'?'\'':c);
        }
        Matcher m=pattern.matcher(normalized);
        while(m.find()){
            int end=offsets[m.end()-1]+1;
            while(end<source.length()&&source.charAt(end)=='\u0301')end++;
            result.add(new Match(offsets[m.start()],end,names.get(m.group())));
        }
        return result;
    }
    static void add(Map<String,String> names,String canonical,String... forms){
        if(canonical==null||canonical.trim().isEmpty()||canonical.length()>100)return;
        names.putIfAbsent(key(canonical),canonical);
        for(String form:forms)if(form!=null&&!form.trim().isEmpty())names.putIfAbsent(key(form),canonical);
    }
}
