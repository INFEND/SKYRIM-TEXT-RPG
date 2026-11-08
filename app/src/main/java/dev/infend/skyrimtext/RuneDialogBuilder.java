package dev.infend.skyrimtext;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Shared Nordic presentation for ordinary confirmations, item details, codex entries,
 * save import, and content lists. The original Builder callbacks and behavior are
 * preserved; only the presentation changes.
 */
public final class RuneDialogBuilder extends AlertDialog.Builder {
    private final Context context;
    private final int accent;

    public RuneDialogBuilder(Context context, int accent) {
        super(context, R.style.RunicDialogTheme);
        this.context=context;
        this.accent=accent;
    }

    private int dp(float value){
        return Math.round(context.getResources().getDisplayMetrics().density*value);
    }

    @Override public AlertDialog create(){
        final AlertDialog dialog=super.create();
        dialog.setOnShowListener(ignored->{
            Window window=dialog.getWindow();
            if(window!=null){
                window.setBackgroundDrawable(new RuneFrameDrawable(0xf0060809,accent,true));
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams attributes=window.getAttributes();
                attributes.dimAmount=.72f;
                window.setAttributes(attributes);
            }
            int titleId=context.getResources().getIdentifier("alertTitle","id","android");
            if(titleId!=0){
                View titleView=dialog.findViewById(titleId);
                if(titleView instanceof TextView){
                    TextView title=(TextView)titleView;
                    title.setTypeface(Typeface.SERIF,Typeface.NORMAL);
                    title.setTextColor(0xfff1f2f0);
                    title.setTextSize(22);
                    title.setLineSpacing(dp(2),1.04f);
                }
            }
            TextView message=dialog.findViewById(android.R.id.message);
            if(message!=null){
                message.setTypeface(Typeface.SERIF,Typeface.NORMAL);
                message.setTextColor(0xffbec5c7);
                message.setTextSize(16);
                message.setLineSpacing(dp(4),1.08f);
            }
            styleButton(dialog.getButton(DialogInterface.BUTTON_POSITIVE),true);
            styleButton(dialog.getButton(DialogInterface.BUTTON_NEGATIVE),false);
            styleButton(dialog.getButton(DialogInterface.BUTTON_NEUTRAL),false);
        });
        return dialog;
    }

    private void styleButton(Button button,boolean primary){
        if(button==null)return;
        button.setAllCaps(false);
        button.setTypeface(Typeface.SERIF,Typeface.NORMAL);
        button.setTextSize(15);
        button.setTextColor(primary?accent:0xffc4cbcd);
        button.setBackground(new RuneFrameDrawable(primary?0xff202426:0xff0b0f11,
            primary?accent:0xff70797d,primary));
        button.setMinHeight(dp(44));
        button.setPadding(dp(12),dp(6),dp(12),dp(6));
    }
}
