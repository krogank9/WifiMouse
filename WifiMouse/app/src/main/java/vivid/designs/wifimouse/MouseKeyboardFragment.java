package vivid.designs.wifimouse;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import java.util.ArrayList;


public class MouseKeyboardFragment extends Fragment implements RemoteActivity.RemoteSettingsProvider {
    /*
     *
     * Fragment userFunctions:
     *
     */
    View touchpadView;
    View scrollAreaView;
    View left_mouse;
    View right_mouse;
    View physical_buttons_container;
    View touchpad_container;

    int darken = 0;
    int darken_less = 0;

    public MouseKeyboardFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_mouse_keyboard, container, false);
        touchpadView = inflated.findViewById(R.id.touchpad);
        scrollAreaView = inflated.findViewById(R.id.scroll_area);
        left_mouse = inflated.findViewById(R.id.left_mouse_button);
        right_mouse = inflated.findViewById(R.id.right_mouse_button);
        physical_buttons_container = inflated.findViewById(R.id.mouse_buttons_container);
        touchpad_container = inflated.findViewById(R.id.touchpad_container);

        darken = getResources().getColor(R.color.darken);
        darken_less = getResources().getColor(R.color.darken_less);

        touchpadView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) { return touchpadTouchEvent(event); }
        });

        final DisplayMetrics dm = getActivity().getApplicationContext().getResources().getDisplayMetrics();
        move_threshold = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, move_threshold_in, dm);
        Log.d("aaa", "move_threshold: "+move_threshold);

        return inflated;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    /*
     * Gesture detector for proper tap handling
     */

    class MyTapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }

    /*
     *
     * Mouse & Keyboard settings code
     *
     */

    SharedPreferences sharedPref;
    boolean prevent_rotation = false;
    boolean enable_lr_buttons = false;
    boolean invert_scrolling = false;
    boolean hide_scrollbar = false;
    double scroll_sensitivity = 10;
    double mouse_sensitivity = 1;

    private double scale(double min, double max, double scalar) {
        double range = max-min;
        return Math.abs(scalar*range) + min;
    }

    private double getSensitivity(double percent, double min, double middle, double max) {
        if(percent <= 50.0)
            return scale(min, middle, percent/50.0);
        else
            return scale(middle, max, (percent-50.0)/50.0);
    }

    public void loadSettings() {
        // sensitivity -- lower is more sensitive
        mouse_sensitivity = getSensitivity(100 - sharedPref.getInt("mouse_sensitivity", 50), 0.1, 1, 10);
        scroll_sensitivity = getSensitivity(100 - sharedPref.getInt("scroll_sensitivity", 50), 0.1, 14, 30);

        if(getActivity() != null) {
            prevent_rotation = sharedPref.getBoolean("prevent_rotation", false);
            if (prevent_rotation)
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            else
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            enable_lr_buttons = sharedPref.getBoolean("enable_lr_buttons", false);
            physical_buttons_container.setVisibility(enable_lr_buttons ? View.VISIBLE:View.GONE);

            invert_scrolling = sharedPref.getBoolean("invert_scrolling", true);
            hide_scrollbar = sharedPref.getBoolean("hide_scrollbar", false);
            scrollAreaView.setVisibility(hide_scrollbar ? View.GONE:View.VISIBLE);
        }
    }

    public void saveSettings() { saveSettings(-1, -1); }
    public void saveSettings(int mouse, int scroll) {
        SharedPreferences.Editor ed = sharedPref.edit();
        ed.putBoolean("prevent_rotation", prevent_rotation);
        ed.putBoolean("enable_lr_buttons", enable_lr_buttons);
        ed.putBoolean("invert_scrolling", invert_scrolling);
        ed.putBoolean("hide_scrollbar", hide_scrollbar);
        if(mouse >= 0)
            ed.putInt("mouse_sensitivity", mouse);
        if(scroll >= 0)
            ed.putInt("scroll_sensitivity", scroll);
        ed.commit();
    }

    @Override
    public void inflateSettingsView(ViewGroup root) {
        sharedPref = root.getContext().getSharedPreferences("cow.emoji.WifiMouse", Context.MODE_PRIVATE);
        LayoutInflater inflater = LayoutInflater.from(root.getContext());
        View v = inflater.inflate(R.layout.settings_mousekeyboard, root);

        SeekBar mouseSeeker = (SeekBar) v.findViewById(R.id.mouse_sensitivity);
        SeekBar scrollSeeker = (SeekBar) v.findViewById(R.id.scroll_sensitivity);
        CheckBox rotationCheck = (CheckBox) v.findViewById(R.id.prevent_rotation);
        CheckBox buttonsCheck = (CheckBox) v.findViewById(R.id.enable_lr_buttons);
        CheckBox invertScrollCheck = (CheckBox)  v.findViewById(R.id.invert_scroll);
        CheckBox hideScrollbarCheck = (CheckBox)  v.findViewById(R.id.hide_scrollbar);

        loadSettings();
        mouseSeeker.setProgress(sharedPref.getInt("mouse_sensitivity", 50));
        scrollSeeker.setProgress(sharedPref.getInt("scroll_sensitivity", 50));

        rotationCheck.setChecked(prevent_rotation);
        buttonsCheck.setChecked(enable_lr_buttons);
        invertScrollCheck.setChecked(invert_scrolling);
        hideScrollbarCheck.setChecked(hide_scrollbar);

        mouseSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                saveSettings(progress, -1);
                loadSettings();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        scrollSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                saveSettings(-1, progress);
                loadSettings();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        rotationCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prevent_rotation = isChecked;
                saveSettings(); loadSettings();
            }
        });
        buttonsCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                enable_lr_buttons = b;
                saveSettings(); loadSettings();
            }
        });
        invertScrollCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                invert_scrolling = b;
                saveSettings(); loadSettings();
            }
        });
        hideScrollbarCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                hide_scrollbar = b;
                saveSettings(); loadSettings();
            }
        });

        loadSettings();
    }


    /*
     * left and right virtual buttons
     */

    public boolean leftMouseTouchEvent(MotionEvent e) {
        switch(e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if(left_physical_down)
                    break;
            case MotionEvent.ACTION_DOWN:
                left_physical_down = true;
                left_mouse.setBackgroundColor(darken);
                WifiMouseApplication.networkConnection.sendMessage("MouseDown 1");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if(getPointersStartingInRect(left_rect).size() > 0)
                    break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                left_physical_down = false;
                left_mouse.setBackgroundColor(darken_less);
                WifiMouseApplication.networkConnection.sendMessage("MouseUp 1");
                break;
        }
        return true;
    }

    boolean left_physical_down = false;
    boolean right_physical_down = false;

    public boolean rightMouseTouchEvent(MotionEvent e) {
        switch(e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if(right_physical_down)
                    break;
            case MotionEvent.ACTION_DOWN:
                right_physical_down = true;
                right_mouse.setBackgroundColor(darken);
                WifiMouseApplication.networkConnection.sendMessage("MouseDown 3");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if(getPointersStartingInRect(right_rect).size() > 0)
                    break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                right_physical_down = false;
                right_mouse.setBackgroundColor(darken_less);
                WifiMouseApplication.networkConnection.sendMessage("MouseUp 3");
                break;
        }
        return true;
    }

    /*
     *
     * Touchpad code:
     * Click, move, scroll
     *
     */

    Rect scroll_rect = new Rect();
    Rect buttons_rect = new Rect();
    Rect left_rect = new Rect();
    Rect right_rect = new Rect();

    PointerElem curPointer = new PointerElem(0,0);
    PointerElem lastPointerMoved = new PointerElem(0,0);
    final int TAP_TIMEOUT = 300;
    long lastDown = 0;
    long now = 0;
    int downCount = 0;
    boolean touchMoved = false;
    int move_threshold = 5;
    float move_threshold_in = 0.02f;

    int tapId = 0;
    boolean left_down = false;
    boolean right_queued = false;

    private void handleTouchDown(MotionEvent e) {
        long elapsed = now - lastDown;
        float dist = avgPointer.dist(lastPointer);

        if(elapsed < TAP_TIMEOUT && dist < 77 && !touchMoved) {
            if(scroll_rect.contains((int)avgPointer.x, (int)avgPointer.y)
            && scroll_rect.contains((int)lastPointer.x, (int)lastPointer.y)) {
                right_queued = true;
            }
            else if(!scroll_rect.contains((int)lastPointer.x, (int)lastPointer.y)){
                WifiMouseApplication.networkConnection.sendMessage("MouseDown 1");
                left_down = true;
            }
        }

        lastDown = now;
        lastPointer.x = avgPointer.x;
        lastPointer.y = avgPointer.y;
    }

    private void handleTouchUp(MotionEvent e) {
        long elapsed = now - lastDown;
        boolean isMultiTouch = downCount > 1;
        boolean inScrollRect = scroll_rect.contains((int)lastPointer.x, (int)lastPointer.y);

        if(left_down) {
            left_down = false;
            WifiMouseApplication.networkConnection.sendMessage("MouseUp 1");
        }

        if(right_queued) {
            right_queued = false;
            WifiMouseApplication.networkConnection.sendMessage("MouseDown 3");
            WifiMouseApplication.networkConnection.sendMessage("MouseUp 3");
        }

        if(elapsed < TAP_TIMEOUT && !touchMoved && !inScrollRect) {
            final int button = isMultiTouch?2:1;
            final int myTapId = tapId;
            touchpadView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(myTapId != tapId)
                        return;
                    lastDown = 0; // prevent click again fast
                    WifiMouseApplication.networkConnection.sendMessage("MouseDown "+button);
                    WifiMouseApplication.networkConnection.sendMessage("MouseUp "+button);
                }
            }, 180);
        }
    }

    // Touch move: Scroll if in scroll bar or 2 fingers down, mouse move otherwise.

    private void handleTouchMove(MotionEvent e) {
        double min_move = mouse_sensitivity;

        boolean inScrollBar = avgPointer.x >= scrollAreaView.getX() && !hide_scrollbar;
        boolean scrolling = getTouchPointersCount() > 1 || inScrollBar;
        if(scrolling) min_move = scroll_sensitivity;

        double mouseMoveX = 0;
        double mouseMoveY = 0;
        if(Math.abs(avgPointer.x - avgPointer.prevX) >= min_move) {
            mouseMoveX = (avgPointer.x - avgPointer.prevX)/min_move;
            avgPointer.prevX = avgPointer.x;
        }
        if(Math.abs(avgPointer.y - avgPointer.prevY) >= min_move) {
            mouseMoveY = (avgPointer.y - avgPointer.prevY)/min_move;
            avgPointer.prevY = avgPointer.y;
        }

        if(Math.abs(mouseMoveX) > 0 || Math.abs(mouseMoveY) > 0) {
            if(scrolling)
                WifiMouseApplication.networkConnection.sendMessageMouseScroll((int)Math.round(mouseMoveY) * (invert_scrolling && !inScrollBar?-1:1));
            else
                WifiMouseApplication.networkConnection.sendMessageMouseMove((int)Math.round(mouseMoveX), (int)Math.round(mouseMoveY));
        }
    }

    class PointerElem {
        float x,y;
        float prevX, prevY;
        final float startX, startY;

        boolean updatePos(float x, float y) {
            float oldX = this.x;
            float oldY = this.y;
            this.x = x;
            this.y = y;
            return x != oldX || y != oldY;
        }
        float dist(PointerElem other) {
            float dX = x - other.x;
            float dY = y - other.y;
            return (float)Math.sqrt(dX*dX + dY*dY);
        }
        PointerElem(MotionEvent e) {
            x = prevX = startX = e.getX(e.getActionIndex());
            y = prevY = startY = e.getY(e.getActionIndex());
        }
        PointerElem(float x, float y) {
            this.x = prevX = startX = x;
            this.y = prevY = startY = y;
        }
    }
    SparseArray<PointerElem> pointers = new SparseArray<>();

    PointerElem lastPointer = new PointerElem(0,0);
    PointerElem avgPointer = new PointerElem(0,0);

    private void updatePointerList(MotionEvent e) {
        boolean remove;
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                remove = false;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                remove = true;
                break;
            case MotionEvent.ACTION_MOVE:
                for(int i=0; i<pointers.size(); i++) {
                    int id = pointers.keyAt(i);
                    int index = e.findPointerIndex(id);
                    PointerElem p = pointers.get(id);
                    if( p.updatePos(e.getX(index), e.getY(index)) )
                        curPointer = p;
                }
                PointerElem newAvg = avgPointers(getAllTouchPointers());
                avgPointer.x = newAvg.x;
                avgPointer.y = newAvg.y;
                return;
            default:
                return;
        }

        int pointerId = getPointerId(e);
        if(remove == false) {
            if(e.getPointerCount() == 1)
                pointers.clear();
            pointers.put(pointerId, new PointerElem(e));
        } else {
            pointers.remove(pointerId);
        }

        avgPointer = avgPointers(getAllTouchPointers());
    }

    private int getPointerId(MotionEvent e) {
        return e.getPointerId(e.getActionIndex());
    }

    private PointerElem getCurPointer(MotionEvent e) {
        return pointers.get(getPointerId(e));
    }

    private ArrayList<PointerElem> getPointersStartingInRect(Rect r) {
        ArrayList<PointerElem> ps = new ArrayList<>();
        for(int i=0; i<pointers.size(); i++) {
            PointerElem p = pointers.valueAt(i);
            if(r.contains((int)p.startX, (int)p.startY))
                ps.add(p);
        }
        return ps;
    }

    private ArrayList<PointerElem> getPointersNotStartingInRect(Rect r) {
        ArrayList<PointerElem> ps = new ArrayList<>();
        for(int i=0; i<pointers.size(); i++) {
            PointerElem p = pointers.valueAt(i);
            if(!r.contains((int)p.startX, (int)p.startY))
                ps.add(p);
        }
        return ps;
    }

    private ArrayList<PointerElem> getAllTouchPointers() {
        return getPointersNotStartingInRect(buttons_rect);
    }
    private int getTouchPointersCount() {
        return getAllTouchPointers().size();
    }

    private PointerElem avgPointers(ArrayList<PointerElem> ps) {
        if(ps.size() == 0)
            return new PointerElem(0,0);
        float x = 0;
        float y = 0;
        for(int i=0; i<ps.size(); i++) {
            x += ps.get(i).x;
            y += ps.get(i).y;
        }
        return new PointerElem(x/ps.size(), y/ps.size());
    }

    public boolean touchpadTouchEvent(MotionEvent e) {
        curPointer = getCurPointer(e);
        updatePointerList(e);
        if(curPointer == null)
            curPointer = getCurPointer(e);

        physical_buttons_container.getHitRect(buttons_rect);
        if(physical_buttons_container.getVisibility() == View.GONE)
            buttons_rect.set(0,0,0,0);
        left_rect.set(0, buttons_rect.top, buttons_rect.right/2, buttons_rect.bottom);
        right_rect.set(buttons_rect.right/2, buttons_rect.top, buttons_rect.right, buttons_rect.bottom);
        scrollAreaView.getHitRect(scroll_rect);
        if(hide_scrollbar)
            scroll_rect.set(0,0,0,0);

        if(buttons_rect.contains((int)curPointer.startX, (int)curPointer.startY)) {
            if(left_rect.contains((int)curPointer.startX, (int)curPointer.startY))
                leftMouseTouchEvent(e);
            else
                rightMouseTouchEvent(e);
            return true;
        }

        now = System.currentTimeMillis();

        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                downCount = getTouchPointersCount();
                tapId++;
                break;
            case MotionEvent.ACTION_DOWN:
                tapId++;
                downCount = getTouchPointersCount();
                if(!left_physical_down && !right_physical_down)
                    handleTouchDown(e);
                touchMoved = false;
                break;
            case MotionEvent.ACTION_UP:
                if(!left_physical_down && !right_physical_down)
                    handleTouchUp(e);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!touchMoved
                        && Math.abs(curPointer.x - curPointer.startX) < move_threshold
                        && Math.abs(curPointer.y - curPointer.startY) < move_threshold)
                {
                    break;
                }
                touchMoved = true;
                handleTouchMove(e);
                break;
        }
        return true;
    }
}
