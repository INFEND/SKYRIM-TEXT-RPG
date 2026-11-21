package dev.infend.skyrimtext;
import org.json.JSONObject;

/** Cosmetic purchases never affect stats or global preferences. */
final class FixShopCosmetics {
    static final String[] TITLES={"Дитя Севера","Пепельный странник","Хранитель сумерек","Наследник драконов"};
    static final String[] FRAMES={"Северное серебро","Даэдрический рубин","Лунный аметист","Золото Совнгарда"};
    static final int[] COLORS={0xffb6c5d5,0xffbd414e,0xffa87be1,0xffd8b36a};
    int titles=0,frames=0,title=-1,frame=-1;
    boolean owns(boolean isFrame,int i){return i>=0&&i<4&&((isFrame?frames:titles)&(1<<i))!=0;}
    boolean select(boolean isFrame,int i){if(i!=-1&&!owns(isFrame,i))return false;if(isFrame)frame=i;else title=i;return true;}
    boolean buy(boolean isFrame,int i){if(i<0||i>=4||owns(isFrame,i))return false;if(isFrame)frames|=1<<i;else titles|=1<<i;return select(isFrame,i);}
    String title(){return title>=0?TITLES[title]:"";}
    int color(){return frame>=0?COLORS[frame]:0;}
    JSONObject save(){JSONObject j=new JSONObject();try{j.put("titles",titles).put("frames",frames).put("title",title).put("frame",frame);}catch(Exception ignored){}return j;}
    static FixShopCosmetics restore(JSONObject j){FixShopCosmetics c=new FixShopCosmetics();if(j==null)return c;c.titles=j.optInt("titles")&15;c.frames=j.optInt("frames")&15;c.select(false,j.optInt("title",-1));c.select(true,j.optInt("frame",-1));return c;}
}
