package vivid.designs.wifimouse;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

public class ServerListFragment extends Fragment {
    View baseFrame;
    ListView savedServersList;
    ListView foundServersList;
    ArrayList<WifiMouseApplication.KnownServer> onlyFoundServers = new ArrayList<>();
    ArrayList<WifiMouseApplication.KnownServer> savedServers = new ArrayList<>();

    public ServerListFragment() {}

    public void updateUi() {
        if(baseFrame == null)
            return;

        // Populate saved server list
        savedServers.clear();
        savedServers.addAll(WifiMouseApplication.savedServers);
        ServerAdapter savedServersListAdapter = (ServerAdapter) savedServersList.getAdapter();
        savedServersListAdapter.notifyDataSetChanged();

        // Populate ArrayList with foundServers minus ones already displayed in saved
        onlyFoundServers.clear();
        for(int i=0; i<WifiMouseApplication.foundServers.size(); i++) {
            WifiMouseApplication.KnownServer server = WifiMouseApplication.foundServers.get(i);
            if(!WifiMouseApplication.serverListContains(savedServers, server))
                onlyFoundServers.add(server);
        }
        ServerAdapter foundServersListAdapter = (ServerAdapter) foundServersList.getAdapter();
        foundServersListAdapter.notifyDataSetChanged();

        boolean nowifi = NetworkSearchRunnable.wifiUnavailable;

        baseFrame.findViewById(R.id.none_saved_text).setVisibility(WifiMouseApplication.savedServers.size() == 0 ? View.VISIBLE : View.INVISIBLE);
        baseFrame.findViewById(R.id.searching_servers_text).setVisibility(!nowifi && onlyFoundServers.size() == 0? View.VISIBLE : View.INVISIBLE);
        baseFrame.findViewById(R.id.server_no_wifi_text).setVisibility(nowifi && onlyFoundServers.size() == 0? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflated = inflater.inflate(R.layout.fragment_server_list, container, false);

        baseFrame = inflated.findViewById(R.id.base_frame);

        // Set up found servers list
        infiniteRotateAnim(inflated.findViewById(R.id.found_servers_searching));
        foundServersList = (ListView) inflated.findViewById(R.id.found_server_list);
        foundServersList.setAdapter(new ServerAdapter(getContext(), onlyFoundServers));

        foundServersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiMouseApplication.KnownServer server = (WifiMouseApplication.KnownServer) parent.getItemAtPosition(position);
                WifiMouseApplication.setSelectedServer( server );
                WifiMouseApplication.addSavedServer(server);
                updateUi();
            }
        });

        savedServersList = (ListView) inflated.findViewById(R.id.saved_server_list);
        savedServersList.setAdapter(new ServerAdapter(getContext(), WifiMouseApplication.savedServers));

        savedServersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiMouseApplication.KnownServer server = (WifiMouseApplication.KnownServer) parent.getItemAtPosition(position);
                WifiMouseApplication.setSelectedServer( server );
                updateUi();
            }
        });

        savedServersList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiMouseApplication.KnownServer server = (WifiMouseApplication.KnownServer) parent.getItemAtPosition(position);
                AlertDialog.Builder builder = new AlertDialog.Builder(savedServersList.getContext());
                builder.setMessage("Remove server "+server.name+"?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        WifiMouseApplication.removeSavedServer(server);
                    }
                }).setNegativeButton("No", null).show();
                return true;
            }
        });

        inflated.findViewById(R.id.floating_action_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddServerPrompt();
            }
        });

        baseFrame.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                if(baseFrame != null)
                    savedServersList.postDelayed(this, 300);
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

    public static class ServerAdapter extends ArrayAdapter<WifiMouseApplication.KnownServer> {

        public ServerAdapter(@NonNull Context context, ArrayList<WifiMouseApplication.KnownServer> servers) {
            super(context, 0, servers);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(position < 0) {
                View v = new View(getContext());
                v.setVisibility(View.GONE);
                return v;
            }

            WifiMouseApplication.KnownServer server = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_element_server, parent, false);
            }

            TextView serverName = (TextView) convertView.findViewById(R.id.server_element_name);
            serverName.setText(server.name);

            ImageView icon = (ImageView) convertView.findViewById(R.id.server_element_icon);
            icon.setImageResource(server.bluetooth? R.drawable.ic_bluetooth:R.drawable.ic_wifi);

            RadioButton selected = (RadioButton) convertView.findViewById(R.id.server_element_radio_button);
            selected.setVisibility(server.equals(WifiMouseApplication.getSelectedServer())? View.VISIBLE:View.INVISIBLE);

            return convertView;
        }

    }

    private void infiniteRotateAnim(final View v) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                v.animate().rotationBy(-360).withEndAction(this).setDuration(6000).setInterpolator(new LinearInterpolator()).start();
            }
        };
        v.animate().rotationBy(-360).withEndAction(runnable).setDuration(6000).setInterpolator(new LinearInterpolator()).start();
    }

    public void showAddServerPrompt() {
        View dialogView = LayoutInflater.from(baseFrame.getContext()).inflate(R.layout.dialog_add_server, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.add_server_edittext);
        Dialog addServerDialog = new android.app.AlertDialog.Builder(baseFrame.getContext())
                .setIcon(R.drawable.ic_wifi)
                .setTitle("  Enter server IP:")
                .setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                WifiMouseApplication.tryAddServerIp = editText.getText().toString();
                            }
                        }
                )
                .setNegativeButton("Cancel", null)
                .setView(dialogView)
                .create();
        addServerDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        baseFrame = null;
    }
}
