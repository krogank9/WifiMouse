package vivid.designs.wifimouse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
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

import java.util.ArrayList;

public class IconTextButton extends RelativeLayout {
    private TextView buttonText;
    private ImageView buttonIcon;
    private View buttonIconContainer;
    private View buttonBackground;
    private View buttonHighlight;

    public boolean newLine = false;
    int marginDp = 1;
    boolean needsConfirm;
    String sendMessageText;

    public IconTextButton(Context c) {super(c); init(c,null);}
    public IconTextButton(Context c, AttributeSet a) {super(c,a); init(c,a);}
    public IconTextButton(Context c, AttributeSet a, int s) {super(c,a,s); init(c,a);}
    public IconTextButton(Context c, String buttonTitle, String sendMessageText, boolean needsConfirm,
                          String textColor, String backgroundColor, String iconDrawable, int marginDp) {
        super(c);
        if(iconDrawable.startsWith("@drawable/") == false)
            iconDrawable = "@drawable/"+iconDrawable;
        this.marginDp = marginDp;
        init(c, buttonTitle, sendMessageText, needsConfirm, textColor, backgroundColor, iconDrawable);
    }

    public void setIconDrawable(String drawable) {
        if(drawable == null)
            setIconDrawable(0);
        else
            setIconDrawable( getResources().getIdentifier(drawable, "drawable", getContext().getPackageName()) );
    }
    public void setIconDrawable(int drawable) {
        if(drawable != 0) {
            buttonIcon.setImageResource(drawable);
        }
        buttonIconContainer.setVisibility(drawable == 0? GONE:VISIBLE);
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

    // init() wrapper to get values out of AttributeSet
    private void init(Context c, AttributeSet a) {
        String buttonTitle = a.getAttributeValue(null, "text");
        String iconDrawable = a.getAttributeValue(null, "icon");

        String textColor = a.getAttributeValue(null, "textColor");
        String backgroundColor = a.getAttributeValue(null, "bgColor");

        marginDp = a.getAttributeIntValue(null, "marginDp", 3);

        boolean needsConfirm = a.getAttributeBooleanValue(null, "needsConfirm", false);
        String sendMessageText = a.getAttributeValue(null, "sendMessageText");

        init(c, buttonTitle, sendMessageText, needsConfirm, textColor, backgroundColor, iconDrawable);
    }

    private void init(Context c, String buttonTitle, String sendMessageText, boolean needsConfirm,
                      String textColor, String backgroundColor, String iconDrawable) {
        inflate(getContext(), R.layout.icontextbutton, this);
        buttonText = (TextView) findViewById(R.id.button_text);
        buttonIcon = (ImageView) findViewById(R.id.button_icon);
        buttonIconContainer = findViewById(R.id.button_icon_container);
        buttonBackground = findViewById(R.id.button_background);
        buttonHighlight = findViewById(R.id.button_pressed_highlight);

        setButtonText( buttonTitle );
        this.needsConfirm = needsConfirm;
        this.sendMessageText = sendMessageText;

        setButtonTextColor(makeColor(textColor, "#fff"));
        setButtonBackgroundColor(makeColor(backgroundColor, "#000"));

        setIconDrawable(iconDrawable);
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

    private static int nonEscapedIndexOf(char index, String in) {
        for(int i=0; i<in.length(); i++)
            if((i == 0 || in.charAt(i-1) != '\\') && in.charAt(i) == index)
                return i;
        return -1;
    }

    private static String[] splitNonEscaped(String regex, @NonNull String toSplit) {
        String[] split = toSplit.split(regex);
        for(int i=0; i<(split.length-1); i++) {
            if(split[i].endsWith("\\")) {
                // if was split at an escaped character, remake split array
                // with the two neighboring strings joined
                String[] copy = new String[split.length - 1];
                for(int j=0; j<copy.length; j++) {
                    if(j < i)
                        copy[j] = split[j];
                    else if(j == i)
                        copy[j] = split[j] + split[j+1];
                    else if(j > i)
                        copy[j] = split[j+1];
                }
                split = copy;
                i = -1;
            }
        }
        return split;
    }

    private void sendMessage(String messageText) {
        String[] messages = splitNonEscaped(";", messageText);
        for(int i=0; i<messages.length; i++) {
            messages[i] = messages[i].replace("\\;", ";").replace("\\<","<").replace("\\>",">");
            WifiMouseApplication.networkConnection.sendMessage(messages[i]);
        }
    }
    private void buttonPressed() { buttonPressed(sendMessageText); }
    private void buttonPressed(final String sendMessageText) {
        if(sendMessageText == null)
            return;

        // Prompts to be evaluated denoted by "< >", but can be escaped with backslash for a regular "<"
        if(nonEscapedIndexOf('<', sendMessageText) >= 0 && nonEscapedIndexOf('>', sendMessageText) >= 0) {
            int start = nonEscapedIndexOf('<', sendMessageText);
            int end = nonEscapedIndexOf('>', sendMessageText);

            final String pre = sendMessageText.substring(0, start);
            String prompt = sendMessageText.substring(start+1, end);
            final String post = end+1 >= sendMessageText.length() ? "":sendMessageText.substring(end+1);

            int typeEnd = prompt.indexOf(" ");
            String promptType = prompt.substring(0, typeEnd);
            String promptTitle = typeEnd+1 >= prompt.length() ? "":prompt.substring(typeEnd+1);
            final EditText promptEditText = new EditText(getContext());
            if(promptType.equals("NumPrompt"))
                promptEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            else if(promptType.equals("TextPrompt"))
                promptTitle = prompt;
            else
                return;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            LinearLayout editTextContainer = new LinearLayout(getContext());
            editTextContainer.setLayoutParams(params);
            promptEditText.setLayoutParams(params);

            int pad_top = getContext().getResources().getDimensionPixelSize(R.dimen.prompt_padding_top);
            int pad_side = getContext().getResources().getDimensionPixelSize(R.dimen.prompt_padding_side);
            editTextContainer.setPadding(pad_side, pad_top, pad_side, 0);
            editTextContainer.addView(promptEditText);
            Dialog promptDialog = new AlertDialog.Builder(getContext())
                    .setTitle(promptTitle)
                    .setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String mid = promptEditText.getText().toString().replace(";","\\;").replace("<","\\<").replace(">","\\>");
                                    if(mid.length() > 0) {
                                        String newMessageText = pre + mid + post;
                                        buttonPressed(newMessageText);
                                    }
                                }
                            }
                    )
                    .setView(editTextContainer)
                    .setNegativeButton("Cancel", null)
                    .create();
            promptDialog.show();
            promptEditText.post(new Runnable() {
                @Override
                public void run() {
                    promptEditText.setFocusableInTouchMode(true);
                    promptEditText.requestFocus();
                    final InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(promptEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
        else if(!needsConfirm) {
            sendMessage(sendMessageText);
        }
        else {
            Dialog confirmPressDialog = new AlertDialog.Builder(getContext())
                    .setTitle(buttonText.getText().toString()+"?")
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    sendMessage(sendMessageText);
                                }
                            }
                    )
                    .setNegativeButton("No", null)
                    .create();
            confirmPressDialog.show();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                buttonHighlight.setVisibility(VISIBLE);
                break;
            case MotionEvent.ACTION_UP:
                // don't trigger press if finger moved off button
                if(ev.getX() >= 0 && ev.getY() >= 0 && ev.getX()<getMeasuredWidth() && ev.getY() < getMeasuredHeight())
                    buttonPressed();
            case MotionEvent.ACTION_CANCEL:
                buttonHighlight.setVisibility(INVISIBLE);
                break;
        }
        return true;
    }
}
