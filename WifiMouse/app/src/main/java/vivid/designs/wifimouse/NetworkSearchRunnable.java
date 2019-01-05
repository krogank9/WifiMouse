package vivid.designs.wifimouse;

import android.util.Log;
import android.widget.Toast;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public class NetworkSearchRunnable implements Runnable {
    public static boolean wifiUnavailable = false;

    private static boolean checkIfShouldScan() {
        // Don't search network while connected or when results aren't displayed
        boolean connected = true;
        if( WifiMouseApplication.networkConnection != null)
            connected = WifiMouseApplication.networkConnection.isConnected();
        boolean serverListOpen = WifiMouseApplication.serverChooserActivityOpen || WifiMouseApplication.tutorialActivityOpen;
        return (!connected || serverListOpen) && WifiMouseApplication.isAppOpen;
    }

    private boolean tryConnectServer(WifiMouseApplication.KnownServer server) { return tryConnectServer(server, false); }
    private boolean tryConnectServer(WifiMouseApplication.KnownServer server, boolean patient) {
        if(server.ip.length() > 0 && server.ip.equals(WifiMouseApplication.getSelectedServer().ip))
            return false;

        // Handle bluetooth in checkBluetooth function
        if(server.bluetooth)
            return true;

        NetworkConnection networkConnection = NetworkConnection.newConnection(server, patient);

        boolean validServer = networkConnection.connectToServer(false) != NetworkConnection.ConnectionResult.SERVER_NOT_FOUND;
        if(validServer)
            WifiMouseApplication.addFoundServer(server);

        return validServer;
    }

    private boolean tryConnectServer(String ip) {
        WifiMouseApplication.KnownServer server = new WifiMouseApplication.KnownServer("WifiMouseServer", ip, "", false);
        return tryConnectServer(server);
    }

    private void checkManuallyAddedServer() {
        if(WifiMouseApplication.tryAddServerIp != null) {
            WifiMouseApplication.KnownServer server = new WifiMouseApplication.KnownServer("WifiMouseServer", WifiMouseApplication.tryAddServerIp, "", false);
            if( !tryConnectServer(server, true) ) {
                WifiMouseApplication.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WifiMouseApplication.appCtx, "Couldn't find server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else {
                WifiMouseApplication.setSelectedServer(server);
            }
            WifiMouseApplication.tryAddServerIp = null;
        }
    }

    private void searchForSavedServers() {
        for(int i=0; i<WifiMouseApplication.previouslyFoundServers.size(); i++) {
            WifiMouseApplication.KnownServer server = WifiMouseApplication.previouslyFoundServers.get(i);
            if( !WifiMouseApplication.serverListContains(WifiMouseApplication.foundServers, server) )
                tryConnectServer(server);
        }
        for(int i=0; i<WifiMouseApplication.savedServers.size(); i++) {
            WifiMouseApplication.KnownServer server = WifiMouseApplication.savedServers.get(i);
            // No need to add servers that are already found
            if(WifiMouseApplication.serverListContains(WifiMouseApplication.foundServers, server))
                continue;
            if( !server.ip.equals(WifiMouseApplication.getSelectedServer().ip) )
                tryConnectServer(server);
        }
    }

    private void clearUnresponsiveServers() {
        try {
            for (Iterator<WifiMouseApplication.KnownServer> it = WifiMouseApplication.foundServers.iterator(); it.hasNext(); ) {
                WifiMouseApplication.KnownServer server = it.next();
                if (!server.ip.equals(WifiMouseApplication.getSelectedServer().ip) && !server.bluetooth && !tryConnectServer(server, true))
                    it.remove();
            }
        } catch (ConcurrentModificationException ex) {}
    }

    private void startConnectionThread() {
        if(WifiMouseApplication.networkConnectionThread.isAlive() == false) {
            WifiMouseApplication.networkConnectionThread = new Thread(WifiMouseApplication.networkConnectionRunnable);
            WifiMouseApplication.networkConnectionThread.start();
        }
    }

    public boolean scanNetworkForHosts(){
        String subnet = NetworkUtils.getSubnet(WifiMouseApplication.appCtx);

        wifiUnavailable = (subnet == null);
        if(subnet == null || subnet.contains(":")) {
            checkManuallyAddedServer();
            startConnectionThread();
            checkBluetooth();
            return false;
        }

        for (int i=1;i<255;i++){
            //Log.d("NetworkSearc...", ""+i);
            // called every ~0.6s, start connection thread & test known servers
            if( ( i == 1 || i%10 == 0 ) ) {
                checkManuallyAddedServer();
                searchForSavedServers();
                clearUnresponsiveServers();

                startConnectionThread();
            }

            // check bluetooth every couple seconds
            if( i%40 == 0 )
                checkBluetooth();

            String ip = subnet + i;
            tryConnectServer(ip);

            if(!checkIfShouldScan())
                break;
        }
        return true;
    }

    public void checkBluetooth() {
        // Don't scan bluetooth if connection open
        if(WifiMouseApplication.getSelectedServer().bluetooth)
            return;

        WifiMouseApplication.KnownServer server = new WifiMouseApplication.KnownServer("WifiMouseServer", "", "", true);
        BluetoothConnection bluetoothConnection = new BluetoothConnection(server);
        NetworkConnection.ConnectionResult result = bluetoothConnection.connectToServer(false);
        //Log.d("checkBluetooth", result.toString());
        if(result != NetworkConnection.ConnectionResult.SERVER_NOT_FOUND)
            WifiMouseApplication.addFoundServer(server);
    }

    public void run() {
        while (true) {
            boolean scanSuccessful = false;
            startConnectionThread();
            if(checkIfShouldScan())
                scanSuccessful = scanNetworkForHosts();

            if( !scanSuccessful ) {
                // Sleep if search failed to prevent loop from spamming
                try { Thread.sleep(1000); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
    }
}
