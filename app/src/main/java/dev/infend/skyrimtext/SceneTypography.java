package dev.infend.skyrimtext;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

/**
 * A dialogue is always two independent lines:
 * **Персонаж:**
 * **— Реплика**
 * The quote decoration is NEVER drawn; all other narration is italic.
 * Supports historic >**— ...** output for old saved AI stories.
 */
public final class SceneTypography {
    private SceneTypography(){}

    private static void appendRich(SpannableStringBuilder out,String source){
        int bold=-1,italic=-1;
        for(int i=0;i<source.length();){
            if(source.startsWith("**",i)){
                if(bold<0)bold=out.length();
                else{
                    if(out.length()>bold)
                        out.setSpan(new StyleSpan(Typeface.BOLD),bold,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    bold=-1;
                }
                i+=2;continue;
            }
            if(source.charAt(i)=='*'){
                if(italic<0)italic=out.length();
                else{
                    if(out.length()>italic)
                        out.setSpan(new StyleSpan(Typeface.ITALIC),italic,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    italic=-1;
                }
                i++;continue;
            }
            out.append(source.charAt(i++));
        }
        // Unclosed markup is not a license to bold the rest of the narration.
        // Style only matched ** pairs; malformed tokens are harmless.
        if(italic>=0&&out.length()>italic)
            out.setSpan(new StyleSpan(Typeface.ITALIC),italic,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // Display-only migration; never rewrite saved quest identifiers or raw AI data.
    static String displaySeparators(String raw){
        return raw==null?"":raw.replace(" "+(char)8226+" "," | ")
            .replace(" "+(char)183+" "," | ");
    }

    private static String normalize(String raw){
        String text=displaySeparators(raw).replace("\r\n","\n").replace('\r','\n');
        // The provider sometimes produces "смотришь. Стражник:" in a prose line.
        // Move a marked (or a clearly unmarked) speaker to its own paragraph.
        text=text.replaceAll("([.!?…])\\h+(\\*\\*[А-ЯЁ][^:\\n]{1,55}:\\*\\*)","$1\n\n$2");
        text=text.replaceAll("([.!?…])\\h+([А-ЯЁ][а-яА-ЯЁё \\-]{1,48}:)(?=\\h*(?:\\n|\\*\\*|[—-]))","$1\n\n$2");
        // Keep the speaker and the dash even when the AI streams both on one line.
        text=text.replaceAll("(\\*\\*[^\\n:]{1,55}:\\*\\*)\\h*(?=(?:\\*\\*)?[—-])","$1\n");
        text=text.replaceAll("(:)\\h+(?=(?:\\*\\*)?[—-]\\h)","$1\n");
        // Models sometimes put narration directly after the closing ** of a
        // spoken sentence. It is a NEW paragraph, never part of the dialogue.
        text=text.replaceAll("(?m)^(\\h*>?\\h*\\*\\*[—-][^\\n]*?\\*\\*)\\h+([^\\n]+)$","$1\n\n$2");
        return text;
    }

    public static SpannableStringBuilder render(String raw,int accent){
        return render(raw,accent,null,accent);
    }
    /** Use the actual saved character name for first-person dialogue. */
    public static SpannableStringBuilder render(String raw,int accent,String heroName,int heroColor){
        SpannableStringBuilder result=new SpannableStringBuilder();
        if(raw==null||raw.isEmpty())return result;
        String[] lines=normalize(raw).split("\n",-1);
        for(int i=0;i<lines.length;i++){
            String line=lines[i].trim();
            boolean quoted=line.startsWith(">");
            if(quoted)line=line.substring(1).trim(); // Old saves, no QuoteSpan.
            String plain=line.replace("**","").trim();
            boolean thought=plain.startsWith("[Мысли]"),event=plain.startsWith("[Событие]"),action=plain.startsWith("[Действие]");
            boolean spoken=quoted||plain.startsWith("— ")||plain.startsWith("- ");
            boolean speaker=!spoken&&!thought&&!event&&!action&&plain.endsWith(":")&&plain.length()>1
                &&plain.length()<62&&plain.indexOf('.')<0;
            boolean heroSpeaker=speaker&&plain.matches("(?iu)ты\\s*:")
                &&heroName!=null&&!heroName.trim().isEmpty();
            if(heroSpeaker){
                String safe=heroName.replace("*","").replace("\n"," ")
                    .replace("\r"," ").trim();
                if(safe.length()>48)safe=safe.substring(0,48);
                line=line.startsWith("**")?"**"+safe+":**":safe+":";
            }
            int start=result.length();
            appendRich(result,line);
            int end=result.length();
            if(i<lines.length-1){
                result.append("\n");
                // Always leave an additional breath after a spoken line,
                // unless the following line is already blank.
                if(spoken&&!lines[i+1].trim().isEmpty())result.append("\n");
            }
            if(end<=start)continue;
            if(thought||event||action){
                result.setSpan(new StyleSpan(thought?Typeface.ITALIC:Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(thought?0xffaab8ca:event?0xffdbc18b:accent),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(spoken){
                result.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(0xfff0f0ef),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(speaker){
                result.setSpan(new StyleSpan(Typeface.BOLD),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new ForegroundColorSpan(heroSpeaker?heroColor:accent),
                    start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                result.setSpan(new RelativeSizeSpan(1.03f),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else{
                // The authorial narrative always has italic styling, even if
                // AI omitted Markdown or this is a previously saved story.
                result.setSpan(new StyleSpan(Typeface.ITALIC),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return result;
    }
}
