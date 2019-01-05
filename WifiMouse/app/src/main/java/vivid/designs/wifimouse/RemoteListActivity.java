package vivid.designs.wifimouse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class RemoteListActivity extends AppCompatActivity implements NavigationListFragment.NavigationDrawerToggler {

    ListView remoteListView;

    static Object[][] baseRemotesArray = {
            //{"Title", Fragment/Integer/String/null, drawable icon, OPTIONAL HASHTABLE PROPS},
            {"Mouse & Keyboard", null, R.drawable.ic_mouse_2},
            {"Screen Mirror", ScreenMirrorFragment.class, R.drawable.ic_logout},
            {"File Manager", FileManagerFragment.class, R.drawable.ic_folder},
            {"Run Command", RunCommandFragment.class, R.drawable.ic_terminal},
            {"Start Application", StartApplicationFragment.class, R.drawable.ic_search},
            {"Task Manager", TaskManagerFragment.class, R.drawable.ic_remove_from_queue},
    };
    static ArrayList<Object[]> baseRemotesArrayList = new ArrayList<Object[]>(Arrays.asList(baseRemotesArray));
    static ArrayList<Object[]> displayedRemotesList = new ArrayList<>();

    public static Object[] getRemoteByName(Context c, String name) {
        ArrayList<Object[]> allRemotes = new ArrayList<>();
        allRemotes.addAll(baseRemotesArrayList);
        allRemotes.addAll(RemoteFiles.getAllRemotes(c));
        for(int i=0; i<allRemotes.size(); i++)
            if( ((String) allRemotes.get(i)[0]).equals(name) )
                return allRemotes.get(i);
        return baseRemotesArray[0];
    }

    public void updateUi() {
        NetworkStatusFragment statusFragment = (NetworkStatusFragment) getSupportFragmentManager().findFragmentById(R.id.network_status_fragment);
        if(statusFragment != null) {
            boolean shown = statusFragment.myState != NetworkStatusFragment.StatusFragmentState.HIDDEN;
            findViewById(R.id.network_status_container).setVisibility(shown ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_list);

        displayedRemotesList.clear();
        displayedRemotesList.addAll(baseRemotesArrayList);
        displayedRemotesList.addAll(RemoteFiles.getAllRemotes(this));
        for(Iterator<Object[]> it = displayedRemotesList.iterator(); it.hasNext(); ) {
            Object[] remote = it.next();
            if(remote.length >= 4 && remote[3] instanceof Hashtable) {
                Hashtable<String, String> props = (Hashtable<String, String>) remote[3];
                // remove remote from display list if OS or version requirement not met
                if(props.containsKey(WifiMouseApplication.lastKnownOs) && props.get(WifiMouseApplication.lastKnownOs).equals("false"))
                    it.remove();
                else if(props.containsKey("minVer") && Integer.parseInt((String)props.get("minVer")) > WifiMouseApplication.lastKnownVersion)
                    it.remove();
            }
        }
        Log.d("lastKnownVersion", ":"+WifiMouseApplication.lastKnownVersion);
        Log.d("lastKnownOs", ":"+WifiMouseApplication.lastKnownOs);

        remoteListView = (ListView) findViewById(R.id.remote_list_view);
        remoteListView.setAdapter(new RemoteListAdapter(getApplicationContext(), displayedRemotesList));

        remoteListView.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                remoteListView.postDelayed(this, 300);
            }
        });

        remoteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), RemoteActivity.class);
                try {
                    intent.putExtra("RemoteName", (String) displayedRemotesList.get(position)[0]);
                    startActivity(intent);
                } catch (Exception e){}
            }
        });

        findViewById(R.id.navigation_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationDrawer();
            }
        });
    }

    boolean alreadyShownTutorial = false;
    @Override
    protected void onResume() {
        super.onResume();
        WifiMouseApplication.isAppOpen = true;
        startService(new Intent(this, NetworkService.class));

        SharedPreferences sharedPref = getSharedPreferences("cow.emoji.WifiMouse", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        boolean force_tutorial = sharedPref.getBoolean("force_tutorial", false);
        boolean needs_tut = sharedPref.getBoolean("needs_tutorial", true) && !alreadyShownTutorial;
        boolean finished_tut_once = sharedPref.getBoolean("finished_tutorial_once",false);

        if(needs_tut || force_tutorial) {
            alreadyShownTutorial = true;
            editor.putBoolean("force_tutorial", false);
            Intent tutorialIntent = new Intent(this, TutorialActivity.class);
            startActivity(tutorialIntent);
        } else if(!finished_tut_once) {
            // Exit activity if user closes tutorial before completing it at least once previously
            finish();
        }

        editor.commit();

        RemoteListAdapter remoteListAdapter = (RemoteListAdapter) remoteListView.getAdapter();
        remoteListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WifiMouseApplication.isAppOpen = false;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        stopService(new Intent(this,NetworkService.class));
    }

    public static class RemoteListAdapter extends ArrayAdapter<Object[]> {

        public RemoteListAdapter(@NonNull Context context, List<Object[]> servers) {
            super(context, 0, servers);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object[] remoteInfo = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_element_remote, parent, false);
            }

            TextView serverName = (TextView) convertView.findViewById(R.id.remote_element_name);
            serverName.setText((String) remoteInfo[0]);

            ImageView icon = (ImageView) convertView.findViewById(R.id.remote_element_icon);
            icon.setImageResource((int)remoteInfo[2]);

            return convertView;
        }

    }

    public void toggleNavigationDrawer() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        View leftDrawer = findViewById(R.id.left_drawer);

        leftDrawer.bringToFront();

        if( drawer.isDrawerOpen(leftDrawer) )
            drawer.closeDrawer(leftDrawer);
        else
            drawer.openDrawer(leftDrawer);
    }
}
