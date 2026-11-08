package dev.infend.skyrimtext;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;

/** Renders authored scene dialogue without exposing Markdown markers to players. */
public final class SceneTypography {
    private SceneTypography(){}

    public static SpannableStringBuilder render(String raw,int accent){
        SpannableStringBuilder result=new SpannableStringBuilder();
        if(raw==null||raw.isEmpty())return result;
        String[] lines=raw.split("\n",-1);
        for(int i=0;i<lines.length;i++){
            String line=lines[i];
            boolean spoken=line.startsWith(">");
            if(spoken)line=line.substring(1).trim();
            line=line.replace("**","");
            int start=result.length();
            result.append(line);
            int end=result.length();
            if(spoken && end>start){
                result.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(0xffe7dbc6),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new LeadingMarginSpan.Standard(20),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(line.endsWith(":") && line.length()>1 && line.length()<64 && line.indexOf('.')<0){
                result.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(accent),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(0.92f),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if(i<lines.length-1)result.append("\n");
        }
        return result;
    }
}
