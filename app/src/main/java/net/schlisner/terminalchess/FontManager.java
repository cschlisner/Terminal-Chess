package net.schlisner.terminalchess;

import android.content.Context;
import android.graphics.Typeface;

import java.lang.reflect.Type;

/**
 * Created by coles on 7/26/2017.
 */

class FontManager {
    static Typeface tf = null;

    static void setFont(Context context, String fontname){
        tf = Typeface.createFromAsset(context.getAssets(), "fonts/"+fontname);
    }

    static Typeface getTypeFace(){
        return tf;
    }
}
