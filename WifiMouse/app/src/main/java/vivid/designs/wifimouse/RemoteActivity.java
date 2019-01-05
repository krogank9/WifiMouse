package vivid.designs.wifimouse;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class RemoteActivity extends AppCompatActivity implements NavigationListFragment.NavigationDrawerToggler {

    public interface RemoteSettingsProvider {
        void inflateSettingsView(ViewGroup root);
    }

    View specialKeys;
    View specialKeysSlideout;
    View slideoutShadow;
    View editTextContainer;
    boolean keyboardShown = false;

    DrawerLayout drawerLayout;
    View leftDrawer;
    View rightDrawer;

    long lastKeyboardButtonPress = 0;
    int lastRootViewHeightDiff = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        // Lock drawer when closed to prevent opening when moving mouse
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        leftDrawer = findViewById(R.id.left_drawer);
        rightDrawer = findViewById(R.id.right_drawer);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerLayout.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if( keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK )
                    drawerLayout.closeDrawers();
                return false;
            }
        });

        findViewById(R.id.navigation_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationDrawer();
            }
        });

        findViewById(R.id.open_settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRightDrawer();
            }
        });

        findViewById(R.id.toggle_media_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleMediaButton();
            }
        });
        findViewById(R.id.toggle_keyboard_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                long elapsed = now - lastKeyboardButtonPress;
                lastKeyboardButtonPress = now;
                if(elapsed < 200) // prevent pressing twice by accident
                    return;
                if(!keyboardShown) {
                    keyboardShown = true;
                    keyboardStateChanged();
                    toggleSoftKeyboard(); // make sure to call this last, editText clearing closes it
                }
            }
        });
        findViewById(R.id.toggle_mouse_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTouchpadButton();
            }
        });

        editText = (MyEditText) findViewById(R.id.text_input);
        editText.addTextChangedListener(editTextWatcher);
        final View rootView = findViewById(R.id.drawer_layout);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // return if just pressed keyboard button
                long timeSinceKeyboardOpened = System.currentTimeMillis() - lastKeyboardButtonPress;

                Rect visibleRect = new Rect();
                rootView.getWindowVisibleDisplayFrame(visibleRect);
                // if none of the root view is hidden, keyboard has been closed
                int heightDiff = rootView.getHeight() - visibleRect.height();
                lastRootViewHeightDiff = heightDiff;

                // sometimes view observer updates with 0 heightDiff just as you press the keyboard button
                // and causes specialkeys to be closed. make sure keyboard has been open for >200ms
                // before closing the special keys
                if(keyboardShown && heightDiff == 0) {
                    // keyboard definitely had time to slide out before closing. close special keys
                    if(timeSinceKeyboardOpened > 500) {
                        keyboardShown = false;
                        keyboardStateChanged();
                    }
                    // sometimes observer returns 0 diff right before keyboard slides out.
                    // if there is any ambiguity, wait a few hundred MS before trying close animation
                    else {
                        rootView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(keyboardShown && lastRootViewHeightDiff == 0) {
                                    keyboardShown = false;
                                    keyboardStateChanged();
                                }
                            }
                        }, 500 - timeSinceKeyboardOpened);
                    }
                }
            }
        });

        editTextContainer = findViewById(R.id.edittext_container);
        specialKeys = findViewById(R.id.special_keys);
        specialKeysSlideout = findViewById(R.id.keyboard_top_slideout);
        slideoutShadow = findViewById(R.id.keyboard_slideout_shadow);
    }

    boolean touchpadLocked = true;
    boolean touchpadEnabled = true;
    boolean mediaButtonsEnabled = false;

    @Override
    protected void onResume() {
        super.onResume();
        WifiMouseApplication.isAppOpen = true;
        startService(new Intent(this, NetworkService.class));
        keyboardShown = false;

        Intent startIntent = getIntent();
        this.setIntent(null); // Don't consume twice
        if(startIntent != null) {
            Bundle extras = startIntent.getExtras();
            if(extras != null) {
                String remoteName = startIntent.getExtras().getString("RemoteName", null);
                Object[] remote = RemoteListActivity.getRemoteByName(this, remoteName);
                setRemote(remote);
            }
        }

        ((MyButton)findViewById(R.id.special_win_key)).setText(
                WifiMouseApplication.networkConnection.serverOs.equals("mac")? "Cmd" : "Win"
        );
        ((MyButton)findViewById(R.id.special_menu_key)).setText(
                WifiMouseApplication.networkConnection.serverOs.equals("mac")? "Opt" : "Menu"
        );
    }

    // Enables changing remotes without destroying activity
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WifiMouseApplication.isAppOpen = false;

        InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        if(keyboardShown) { // hide keyboard on pause
            keyboardShown = false;
            keyboardStateChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this,NetworkService.class));
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        stopService(new Intent(this,NetworkService.class));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        // Don't trigger volume twice for down & up
        if(event.getAction() != KeyEvent.ACTION_DOWN)
            return super.dispatchKeyEvent(event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(scriptObj != null && scriptObj.userFunctions.containsKey("VolumeButtonUp"))
                    scriptObj.evalFunctions("<VolumeButtonUp>", null);
                else
                    WifiMouseApplication.networkConnection.sendMessage("SpecialKey Tap VolumeUp");
                // consume keyEvent
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(scriptObj != null && scriptObj.userFunctions.containsKey("VolumeButtonDown"))
                    scriptObj.evalFunctions("<VolumeButtonDown>", null);
                else
                    WifiMouseApplication.networkConnection.sendMessage("SpecialKey Tap VolumeDown");
                // consume keyEvent
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {

        if(keyEvent.getAction() == KeyEvent.ACTION_UP) {
            if(keyCode == KeyEvent.KEYCODE_DEL && editText.getText().length() == 0) {
                WifiMouseApplication.networkConnection.sendMessage("Backspace 1");
            }
        }

        return super.onKeyUp(keyCode, keyEvent);
    }

    public void initSettingsMenu(Fragment remoteFragment) {
        FrameLayout settingsContainer = (FrameLayout) findViewById(R.id.remote_settings_container);
        settingsContainer.removeAllViewsInLayout();
        if(remoteFragment instanceof RemoteSettingsProvider) {
            RemoteSettingsProvider remoteSettings = (RemoteSettingsProvider) remoteFragment;
            remoteSettings.inflateSettingsView(settingsContainer);
            findViewById(R.id.open_settings_button).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.open_settings_button).setVisibility(View.GONE);
        }
    }

    RemoteScript scriptObj = null;
    Fragment curRemoteFragment = null;
    public void initRemoteView(Object remote) {
        if(remote == null) {
            // for the mouse and keyboard remote, lock the touchpad to show always
            touchpadLocked = true;
            touchpadEnabled = true;
        }
        else {
            touchpadLocked = false;
            touchpadEnabled = false;
        }
        updateTouchpadState();

        FrameLayout remoteContainer = (FrameLayout) findViewById(R.id.remote_container);
        remoteContainer.removeAllViewsInLayout();

        if(remote instanceof String) {
            scriptObj = new RemoteScript(this, (String) remote);
            remoteContainer.addView(scriptObj.makeLayout());
            initSettingsMenu(null);
            
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
        else if(remote instanceof Integer) {
            LayoutInflater inflater = LayoutInflater.from(this);
            int layout_xml_file = (Integer) remote;

            inflater.inflate(layout_xml_file, remoteContainer);

            initSettingsMenu(null);
        }
        else if(remote instanceof Class) {
            try {
                Class<Fragment> fragmentClass = (Class<Fragment>) remote;
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                if(curRemoteFragment != null) {
                    fragmentTransaction.remove(curRemoteFragment).commit();
                }
                curRemoteFragment = fragmentClass.newInstance();
                fragmentTransaction.add(remoteContainer.getId(), curRemoteFragment);
                fragmentTransaction.commit();

                initSettingsMenu(curRemoteFragment);
            }
            catch (Exception e) {}
        }
    }

    public void setRemote(Object[] remote) {
        if(remote == null)
            return;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // Mouse & Keyboard remote
        if(remote[1] == null)
            initSettingsMenu( getSupportFragmentManager().findFragmentById(R.id.touchpad_fragment) );

        TextView settingsTitle = (TextView) findViewById(R.id.remote_settings_title);
        settingsTitle.setText(remote[0] + " Settings");

        // Set up/inflate remote view
        TextView remoteTitle = (TextView) findViewById(R.id.toolbar_title);
        remoteTitle.setText((String)remote[0]);

        initRemoteView(remote[1]);
    }

    /* --------------------
        An EditText is kept in sync with the text sent to the WifiMouse server to allow for
        text suggestions, autocomplete, and Swype keyboard use
       -------------------- */
    MyEditText editText;
    boolean textCleared = false;
    public void clearEditText() {
        textCleared = true;
        editText.setText("");
    }
    private TextWatcher editTextWatcher = new TextWatcher() {
        String prevText = "";

        private int stringSimilarity(String oldStr, String newStr) {
            int i = 0;
            int minLen = Math.min(oldStr.length(), newStr.length());
            while(i<minLen) {
                if( oldStr.charAt(i) != newStr.charAt(i) )
                    break;
                i++;
            }
            return i;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean hide = editText.length() == 0;
            editTextContainer.setVisibility(hide? View.GONE:View.VISIBLE);
            editText.requestFocus(); // gets unfocused when setting its container GONE.........................................

            if(textCleared) {
                textCleared = false;
                prevText = "";
                return;
            }

            String str = s.toString();

            // Find out how many characters have to be backspaced:
            // Compare to see how many characters are different on the end
            int similarity = stringSimilarity(str, prevText);
            int numBackspaces = prevText.length() - similarity;
            if(numBackspaces > 0)
                WifiMouseApplication.networkConnection.sendMessage("Backspace " + numBackspaces);

            // After backspacing, retype the end of the string that was changed.
            String trimmed = str.substring(similarity);
            if(trimmed.length() > 0)
                WifiMouseApplication.networkConnection.sendMessage("TypeString " + trimmed);

            prevText = str;
            if(str.contains("\n") /*|| specialKeyIsDown()*/) {
                clearEditText();
            }
            //tryUnstickSpecialKeys();
        }
        @Override
        public void afterTextChanged(Editable s) {}
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    };

    private void keyboardStateChanged() {
        clearEditText();
        final View slideout = specialKeysSlideout;
        if(keyboardShown && mediaButtonsEnabled) {
            toggleMediaButton();
        }

        if(keyboardShown) {
            if(slideout.getVisibility() == View.INVISIBLE) {
                slideout.setClickable(true);
                slideout.setVisibility(View.VISIBLE);
            }

            specialKeys.setVisibility(View.VISIBLE);
            slideoutShadow.setVisibility(View.VISIBLE);
            slideout.setTranslationY(-slideout.getHeight());
            slideout.animate().setDuration(200).translationY(0).setInterpolator(new LinearInterpolator()).start();
        }
        else { //disabled
            slideout.setTranslationY(0);
            slideout.animate().setDuration(200).translationY(-slideout.getHeight()).setInterpolator(new LinearInterpolator()).withEndAction(new Runnable() {
                @Override
                public void run() {
                    specialKeys.setVisibility(View.INVISIBLE);
                    slideoutShadow.setVisibility(View.INVISIBLE);
                }
            }).start();

            ((MyButton)findViewById(R.id.special_win_key)).forceUnstick();
            ((MyButton)findViewById(R.id.special_alt_key)).forceUnstick();
            ((MyButton)findViewById(R.id.special_ctrl_key)).forceUnstick();
            ((MyButton)findViewById(R.id.special_menu_key)).forceUnstick();
            ((MyButton)findViewById(R.id.special_shift_key)).forceUnstick();
        }
    }

    private void toggleSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(getApplicationContext().INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
    }

    public void updateTouchpadState() {
        View touchpadFragment = findViewById(R.id.touchpad_fragment);
        touchpadFragment.setVisibility(touchpadEnabled? View.VISIBLE:View.GONE);
        findViewById(R.id.touchpad_mouse_icon).setBackgroundResource(touchpadEnabled ? R.drawable.ic_mouse_selected:R.drawable.ic_mouse);
    }

    public void toggleMediaButton() {
        if(keyboardShown && !mediaButtonsEnabled)
            return; // prevent pressing at same time

        mediaButtonsEnabled = !mediaButtonsEnabled;
        if(mediaButtonsEnabled) {
            specialKeys.setVisibility(View.INVISIBLE);
            slideoutShadow.setVisibility(View.INVISIBLE);
            findViewById(R.id.keyboard_top_slideout).setClickable(false);
        }

        final View slideout = findViewById(R.id.media_buttons_slideout);
        if(mediaButtonsEnabled) {
            slideout.setTranslationY(-slideout.getHeight());
            slideout.setVisibility(View.VISIBLE);
            slideout.setClickable(true);
            slideout.animate().setDuration(200).translationY(0).setInterpolator(new LinearInterpolator()).start();
        }
        else { //disabled
            slideout.setTranslationY(0);
            slideout.animate().setDuration(200).translationY(-slideout.getHeight()).setInterpolator(new LinearInterpolator()).withEndAction(new Runnable() {
                @Override
                public void run() {
                    slideout.setVisibility(View.INVISIBLE);
                    slideout.setClickable(false);
                }
            }).start();
        }
        findViewById(R.id.toggle_media_icon).setBackgroundResource(mediaButtonsEnabled? R.drawable.ic_media_selected:R.drawable.ic_media);
    }

    public void toggleTouchpadButton() {
        if(touchpadLocked)
            return;

        touchpadEnabled = !touchpadEnabled;

        updateTouchpadState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(leftDrawer) || drawerLayout.isDrawerOpen(rightDrawer)) {
            drawerLayout.closeDrawer(leftDrawer);
            drawerLayout.closeDrawer(rightDrawer);
        }
        else if(mediaButtonsEnabled) {
            toggleMediaButton();
        }
        else if(touchpadEnabled && !touchpadLocked) {
            touchpadEnabled = false;
            updateTouchpadState();
        }
        else
            super.onBackPressed();
    }

    int specialKeysPage = 0;
    private void onKeysPageChanged() {
        MyButton specialKeysToggler = (MyButton) findViewById(R.id.special_keys_toggler);
        if(specialKeysPage == 0) {
            specialKeysToggler.setText("...");
            findViewById(R.id.special_keys_pg1).setVisibility(View.GONE);
            findViewById(R.id.special_keys_pg2).setVisibility(View.GONE);
        }
        else if(specialKeysPage == 1) {
            specialKeysToggler.setText("1/2");
            findViewById(R.id.special_keys_pg1).setVisibility(View.VISIBLE);
            findViewById(R.id.special_keys_pg2).setVisibility(View.GONE);
        }
        else if(specialKeysPage == 2) {
            specialKeysToggler.setText("2/2");
            findViewById(R.id.special_keys_pg1).setVisibility(View.GONE);
            findViewById(R.id.special_keys_pg2).setVisibility(View.VISIBLE);
        }
    }

    public void myButtonPressed(MyButton sb, Boolean down) {
        if(sb.getId() == R.id.special_keys_toggler) {
            if(!down) {
                specialKeysPage = (specialKeysPage + 1) % 3;
                onKeysPageChanged();
            }
        }
        else
            WifiMouseApplication.networkConnection.sendMessage("SpecialKey "+(down?"Down ":"Up ")+sb.getText());
    }

    public void toggleNavigationDrawer() {
        drawerLayout.closeDrawer(rightDrawer);
        leftDrawer.bringToFront();

        if( drawerLayout.isDrawerOpen(leftDrawer) )
            drawerLayout.closeDrawer(leftDrawer);
        else
            drawerLayout.openDrawer(leftDrawer);
    }

    public void toggleRightDrawer() {
        drawerLayout.closeDrawer(leftDrawer);
        rightDrawer.bringToFront();

        if( drawerLayout.isDrawerOpen(rightDrawer) )
            drawerLayout.closeDrawer(rightDrawer);
        else
            drawerLayout.openDrawer(rightDrawer);
    }
}
