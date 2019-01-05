package vivid.designs.wifimouse;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpConnection extends NetworkConnection {
    private final Socket socket;
    private InputStream input;
    private OutputStream output;

    public TcpConnection(@NonNull WifiMouseApplication.KnownServer server, boolean patient) {
        super(server);

        socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(server.ip, 9798), patient? 1500:60);
            socket.setSoTimeout(1500);
            socket.setTcpNoDelay(true);

            input = socket.getInputStream();
            output = socket.getOutputStream();
        } catch (Exception e) {
            connectionFailed = true;
        }
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && super.isConnected();
    }

    @Override
    public void closeSocket() {
        try { socket.close(); }
        catch (IOException e) {}
    }

    @Override
    public void sendDataUnencrypted(byte[] byteArray) {
        if(!isConnected() || byteArray.length > IO_MAX_FILE_SIZE)
            return;

        try {
            output.write(NetworkUtils.intToBytes(byteArray.length));
            output.write(byteArray);
        } catch (IOException ex) {
            closeSocket();
            return;
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
        Log.d("TcpConnection", "Couldn't read all data");
        return new byte[0];
    }
}
