package dev.infend.skyrimtext;
import org.json.JSONObject;
public final class LivingEconomy {
 public int capital=0,accrued=0; private int settledDay=-1;
 public int income(){return capital/50;}
 public void sync(int day){day=Math.max(1,day);if(settledDay<0){settledDay=day;return;}if(day<=settledDay)return;accrued=(int)Math.min(Integer.MAX_VALUE,(long)accrued+(long)(day-settledDay)*income());settledDay=day;}
 public boolean invest(GameEngine g,int amount){sync(g.day);if(amount<=0||g.gold<amount||capital>Integer.MAX_VALUE-amount)return false;g.gold-=amount;capital+=amount;return true;}
 public boolean withdraw(GameEngine g,int amount){sync(g.day);if(amount<=0||capital<amount||g.gold>Integer.MAX_VALUE-amount)return false;capital-=amount;g.gold+=amount;return true;}
 public int claim(GameEngine g){sync(g.day);if(g.gold<0)return 0;int n=Math.min(accrued,Integer.MAX_VALUE-g.gold);g.gold+=n;accrued-=n;return n;}
 public JSONObject toJson(){JSONObject o=new JSONObject();try{o.put("capital",capital).put("accrued",accrued).put("day",settledDay);}catch(Exception ignored){}return o;}
 public static LivingEconomy fromJson(JSONObject o){LivingEconomy e=new LivingEconomy();if(o!=null){e.capital=Math.max(0,o.optInt("capital"));e.accrued=Math.max(0,o.optInt("accrued"));e.settledDay=o.optInt("day",-1);}return e;}
}
