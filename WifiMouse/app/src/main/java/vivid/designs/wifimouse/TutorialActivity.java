package vivid.designs.wifimouse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class TutorialActivity extends AppCompatActivity {
    int tutorial_step = 0;
    int[] step_ids = {R.id.step_1, R.id.step_2, R.id.step_3};
    Button prevButton;
    Button nextButton;

    View wrongPassword;
    View passwordView;

    TextView step3_text_incorrect;
    TextView step3_text_correct;
    EditText passwordEditText;
    long last_err_time = 0;

    private void updateUi() {
        if(!WifiMouseApplication.tutorialActivityOpen)
            return;

        // Update password entry
        if(WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.WRONG_PASSWORD) {
            // Show incorrect password text & image
            wrongPassword.setVisibility(View.VISIBLE);
            step3_text_correct.setVisibility(View.GONE);
            step3_text_incorrect.setVisibility(View.VISIBLE);
            passwordView.setVisibility(View.VISIBLE);
        }
        else if(WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.SERVER_NOT_FOUND) {
            if(tutorial_step == 2 && System.currentTimeMillis() - last_err_time > 3000) {
                last_err_time = System.currentTimeMillis();
                Toast.makeText(getApplicationContext(), "Lost connection to server", Toast.LENGTH_SHORT).show();
            }
        }
        else if(WifiMouseApplication.networkConnection.isConnected()) {
            // Show correct password text & hide incorrect image
            wrongPassword.setVisibility(View.GONE);
            step3_text_correct.setVisibility(View.VISIBLE);
            step3_text_incorrect.setVisibility(View.GONE);
            passwordView.setVisibility(View.INVISIBLE);
        }

        // After entering password, show message if it was wrong
        if(passwordJustSet
        && connectionCounter != WifiMouseApplication.connectionAttemptCounter
        && WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.WRONG_PASSWORD) {
            passwordJustSet = false;
            Toast.makeText(getApplicationContext(), "Password incorrect", Toast.LENGTH_SHORT).show();
        }

        // If selected server is empty, set it to the first one we find.
        //if(WifiMouseApplication.getSelectedServer().name.equals("") && WifiMouseApplication.foundServers.size() > 0)
            //WifiMouseApplication.setSelectedServer(WifiMouseApplication.foundServers.get(0));

        updateButtons();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        prevButton = (Button) findViewById(R.id.prev_button);
        nextButton = (Button) findViewById(R.id.next_button);

        wrongPassword = findViewById(R.id.wrong_password);
        passwordView = findViewById(R.id.tutorial_password_input);
        passwordEditText = (EditText) findViewById(R.id.tutorial_password_edittext);

        // Set up server list
        passwordView.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                passwordView.postDelayed(this, 300);
            }
        });

        // Create textviews from html for bolding and coloring:
        TextView tv1 = (TextView) findViewById(R.id.text_step_1);
        TextView tv2 = (TextView) findViewById(R.id.text_step_2);
        TextView tv3 = (TextView) findViewById(R.id.text_step_3);
        TextView tv3_incorrect = (TextView) findViewById(R.id.text_step_3_incorrect);

        // This may cause weird startup crash:
        // I think I fixed it by disabling saved instances
        tv1.setText(Html.fromHtml(tv1.getText().toString().replace("(","<").replace(")",">").replace("[","(").replace("]",")")));
        tv2.setText(Html.fromHtml(tv2.getText().toString().replace("(","<").replace(")",">").replace("[","(").replace("]",")")));
        tv3.setText(Html.fromHtml(tv3.getText().toString().replace("(","<").replace(")",">").replace("[","(").replace("]",")")));
        tv3_incorrect.setText(Html.fromHtml(tv3_incorrect.getText().toString().replace("(","<").replace(")",">")));

        step3_text_correct = tv3;
        step3_text_incorrect = tv3_incorrect;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, NetworkService.class));
        WifiMouseApplication.isAppOpen = true;
        WifiMouseApplication.tutorialActivityOpen = true;
        nextButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateAll();
            }
        }, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WifiMouseApplication.isAppOpen = false;
        WifiMouseApplication.tutorialActivityOpen = false;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        stopService(new Intent(this,NetworkService.class));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    boolean animated = false;
    private void animateAll() {
        if(animated)
            return;
        animated = true;

        animateLoadingBar();
        animatePassword();
    }

    int counter = 1;
    int numChars = 6;
    private void animatePassword() {
        final ImageView bg = (ImageView) findViewById(R.id.password_reveal);

        final float startX = bg.getX();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                counter = (counter+1) % (numChars+1);

                float width = bg.getWidth();
                float passwordLength = (float)(222.945/500) * width;
                float movePercent = ((float)counter)/((float)numChars);
                bg.setX(startX + passwordLength*movePercent);

                bg.postDelayed(this, counter==0?650:(long)(200+400*Math.random()));
            }
        };
        runnable.run();
    }

    private void animateLoadingBar() {
        final View stripes = findViewById(R.id.moving_stripes);
        final int duration = 2000;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                stripes.setTranslationX(0);
                stripes.animate().translationX(stripes.getWidth()/-10).withEndAction(this).setDuration(duration).setInterpolator(new LinearInterpolator()).start();
            }
        };
        runnable.run();
    }

    public void updateButtons() {
        boolean result = false;
        switch(tutorial_step) {
            case 0:
                result = true;
                break;
            case 1:
                result = WifiMouseApplication.lastConnectionResult != NetworkConnection.ConnectionResult.SERVER_NOT_FOUND;
                break;
            case 2:
                result = WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.SUCCESS;
                break;
        }
        final boolean b = result;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                nextButton.setEnabled(b);
                prevButton.setEnabled(tutorial_step > 0);
            }
        });
    }

    public void incrStep(int n) {
        final int newStep = tutorial_step+n;
        if(newStep < step_ids.length && newStep >= 0) {
            tutorial_step = newStep;
            for(int i=0; i < step_ids.length; i++) {
                findViewById(step_ids[i]).setVisibility( i==tutorial_step? View.VISIBLE:View.GONE );
            }

            updateButtons();

            if(tutorial_step+1 >= step_ids.length)
                nextButton.setText("DONE");
            else
                nextButton.setText("NEXT");
        }
        else if(newStep >= step_ids.length) {
            Intent mainIntent = new Intent(this, RemoteListActivity.class);
            startActivity(mainIntent);

            SharedPreferences sharedPref = getSharedPreferences("cow.emoji.WifiMouse", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("needs_tutorial", false);
            editor.putBoolean("finished_tutorial_once", true);
            editor.commit();

            finish();
        }
    }

    public void nextStep(View v) { incrStep(1); }
    public void prevStep(View v) { incrStep(-1); }

    @Override
    public void onBackPressed() {
        if(tutorial_step > 0)
            incrStep(-1);
        else
            finish();
    }

    boolean passwordJustSet = false;
    int connectionCounter = 0;
    public void tutorialSetPassword(View v) {
        WifiMouseApplication.setSelectedServerPassword(passwordEditText.getText().toString());
        connectionCounter = WifiMouseApplication.connectionAttemptCounter;
        passwordJustSet = true;
    }
}
