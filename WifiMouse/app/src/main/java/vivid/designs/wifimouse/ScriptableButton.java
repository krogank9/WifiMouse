package vivid.designs.wifimouse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Hashtable;

public class ScriptableButton extends RelativeLayout implements RemoteScript.ScriptableView {
    RemoteScript scriptObj;
    Hashtable<String, String> props;
    public void updateProps() {
        if(props.containsKey("text") && props.get("text").length() > 0) {
            scriptObj.evalFunctions(props.get("text"), new RemoteScript.FunctionCallback() {
                @Override
                public void functionCallback(String result) {
                    setButtonText(result);
                }
            });
        }
        if(props.containsKey("icon") && props.get("icon").length() > 0) {
            scriptObj.evalFunctions(props.get("icon"), new RemoteScript.FunctionCallback() {
                @Override
                public void functionCallback(String result) {
                    if(result != null && result.startsWith("@drawable/") == false)
                        result = "@drawable/"+result;
                    setIconDrawable(result);
                }
            });
        }
    }

    private TextView buttonText;
    private ImageView buttonIcon;
    private View buttonIconContainer;
    private View buttonBackground;
    private View buttonHighlight;

    int marginDp = 3;
    boolean needsConfirm;

    static int lastMargin = 3;
    static String lastFgColor = "#fff";
    static String lastBgColor = "#000";
    public ScriptableButton(Context c, Hashtable<String, String> hashtable, RemoteScript scriptObj) {
        super(c);
        this.props = hashtable;
        this.scriptObj = scriptObj;
        boolean needsConfirm = false;
        if(hashtable.containsKey("bg"))
            lastBgColor = hashtable.get("bg");
        if(hashtable.containsKey("fg"))
            lastFgColor = hashtable.get("fg");
        if(hashtable.containsKey("confirm"))
            needsConfirm = Boolean.parseBoolean(hashtable.get("confirm"));
        if(hashtable.containsKey("margin"))
            try {
                lastMargin = Integer.parseInt(hashtable.get("margin"));
            } catch (NumberFormatException e) {}

        marginDp = lastMargin;

        hashtable.put("onup", denull(hashtable.get("onup")));
        hashtable.put("ondown", denull(hashtable.get("ondown")));
        hashtable.put("onclick", denull(hashtable.get("onclick")));

        init(c, needsConfirm, lastFgColor, lastBgColor);
    }

    public String denull(String str) {
        return str == null ? "" : str;
    }

    private void init(Context c, boolean needsConfirm,
                      String textColor, String backgroundColor) {

        inflate(getContext(), R.layout.icontextbutton, this);
        buttonText = (TextView) findViewById(R.id.button_text);
        buttonIcon = (ImageView) findViewById(R.id.button_icon);
        buttonIconContainer = findViewById(R.id.button_icon_container);
        buttonBackground = findViewById(R.id.button_background);
        buttonHighlight = findViewById(R.id.button_pressed_highlight);

        this.needsConfirm = needsConfirm;

        setButtonTextColor(makeColor(textColor, "#fff"));
        setButtonBackgroundColor(makeColor(backgroundColor, "#000"));

        setButtonText( null );
        setIconDrawable( null );
        updateProps();
    }

    public void setIconDrawable(String drawable) {
        if(drawable == null) {
            buttonIconContainer.setVisibility(GONE);
        }
        else {
            int drawableInt = getResources().getIdentifier(drawable, "drawable", getContext().getPackageName());
            buttonIcon.setImageResource(drawableInt);
            buttonIconContainer.setVisibility(VISIBLE);
        }
    }

    public void setButtonText(String text) {
        if(text != null)
            buttonText.setText(text);

        buttonText.setVisibility(text == null || text.equals("")? GONE:VISIBLE);
    }

    public void setButtonTextColor(int color) {
        buttonText.setTextColor(color);
        buttonIcon.setColorFilter(color);
    }

    public String expandColorStr(String color) {
        if(color.length() == 4 && color.startsWith("#")) {
            return "#" + color.charAt(1) + color.charAt(1)
                                  + color.charAt(2) + color.charAt(2)
                                  + color.charAt(3) + color.charAt(3);
        }
        else
            return color;
    }

    public int makeColor(String color) { return  makeColor(color, "#fff"); }
    public int makeColor(String color, String defaultColor) {
        if(color == null)
            color = defaultColor;
        color = expandColorStr(color);
        return Color.parseColor(color);
    }

    public void setButtonBackgroundColor(int color) {
        buttonBackground.setBackgroundColor(color);
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        MarginLayoutParams margins = MarginLayoutParams.class.cast(getLayoutParams());
        int dp = marginDp;
        final float scale = getContext().getResources().getDisplayMetrics().density;
        int margin = (int) (dp * scale + 0.5f);
        margins.topMargin = margin;
        margins.bottomMargin = margin;
        margins.leftMargin = margin;
        margins.rightMargin = margin;
        setLayoutParams(margins);
    }

    private void buttonUp() {
        scriptObj.evalFunctions(props.get("onup"), null);
    }
    private void buttonDown() {
        scriptObj.evalFunctions(props.get("ondown"), null);
    }
    private void buttonClicked() {
        if(needsConfirm) {
            Dialog confirmPressDialog = new AlertDialog.Builder(getContext())
                    .setTitle(buttonText.getText().toString() + "?")
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    scriptObj.evalFunctions(props.get("onclick"), null);
                                }
                            }
                    )
                    .setNegativeButton("No", null)
                    .create();
            confirmPressDialog.show();
        }
        else {
            scriptObj.evalFunctions(props.get("onclick"), null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                buttonHighlight.setVisibility(VISIBLE);
                buttonDown();
                break;
            case MotionEvent.ACTION_UP:
                // don't trigger press if finger moved off button
                if(ev.getX() >= 0 && ev.getY() >= 0 && ev.getX()<getMeasuredWidth() && ev.getY() < getMeasuredHeight())
                    buttonClicked();
            case MotionEvent.ACTION_CANCEL:
                buttonUp();
                buttonHighlight.setVisibility(INVISIBLE);
                break;
        }
        return true;
    }
}
