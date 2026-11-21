package dev.infend.skyrimtext;
import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
/** A real foreground entry point so Android can activate a newly installed or stopped module. */
public final class EngineActivationActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(32,32,32,32);root.setBackgroundColor(Color.BLACK);
        TextView title=new TextView(this);title.setText("SKYRIM LOCAL AI");title.setTextColor(Color.WHITE);title.setTextSize(24);root.addView(title);
        TextView detail=new TextView(this);detail.setText("Движок активирован. Модель GGUF и игровые сохранения находятся в основной игре.");detail.setTextColor(0xffcccccc);detail.setTextSize(16);detail.setPadding(0,24,0,24);root.addView(detail);
        Button back=new Button(this);back.setText("Вернуться в Skyrim");root.addView(back);
        setContentView(root);
        boolean trusted="dev.infend.skyrimtext".equals(getCallingPackage())&&getPackageManager().checkSignatures(getCallingPackage(),getPackageName())==PackageManager.SIGNATURE_MATCH;
        if(trusted&&getIntent().getBooleanExtra("returnToGame",false)){root.post(()->{setResult(RESULT_OK);finish();});return;}
        back.setOnClickListener(v->{try{startActivity(new Intent().setComponent(new ComponentName("dev.infend.skyrimtext","dev.infend.skyrimtext.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception ignored){}finish();});
    }
}
