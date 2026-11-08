package dev.infend.skyrimtext;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.QuoteSpan;
import android.text.style.StyleSpan;
import android.text.style.RelativeSizeSpan;

/** Renders **bold**, *italic* and > dialogue with a visible side rule. */
public final class SceneTypography {
    private SceneTypography(){}

    private static void appendRich(SpannableStringBuilder out,String source){
        int bold=-1,italic=-1;
        for(int i=0;i<source.length();){
            if(source.startsWith("**",i)){
                if(bold<0)bold=out.length();
                else {
                    if(out.length()>bold)out.setSpan(new StyleSpan(Typeface.BOLD),bold,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bold=-1;
                }
                i+=2;continue;
            }
            if(source.charAt(i)=='*'){
                if(italic<0)italic=out.length();
                else{
                    if(out.length()>italic)out.setSpan(new StyleSpan(Typeface.ITALIC),italic,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    italic=-1;
                }
                i++;continue;
            }
            out.append(source.charAt(i++));
        }
        if(bold>=0&&out.length()>bold)out.setSpan(new StyleSpan(Typeface.BOLD),bold,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(italic>=0&&out.length()>italic)out.setSpan(new StyleSpan(Typeface.ITALIC),italic,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static SpannableStringBuilder render(String raw,int accent){
        SpannableStringBuilder result=new SpannableStringBuilder();
        if(raw==null||raw.isEmpty())return result;
        String[] lines=raw.split("\n",-1);
        java.util.ArrayList<int[]> quoteRanges=new java.util.ArrayList<>();
        for(int i=0;i<lines.length;i++){
            String line=lines[i];
            boolean spoken=line.startsWith(">");
            if(spoken)line=line.substring(1).trim();
            String visibleForSpeaker=line.replace("**","").trim();
            boolean speaker=!spoken&&visibleForSpeaker.endsWith(":")
                &&visibleForSpeaker.length()>1&&visibleForSpeaker.length()<64
                &&visibleForSpeaker.indexOf('.')<0;
            int start=result.length();
            appendRich(result,line);
            int end=result.length();
            if(i<lines.length-1)result.append("\n");
            if(spoken&&end>start){
                // QuoteSpan is a PARAGRAPH span: its end is inclusive while it is
                // at the SpannableStringBuilder's current end. Register it only
                // once all later prose has been appended, or the gold stripe
                // incorrectly expands through subsequent ordinary paragraphs.
                quoteRanges.add(new int[]{start,result.length()});
                result.setSpan(new ForegroundColorSpan(0xffeeeae5),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(speaker&&end>start){
                result.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(0xffeeeae5),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(0.96f),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        // The completed text already contains the exact line boundaries.
        // SPAN_PARAGRAPH now applies only to the spoken line, even with
        // multiline narration following it, including AI streaming fragments.
        for(int[] range:quoteRanges)
            result.setSpan(new QuoteSpan(accent),range[0],range[1],Spanned.SPAN_PARAGRAPH);
        return result;
    }
}
