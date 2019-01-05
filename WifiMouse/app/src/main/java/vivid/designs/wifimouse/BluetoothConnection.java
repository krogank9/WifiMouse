package vivid.designs.wifimouse;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnection extends NetworkConnection {
    private BluetoothSocket socket;
    private InputStream input;
    private OutputStream output;

    Runnable readTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            while (socket != null && socket.isConnected()) {
                if(System.currentTimeMillis() - lastReadStart > 1500) {
                    closeSocket();
                }
            }
        }
    };
    Thread readTimeoutThread = new Thread();

    private void startTimeoutThread() {
        lastReadStart = System.currentTimeMillis();
        if(readTimeoutThread.isAlive() == false) {
            readTimeoutThread = new Thread(readTimeoutRunnable);
            readTimeoutThread.start();
        }
    }

    public BluetoothConnection(@NonNull WifiMouseApplication.KnownServer server) {
        super(server);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(!adapter.isEnabled()) {
            connectionFailed = true;
            return;
        }
        Set<BluetoothDevice> deviceSet = adapter.getBondedDevices();
        for(Iterator<BluetoothDevice> it = deviceSet.iterator(); it.hasNext();) {
            if(adapter.isDiscovering()) {
                connectionFailed = true;
                return;
            }
            BluetoothDevice device = it.next();
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("c05efa2b-5e9f-4a39-9705-72ccf47d2eb8"));
                tryConnectBtSocket(2000);
                if(socket.isConnected()) {
                    Log.d("BluetoothConnection", "connected");
                    input = socket.getInputStream();
                    output = socket.getOutputStream();
                    connectionFailed = false;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                connectionFailed = true;
            }
        }

        startTimeoutThread();
    }

    private void tryConnectBtSocket(final int timeout) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try { Thread.sleep(timeout); }
                catch (InterruptedException e) { e.printStackTrace(); }

                if(!isConnected())
                    closeSocket();
            }
        }).start();

        try { socket.connect(); }
        catch (IOException e) {}
    }

    @Override
    public boolean isConnected() {
        try {
            // Bluetooth is a buggy pos so start readTimeoutThread when checking if connected.
            if(readTimeoutThread.isAlive() == false) {
                readTimeoutThread = new Thread(readTimeoutRunnable);
                readTimeoutThread.start();
            }
            return socket != null && socket.isConnected() && super.isConnected();
        } catch (IllegalThreadStateException ex) {
            // got a crash here once. fucky threads. no idea what it's about. just return false instead of crash
            return false;
        }
    }

    @Override
    protected void closeSocket() {
        if(socket == null)
            return;

        try { socket.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void sendDataUnencrypted(byte[] byteArray) {
        if(!isConnected() || byteArray.length > IO_MAX_FILE_SIZE)
            return;
        try {
            output.write(NetworkUtils.intToBytes(byteArray.length));
            output.write(byteArray);
        } catch (IOException e) {
            closeSocket();
            e.printStackTrace();
        }
    }

    private boolean readAllData(byte[] data) {
        int readSoFar = 0;
        if(!isConnected())
            return false;
        while(readSoFar < data.length) {
            int bytesLeft = data.length - readSoFar;
            int readThisTime = 0;
            try {
                lastReadStart = System.currentTimeMillis();
                readThisTime = input.read(data, readSoFar, bytesLeft);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if(readThisTime < 0)
                break;
            else
                readSoFar += readThisTime;
        }
        return readSoFar == data.length;
    }

    @Override
    public byte[] readDataUnencrypted() {
        if(!isConnected())
            return new byte[0];

        try {
            // Get data size
            byte[] dataLengthBytes = new byte[4];
            if( !readAllData(dataLengthBytes) ) {
                closeSocket();
                return new byte[0];
            }
            int size = NetworkUtils.bytesToInt(dataLengthBytes);
            if(size > IO_MAX_FILE_SIZE) {
                return new byte[0];
            }

            // Read data
            byte[] data = new byte[size];
            if( readAllData(data) ) {
                return data;
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        closeSocket();
        Log.d("BluetoothConnection", "Couldn't read all data");
        return new byte[0];
    }
}
