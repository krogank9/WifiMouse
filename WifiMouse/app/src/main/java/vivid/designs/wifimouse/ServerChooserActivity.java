package vivid.designs.wifimouse;

import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class ServerChooserActivity extends AppCompatActivity implements NavigationListFragment.NavigationDrawerToggler {

    boolean activityVisible = true;
    public void updateUi() {
        if(!activityVisible)
            return;

        NetworkStatusFragment statusFragment = (NetworkStatusFragment) getSupportFragmentManager().findFragmentById(R.id.network_status_fragment);
        if(statusFragment != null) {
            boolean shown = statusFragment.myState != NetworkStatusFragment.StatusFragmentState.HIDDEN;
            findViewById(R.id.network_status_container).setVisibility(shown ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_chooser);

        findViewById(R.id.navigation_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationDrawer();
            }
        });

        final View statusContainer = findViewById(R.id.network_status_container);
        statusContainer.post(new Runnable() {
            @Override
            public void run() {
                updateUi();
                statusContainer.postDelayed(this, 300);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WifiMouseApplication.isAppOpen = true;
        activityVisible = true;
        WifiMouseApplication.serverChooserActivityOpen = true;
    }
    @Override
    protected void onPause() {
        super.onPause();
        activityVisible = false;
        WifiMouseApplication.serverChooserActivityOpen = false;
        WifiMouseApplication.isAppOpen = false;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        stopService(new Intent(this,NetworkService.class));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        View leftDrawer = findViewById(R.id.left_drawer);

        boolean wasOpen = drawer.isDrawerOpen(leftDrawer);
        drawer.closeDrawers();

        if( wasOpen == false )
            super.onBackPressed();
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
