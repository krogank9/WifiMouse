package vivid.designs.wifimouse;

import android.util.Log;
import android.widget.Toast;

// Scan network and establish socket connection with WifiMouse server
class NetworkConnectionRunnable implements Runnable {

    public void run() {
        while(true) {
            WifiMouseApplication.KnownServer server = WifiMouseApplication.getSelectedServer();

            if( (NetworkSearchRunnable.wifiUnavailable && !server.bluetooth) || ! WifiMouseApplication.isAppOpen) {
                try { Thread.sleep(1000); }
                catch (InterruptedException e) {}
                continue;
            }

            if (server.name.equals("")) {
                WifiMouseApplication.lastConnectionResult = NetworkConnection.ConnectionResult.SERVER_NOT_FOUND;
                return;
            }

            WifiMouseApplication.networkConnection = NetworkConnection.newConnection(server, true);
            WifiMouseApplication.lastConnectionResult = WifiMouseApplication.networkConnection.connectToServer(true);
            WifiMouseApplication.connectionAttemptCounter++;

            Log.d("NetworkConnectionRu...", server.ip+" "+WifiMouseApplication.lastConnectionResult.toString());

            if (WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.SUCCESS) {
                WifiMouseApplication.addFoundServer(server);
                WifiMouseApplication.addFoundServer(server);
                WifiMouseApplication.networkConnection.startInputLoop();
            }
            else if (WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.WRONG_PASSWORD) {
                if (WifiMouseApplication.justSetPassword == true) {
                    WifiMouseApplication.justSetPassword = false;
                    WifiMouseApplication.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(WifiMouseApplication.appCtx, "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            else if (WifiMouseApplication.lastConnectionResult == NetworkConnection.ConnectionResult.SERVER_NOT_FOUND) {
                WifiMouseApplication.removeFoundServer(server);
            }

            try { Thread.sleep(1000); }
            catch (InterruptedException e) {}
        }
    }
}