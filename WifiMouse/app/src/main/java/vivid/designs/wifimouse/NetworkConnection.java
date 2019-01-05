package vivid.designs.wifimouse;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

abstract public class NetworkConnection {
    private long lastPing;
    private long lastLoop = 0;
    private boolean inLoop = false;
    private boolean loopStarted = false;
    protected long lastReadStart = 0;
    private Vector<Message> messageList = new Vector<>();

    protected WifiMouseApplication.KnownServer server;
    public static final int IO_MAX_FILE_SIZE = 25 * 1000 * 1000; // read/write 25mb max
    public int serverVersion = 0;
    public String serverOs = "windows";

    private byte[] sessionPasswordHash = new byte[0];
    private int sessionIV = 0;

    public boolean isConnected() {
        if(loopStarted)
            return inLoop && System.currentTimeMillis() - lastLoop < 1000;
        else
            return !connectionFailed;
    }
    abstract void sendDataUnencrypted(byte[] byteArray);
    abstract byte[] readDataUnencrypted();

    private static int count = 0;
    public void sendDataEncrypted(byte[] byteArray) {
        if(this instanceof BluetoothConnection) {
            // bluetooth already encrypted
            sendDataUnencrypted(byteArray);
            return;
        }

        sessionIV = (sessionIV + 1) % Integer.MAX_VALUE;
        byte[] ivHash = EncryptUtils.makeHash16(sessionIV);

        // don't need to add padding
        if(byteArray.length%16 == 0) {
            sendDataUnencrypted(EncryptUtils.encryptBytesAES(byteArray, sessionPasswordHash, ivHash));
            return;
        }

        int padding = 16-(byteArray.length%16);
        byte[] resized16 = new byte[byteArray.length + padding];
        System.arraycopy(byteArray, 0, resized16, 0, byteArray.length);

        byte[] encrypted = EncryptUtils.encryptBytesAES(resized16, sessionPasswordHash, ivHash);
        sendDataUnencrypted(encrypted);
    }
    public byte[] readDataEncrypted() {
        byte[] readBytes = readDataUnencrypted();
        if(this instanceof BluetoothConnection)
            return readBytes;
        else {
            sessionIV = (sessionIV + 1) % Integer.MAX_VALUE;
            byte[] ivHash = EncryptUtils.makeHash16(sessionIV);
            return EncryptUtils.decryptBytesAES(readBytes, sessionPasswordHash, ivHash);
        }
    }

