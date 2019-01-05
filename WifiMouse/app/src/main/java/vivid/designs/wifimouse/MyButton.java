package vivid.designs.wifimouse;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;

import java.lang.reflect.Method;

import static vivid.designs.wifimouse.R.color.darken_less;

public class MyButton extends AppCompatButton {
    public MyButton(Context c) {super(c); init(c,null);}
    public MyButton(Context c, AttributeSet a) {super(c,a); init(c,a);}
    public MyButton(Context c, AttributeSet a, int s) {super(c,a,s); init(c,a);}

    private int drawablePadding = 0;
    private CharSequence initalText = "";
    @Override
    public CharSequence getText() {
        if(drawable != null)
            return initalText; // text set to "" with drawable to prevent scaling button
        else
            return super.getText();
    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    Activity parentActivity = null;
    private Drawable drawable = null;
    int selectableBg = 0;
    boolean enable_sticky = false;

    private void init(Context c, AttributeSet a) {
        parentActivity = getActivity();

        initalText = this.getText();

        TypedValue outValue = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        selectableBg = outValue.resourceId;
        this.setBackgroundResource(selectableBg);

        String enable_sticky_str = a.getAttributeValue(null, "enable_sticky");
        if(enable_sticky_str != null)
            enable_sticky = Boolean.parseBoolean(enable_sticky_str);
        String onChangeMethod = a.getAttributeValue(null, "onChange");
        if(onChangeMethod == null)
            onChangeMethod = "myButtonPressed";
        try {
            onChange = parentActivity.getClass().getMethod(onChangeMethod, MyButton.class,Boolean.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String drawableName = a.getAttributeValue(null, "drawable");
        if(drawableName != null) {
            int id = c.getResources().getIdentifier(drawableName, "drawable", c.getPackageName());
            drawable = getResources().getDrawable(id);
            if(drawable != null)
                this.setText("");// hide text when using a drawable instead
        }

        drawablePadding = a.getAttributeIntValue(null, "drawablePadding", 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(drawable == null) {
            super.onDraw(canvas);
            return;
        }

        int bWidth = canvas.getWidth()-drawablePadding;
        int bHeight = canvas.getHeight()-drawablePadding;
        float dAr = (float)drawable.getIntrinsicWidth()/(float)drawable.getIntrinsicHeight();
        float cAr = (float)canvas.getWidth()/(float)canvas.getHeight();
        if(cAr < dAr) // if canvas is less wide than drawable
            bHeight *= cAr/dAr; // decrease drawable height
        else if(dAr < cAr) // if canvas is less tall than drawable
            bWidth *= dAr/cAr; // decrease drawable width

        canvas.save();
        canvas.translate((canvas.getWidth() - bWidth)/2, (canvas.getHeight() - bHeight)/2);
        drawable.setBounds(0,0,bWidth,bHeight);
        drawable.draw(canvas);
        canvas.restore();
    }

    public void forceUnstick() {
        if(isDown && sticky_down) {
            buttonUp();
            sticky_down = false;
            setBackgroundColor(Color.TRANSPARENT);
            setBackgroundResource(selectableBg);
        }
    }

    Method onChange;

    boolean isDown = false;
    private void buttonDown() {
        isDown = true;

        if(onChange == null)
            return;
        try {
            onChange.invoke(parentActivity, this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void buttonUp() {
        isDown = false;

        if(onChange == null)
            return;
        try {
            onChange.invoke(parentActivity, this, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    long down_start = 0;
    boolean sticky_down = false;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                down_start = System.currentTimeMillis();
                sticky_down = false;
                if(!isDown) {
                    buttonDown();
                    final long myDownStart = down_start;
                    if(enable_sticky) {
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(isDown && myDownStart == down_start) {
                                    sticky_down = true;
                                    setBackgroundColor(getResources().getColor(darken_less));
                                }
                            }
                        }, 1000);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!sticky_down) {
                    setBackgroundColor(Color.TRANSPARENT);
                    setBackgroundResource(selectableBg);
                    buttonUp();
                }
                break;
        }
        return true;
    }
}
