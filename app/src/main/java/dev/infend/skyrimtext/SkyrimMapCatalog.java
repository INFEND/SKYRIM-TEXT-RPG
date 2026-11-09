package dev.infend.skyrimtext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Offline geographic facts. No grid-cell positions or random scatter. */
public final class SkyrimMapCatalog {
    public static final class Site {
        public final String id,name,english,type,grid;
        public final int sheet,region;
        public final float worldX,worldY,x,y;
        public final boolean registered;
        private final String search;
        Site(String row){
            String[] v=row.split("\\|",-1);
            id=v[0];name=v[1];english=v[2];type=v[3];
            sheet=Integer.parseInt(v[4]);worldX=Float.parseFloat(v[5]);
            worldY=Float.parseFloat(v[6]);region=Integer.parseInt(v[7]);
            grid=sheet==0?"Скайрим":"Солстейм";
            // 32-cell LOD textures, 4096 game units per cell, 4096px sheet.
            x=v.length>9?Float.parseFloat(v[8]):2048f+worldX/128f;
            y=v.length>9?Float.parseFloat(v[9]):2048f-worldY/128f;
            registered=v.length<=10||!v[10].equals("interpolated");
            search=normalize(name+" "+english+" "+type);
        }
        public boolean matches(String query){return search.contains(normalize(query));}
        public boolean capital(){return type.equals("Столица");}
    }
    private static final ArrayList<Site> SITES=new ArrayList<>();
    private static final HashMap<String,Site> INDEX=new HashMap<>();
    static {
        HashSet<String> ids=new HashSet<>();
        for(String row:SkyrimMapPoints.ROWS){
            Site site=new Site(row);
            if(!ids.add(site.id))throw new IllegalStateException("Duplicate map ID: "+site.id);
            SITES.add(site);
            INDEX.put(normalize(site.id),site);
            INDEX.put(normalize(site.name),site);
            INDEX.putIfAbsent(normalize(site.english),site);
        }
        alias("Камень Шора","Shor's Stone");
        alias("Камни-хранители","The Guardian Stones");
        alias("Кинсгроув","Kynesgrove");
        alias("Мзинчалефт","Mzinchaleft");
        alias("Саретийская ферма","Sarethi Farm");
        alias("Ферма Сарети","Sarethi Farm");
        alias("Роща Кин","Kynesgrove");
        alias("Убежище Тёмного Братства","Dark Brotherhood Sanctuary");
    }
    private SkyrimMapCatalog(){}
    private static void alias(String name,String english){
        Site site=INDEX.get(normalize(english));
        if(site!=null)INDEX.put(normalize(name),site);
    }
    public static String normalize(String text){
        return text==null?"":text.trim().toLowerCase(Locale.ROOT).replace('ё','е');
    }
    public static ArrayList<Site> all(){return new ArrayList<>(SITES);}
    public static int size(){return SITES.size();}
    public static int visibleCount(Set<String> hiddenTypes){return visibleCount(hiddenTypes,-1);}
    public static int visibleCount(Set<String> hiddenTypes,int region){
        int count=0;
        for(Site site:SITES)if((region<0||site.region==region)&&
            (hiddenTypes==null||!hiddenTypes.contains(site.type)))count++;
        return count;
    }
    public static Site find(String name){return INDEX.get(normalize(name));}
    public static String travelDestination(Site site){
        if(site==null)return "";
        if(site.english.equals("To Solstheim (DB)"))return "Воронья Скала";
        if(site.english.equals("To Skyrim"))return "Виндхельм";
        return site.name;
    }
}

