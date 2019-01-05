package vivid.designs.wifimouse;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;


public class MyEditText extends AppCompatEditText {
    public MyEditText(Context c){super(c);}
    public MyEditText(Context c, AttributeSet a){super(c, a);}
    public MyEditText(Context c, AttributeSet a, int d){super(c, a, d);}

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        setSelection(this.length());
    }
}
