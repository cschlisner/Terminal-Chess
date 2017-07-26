package net.schlisner.terminalchess;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by coles on 7/26/2017.
 */

public class StyledTextView extends android.support.v7.widget.AppCompatTextView {
    public StyledTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(FontManager.getTypeFace());
    }

    public StyledTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setTypeface(FontManager.getTypeFace());
    }

    public StyledTextView(Context context) {
        super(context);
        this.setTypeface(FontManager.getTypeFace());
    }
}