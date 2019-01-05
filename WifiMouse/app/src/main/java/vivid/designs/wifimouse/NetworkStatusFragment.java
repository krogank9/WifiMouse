package vivid.designs.wifimouse;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NetworkStatusFragment extends Fragment {

    public NetworkStatusFragment() {}

    View baseFrame;
    View backgroundView;
    TextView statusText;
    Button actionButton;

    private void showSetPassDialog() {
        View dialogView = LayoutInflater.from(baseFrame.getContext()).inflate(R.layout.dialog_set_password, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.set_password_edittext);
        Dialog addServerDialog = new android.app.AlertDialog.Builder(baseFrame.getContext())
                .setIcon(R.drawable.ic_lock)
                .setTitle("  Enter server password:")
                .setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                WifiMouseApplication.setSelectedServerPassword(editText.getText().toString());
                            }
                        }
                )
                .setNegativeButton("Cancel", null)
                .setView(dialogView)
                .create();
        addServerDialog.show();
    }

    private void actionButtonPressed() {
        if( actionButton.getText().equals("Set password") ) {
            showSetPassDialog();
        }
        else if( actionButton.getText().equals("Change servers") ) {
            startActivity(new Intent(getActivity(), ServerChooserActivity.class));
        }
    }

    enum StatusFragmentState {
        HIDDEN, HIDING, SHOWING, SHOWN
    }
    public StatusFragmentState myState = StatusFragmentState.HIDDEN;

    private void hideMe() {
        if(baseFrame == null)
            return;

        if(myState == StatusFragmentState.SHOWN || myState == StatusFragmentState.SHOWING) {
            myState = StatusFragmentState.HIDING;
            baseFrame.setTranslationY(0);
            baseFrame.animate().cancel();
            baseFrame.animate().translationY(baseFrame.getHeight()*-1).setDuration(500).setStartDelay(1000).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if(baseFrame == null)
                        return;

                    myState = StatusFragmentState.HIDDEN;
                    baseFrame.setVisibility(View.GONE);
                }
            }).start();
        }
    }

    private void showMe() {
        if(baseFrame == null)
            return;
        if(myState == StatusFragmentState.HIDDEN || myState == StatusFragmentState.HIDING) {
            myState = StatusFragmentState.SHOWING;
            baseFrame.setVisibility(View.VISIBLE);
            baseFrame.setTranslationY(-baseFrame.getHeight());
            baseFrame.animate().cancel();
            baseFrame.animate().translationY(0).setDuration(500).withEndAction(new Runnable() {
                @Override
                public void run() {
                    if(baseFrame == null)
                        return;

                    myState = StatusFragmentState.SHOWN;
                }
            }).start();
        }
    }

    public void updateUi() {
        if(baseFrame == null)
            return;

        if(WifiMouseApplication.getSelectedServer().bluetooth == false
        && WifiMouseApplication.networkConnection.isConnected() == false
        && NetworkSearchRunnable.wifiUnavailable == true) {
            backgroundView.setBackgroundResource(R.drawable.bg_lines_red);
            statusText.setText("WiFi unavailable.");
            actionButton.setVisibility(View.GONE);
            showMe();
        }
        else if(WifiMouseApplication.getSelectedServer().name.length() == 0) {
            backgroundView.setBackgroundResource(R.drawable.bg_lines_red);
            statusText.setText("No server selected.");
            actionButton.setText("Change servers");
            actionButton.setVisibility(getActivity() instanceof ServerChooserActivity? View.GONE:View.VISIBLE);
            // don't show "No server selected" when in server selection menu
            if(getActivity() instanceof ServerChooserActivity)
                hideMe();
            else
                showMe();
        }
        else if(WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.SERVER_NOT_FOUND) {
            backgroundView.setBackgroundResource(R.drawable.bg_lines_red);
            statusText.setText("Server "+WifiMouseApplication.getSelectedServer().name+" not found.");
            actionButton.setText("Change servers");
            actionButton.setVisibility(getActivity() instanceof ServerChooserActivity? View.GONE:View.VISIBLE);
            showMe();
        }
        else if(WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.WRONG_PASSWORD) {
            backgroundView.setBackgroundResource(R.drawable.bg_lines_red);
            statusText.setText("Incorrect password for "+WifiMouseApplication.getSelectedServer().name+".");
            actionButton.setText("Set password");
            actionButton.setVisibility(View.VISIBLE);
            showMe();
        }
        else if(WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.SUCCESS) {
            backgroundView.setBackgroundResource(R.drawable.bg_lines_green);
            statusText.setText("Connected to server "+WifiMouseApplication.getSelectedServer().name+"!");
            actionButton.setVisibility(View.GONE);
            hideMe();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View inflated = inflater.inflate(R.layout.fragment_network_status, container, false);
        baseFrame = inflated.findViewById(R.id.network_status_frame);
        baseFrame.setVisibility(View.INVISIBLE);
        statusText = (TextView) inflated.findViewById(R.id.network_status_text);
        actionButton = (Button) inflated.findViewById(R.id.network_status_action_button);
        backgroundView = inflated.findViewById(R.id.network_status_background);
        final View stripesToAnimate = backgroundView;
        inflated.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(baseFrame == null)
                    return;
                animateStripes(stripesToAnimate);
                if(myState == StatusFragmentState.HIDDEN)
                    baseFrame.setVisibility(View.GONE);
            }
        }, 300);
        baseFrame.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                if(baseFrame != null)
                    baseFrame.postDelayed(this, 300);
            }
        });
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtonPressed();
            }
        });
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

    private void animateStripes(final View stripes) {
        // stripes distance in original image was 10.5px and canvas size was 100px
        (new Runnable() {
            @Override
            public void run() {
                double stripeDist = stripes.getHeight() * (10.5/100);
                stripes.setTranslationY(0);
                stripes.animate().translationY((int)(stripeDist*-3)).withEndAction(this).setDuration(6000).setInterpolator(new LinearInterpolator()).start();
            }
        }).run();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baseFrame = null;
    }
}
