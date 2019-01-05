package vivid.designs.wifimouse;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WifiMouseApplication extends Application {
    static boolean tutorialActivityOpen = false;
    static boolean serverChooserActivityOpen = false;
    static boolean isAppOpen = false;
    static SharedPreferences sharedPreferences;
    public static Context appCtx = null;
    public static int lastKnownVersion = 1;
    public static String lastKnownOs = "";

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getApplicationContext().getSharedPreferences("cow.emoji.WifiMouse", Context.MODE_PRIVATE);
        loadServersFromSharedPref();
        appCtx = getApplicationContext();
    }

    public static void saveServersToSharedPref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Set<String> savedServersSet = new HashSet<String>();
        for (int i = 0; i < savedServers.size(); i++) {
            savedServersSet.add(savedServers.get(i).toString());
        }
        editor.putStringSet("savedServers", savedServersSet);
        editor.putString("selectedServer", selectedServer.toString());
        editor.putString("lastKnownOs", lastKnownOs);
        editor.putInt("lastKnownVersion", lastKnownVersion);
        editor.commit();
    }

    private static void loadServersFromSharedPref() {
        Set<String> savedServersSet = sharedPreferences.getStringSet("savedServers", null);
        ArrayList<String> savedServersList = new ArrayList<String>();

        if (savedServersSet != null)
            savedServersList.addAll(savedServersSet);

        savedServers.clear();
        for (int i = 0; i < savedServersList.size(); i++) {
            savedServers.add(new KnownServer(savedServersList.get(i).toString()));
        }

        selectedServer = new KnownServer(sharedPreferences.getString("selectedServer", ""));
        lastKnownOs = sharedPreferences.getString("lastKnownOs", lastKnownOs);
        lastKnownVersion = sharedPreferences.getInt("lastKnownVersion", lastKnownVersion);
    }

    public static void setSelectedServer(KnownServer server) {
        selectedServer = server;
        addSavedServer(server);
        saveServersToSharedPref();
    }

    public static KnownServer getSelectedServer() {
        return selectedServer;
    }

    public static int getSelectedServerPosition() {
        for (int i = 0; i < savedServers.size(); i++) {
            if (savedServers.get(i).equals(selectedServer))
                return i;
        }
        return 0;
    }

    public static void setSelectedServerPassword(String password) {
        justSetPassword = true;
        selectedServer.password = password;
        saveServersToSharedPref();
    }

    static String tryAddServerIp = null;
    static boolean justSetPassword = false;
    private static KnownServer selectedServer = new KnownServer();
    static ArrayList<KnownServer> savedServers = new ArrayList<KnownServer>();
    static ArrayList<KnownServer> foundServers = new ArrayList<KnownServer>();
    static ArrayList<KnownServer> previouslyFoundServers = new ArrayList<>();
    static NetworkConnection.ConnectionResult lastConnectionResult = NetworkConnection.ConnectionResult.SERVER_NOT_FOUND;
    static int connectionAttemptCounter = 0;

    static Thread networkConnectionThread = new Thread();
    static Thread networkSearchThread = new Thread();
    static NetworkConnectionRunnable networkConnectionRunnable = new NetworkConnectionRunnable();
    static NetworkSearchRunnable networkSearchRunnable = new NetworkSearchRunnable();
    static NetworkConnection networkConnection = new TcpConnection(new KnownServer(), false);

    public static class KnownServer {
        String name = "";
        String ip = "";
        String password = "";
        boolean bluetooth = false;

        public KnownServer() {
        }

        public KnownServer(String name, String ip, String password, boolean bluetooth) {
            this.name = name;
            this.ip = ip;
            this.password = password;
            this.bluetooth = bluetooth;
        }

        public KnownServer(String serialized) {
            String[] fields = serialized.split("\n");
            if (fields.length == 4) {
                name = fields[0];
                ip = fields[1];
                password = fields[2];
                bluetooth = Boolean.parseBoolean(fields[3]);
            }
        }

        public boolean equals(KnownServer that) {
            return this.name.equals(that.name) && this.ip.equals(that.ip) && (this.bluetooth == that.bluetooth);
        }

        public String toString() {
            return this.name + "\n" + this.ip + "\n" + this.password + "\n" + this.bluetooth;
        }
    }

    public static boolean serverListContains(ArrayList<KnownServer> list, KnownServer server) {
        for (int i = 0; i < list.size(); i++) {
            if (server.equals(list.get(i)))
                return true;
        }
        return false;
    }

    public static void removeSavedServer(final KnownServer server) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < savedServers.size(); i++) {
                    if (savedServers.get(i).equals(server)) {
                        savedServers.remove(i);
                        if (server.equals(selectedServer)) {
                            selectedServer = new KnownServer();
                            Log.d("WifiMouseApplication", "Removing selected server");
                        }
                        break;
                    }
                }
                saveServersToSharedPref();
            }
        });
    }

    public static void removeFoundServer(final KnownServer server) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Iterator<KnownServer> it = WifiMouseApplication.foundServers.iterator(); it.hasNext(); ) {
                        WifiMouseApplication.KnownServer cur = it.next();
                        if (cur.equals(server))
                            it.remove();
                    }
                    for (int i = 0; i < foundServers.size(); i++) {
                        if (foundServers.get(i).equals(server)) {
                            foundServers.remove(i);
                        }
                    }
                } catch (ConcurrentModificationException ex) {
                }
            }
        });
    }

    public static void addFoundServer(final KnownServer server) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (serverListContains(foundServers, server) == false)
                    foundServers.add(server);
                if (serverListContains(previouslyFoundServers, server) == false)
                    previouslyFoundServers.add(server);
            }
        });
    }

    public static void addSavedServer(final KnownServer server) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < savedServers.size(); i++) {
                    if (savedServers.get(i).equals(server))
                        return;
                }
                savedServers.add(server);
                saveServersToSharedPref();
            }
        });
    }

    private static final Handler mHandler = new Handler();

    public static final void runOnUiThread(Runnable runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            mHandler.post(runnable);
        }
    }
}