    public void sendStringOverNetwork(@NonNull String str, boolean encrypt) {
        // Sending to server written in C++. Make sure str null terminated
        str = str + '\0';
        try {
            if (encrypt)
                sendDataEncrypted(str.getBytes("UTF-8"));
            else
                sendDataUnencrypted(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {}
    }
    public String readStringFromNetwork(boolean decrypt) {
        byte[] bytes;

        if(decrypt)
            bytes = readDataEncrypted();
        else
            bytes = readDataUnencrypted();

        String str;
        try {
            try {
                str = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                str = new String(bytes);
            }
        } catch (Exception e) { return ""; } // got a null pointer exception here somehow

        // String received from server written in C++ will be null terminated
        for(int i=0; i<str.length(); i++) {
            if(str.charAt(i) == '\0') {
                str = str.substring(0, i);
                break;
            }
        }
        return str;
    }

    public NetworkConnection(WifiMouseApplication.KnownServer server) {
        this.server = server;
        this.sessionPasswordHash = EncryptUtils.makeHash16(server.password);
    }

    static NetworkConnection newConnection(WifiMouseApplication.KnownServer server, boolean patient) {
        if(server.bluetooth)
            return new BluetoothConnection(server);
        else
            return new TcpConnection(server, patient);
    }

    public enum ConnectionResult {
        SERVER_NOT_FOUND, SUCCESS, WRONG_PASSWORD
    }

    protected boolean connectionFailed = false;
    public ConnectionResult connectToServer(boolean establish) {
        ConnectionResult result = ConnectionResult.SERVER_NOT_FOUND;
        if(connectionFailed)
            return result;
        count = 0;

        sendStringOverNetwork("cow.emoji.WifiMouseClient", false);

        String response = readStringFromNetwork(false);
        Log.d("response", " "+response);

        // get the server & encryption IV
        String[] auth_data = response.split(" ");
        if(auth_data.length >= 1 && auth_data[0].equals("cow.emoji.WifiMouseServer")) {
            try { serverVersion = Integer.parseInt(auth_data[1]); } catch (Exception e) {}
            try { server.name = auth_data[2]; } catch (Exception e) {}
            try { serverOs = auth_data[3]; } catch (Exception e) {}
            try { sessionIV = Integer.parseInt( auth_data[4] ); } catch (Exception e) {}
            result = ConnectionResult.WRONG_PASSWORD; // server was at least found.
        }
        else {
            closeConnection();
            return result;
        }

        if(!establish) {
            sessionIV++; // send incorrect dummy string to force disconnect
            sendStringOverNetwork(" ", true);
            readStringFromNetwork(false);
            closeSocket();
            return result;
        }

        // now we have session IV & can encrypt. send encrypted message to verify
        sendStringOverNetwork("cow.emoji.WifiMouseClient", true);

        String response2 = readStringFromNetwork(false);
        if(response2.equals("Verified"))
            result = ConnectionResult.SUCCESS;
        else if(response2.equals("Wrong password"))
            result = ConnectionResult.WRONG_PASSWORD;

        if(result != ConnectionResult.SUCCESS) {
            Log.d("NetworkConnection", "closeConnection()");
            Log.d("NetworkConnection", "isConnected() == "+isConnected());
            sendStringOverNetwork("Quit", true);
            closeSocket();
        }
        else {
            WifiMouseApplication.lastKnownVersion = serverVersion;
            WifiMouseApplication.lastKnownOs = serverOs;
            WifiMouseApplication.saveServersToSharedPref();
        }

        return result;
    }

    // Close connection with a small time tolerance to allow for
    //  continuous connection between activity changes
    long lastAppWasOpen = 0;
    private boolean checkShouldContinueLoop() {
        long now = System.currentTimeMillis();
        if(WifiMouseApplication.isAppOpen)
            lastAppWasOpen = now;
        return (now - lastAppWasOpen) < 30000;
    }

    public void startInputLoop() {
        lastLoop = lastPing = System.currentTimeMillis();
        inLoop = true;
        loopStarted = true;
        while (isConnected() && checkShouldContinueLoop()) {
            lastLoop = System.currentTimeMillis();
            // handle messages
            if (messageList.size() > 0) {
                Message message = messageList.get(0);
                if (message != null) {
                    messageList.remove(0);
                    receiveMessage(message);
                }
            }

            if(System.currentTimeMillis() - lastPing > 500) {
                if(pingServer())
                    lastPing = System.currentTimeMillis();
                else
                    break;
            }
        }
        closeSocket();
        inLoop = false;
    }

    abstract void closeSocket();

    protected void closeConnection() {
        sendMessage("Quit");
    }

    public boolean pingServer() {
        // disconnect if server changed.
        WifiMouseApplication.KnownServer selected = WifiMouseApplication.getSelectedServer();
        if(this.server.name != selected.name || this.server.bluetooth != selected.bluetooth)
            return false;

        sendStringOverNetwork("PING", true);
        //Log.d("Waiting for ping...", "count: "+(++count) +", sessionIv: "+sessionIV);
        String response = readStringFromNetwork(true);
        //Log.d("PingResponse", " "+response);

        return response.equals("PING");
    }

    public static class Message {
        String str;
        long sentTime;
        MessageCallback callback = null;
        byte[] dataToSend = null;
        public Message(String str) { this.str = new String(str); sentTime = System.currentTimeMillis(); }
        public boolean equals(@NonNull String to) { return str.equals(to); }
        public boolean startsWith(String to) { return str.startsWith(to); }
    }

    AtomicInteger scrollQueue = new AtomicInteger(0);
    public void sendMessageMouseScroll(int amt) {
        scrollQueue.addAndGet(amt);
        if(isConnected())
            messageList.add(new Message("MouseScroll"));
    }

    AtomicInteger xMoveQueue = new AtomicInteger(0);
    AtomicInteger yMoveQueue = new AtomicInteger(0);
    public void sendMessageMouseMove(int x, int y) {
        xMoveQueue.addAndGet(x);
        yMoveQueue.addAndGet(y);
        if(isConnected())
            messageList.add(new Message("MouseMove"));
    }

    public void sendMessage(@NonNull Message message) {
        if(isConnected()) {
            messageList.add(message);
        }
    }

    public void sendMessage(@NonNull String message) {
        if(isConnected()) {
            messageList.add(new Message(message));
        }
        else {
            Log.d("sendMessage", "Not connected. Couldn't send message: "+message);
        }
    }

    public abstract class MessageCallback {
        abstract void messageCallback();
    }

    // add support for callbacks with strings for relevant messages
    public void sendMessageForResult(@NonNull String message, final RemoteScript.FunctionCallback callback) {
        if(message.startsWith("Command Run ")) {
            Message msgObj = new Message(message);
            msgObj.callback = new MessageCallback() {
                @Override
                void messageCallback() {
                    callback.functionCallback(RunCommandUtils.lastCommandOutput);
                }
            };
            messageList.add(msgObj);
        }
        else if(message.startsWith("DownloadUrl ")) {
            Message msgObj = new Message(message);
            msgObj.callback = new MessageCallback() {
                @Override
                void messageCallback() {
                    String urlContents = readStringFromNetwork(true);
                    callback.functionCallback(urlContents);
                }
            };
            messageList.add(msgObj);
        }
    }

    private void receiveMessage(Message message) {
        if(message.equals("Quit")) {
            Log.d("NetworkConnection", "Quitting");
            sendStringOverNetwork("Quit", true);
            closeSocket();
            inLoop = false;
        }
        else if(message.startsWith("FileManager ")) {
            message.str = message.str.substring("FileManager ".length());
            FileManagerUtils.fileManagerCommand(message.str, this);
        }
        else if(message.equals("ScreenMirror")) {
            if(System.currentTimeMillis() - message.sentTime < 200)
                ScreenMirrorUtils.getScreenMirror(this);
        }
        else if(message.startsWith("Command ")) {
            RunCommandUtils.callHelperFunc(this, message.str);
        }
        else if(message.equals("GetApplications")) {
            sendStringOverNetwork(message.str, true);
            StartApplicationFragment.applicationsListResult = readStringFromNetwork(true);
        }
        else if(message.equals("GetRamUsage")) {
            TaskManagerUtils.getRamUsage(this);
        }
        else if(message.equals("GetCpuUsage")) {
            TaskManagerUtils.getCpuUsage(this);
        }
        else if(message.equals("GetTasks")) {
            TaskManagerUtils.getTasks(this);
        }
        else if(message.equals("MouseScroll")) {
            sendStringOverNetwork("MouseScroll " + scrollQueue.getAndSet(0), true);
        }
        else if(message.equals("MouseMove")) {
            String movMsg = xMoveQueue.getAndSet(0) + "," + yMoveQueue.getAndSet(0);
            sendStringOverNetwork("MouseMove " + movMsg, true);
        }
        else {
            sendStringOverNetwork(message.str, true);
            if(message.dataToSend != null)
                sendDataEncrypted(message.dataToSend);
        }

        if(message.callback != null)
            message.callback.messageCallback();
    }
}
