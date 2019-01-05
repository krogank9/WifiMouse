package vivid.designs.wifimouse;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class NetworkUtils {
    public static String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public static String getSubnet(Context context) {
        String local_ip = getLocalIpAddress(context);
        if(local_ip == null)
            return null;

        int endIndex = local_ip.lastIndexOf(".") + 1;
        if(endIndex < 0)
            endIndex = 0;

        return local_ip.substring(0, endIndex);
    }

    public static byte[] intToBytes(int n) {
        byte[] b = new byte[4];
        for(int i=0; i<4; i++)
            b[i] = (byte) (n >>> 8*i);

        return b;
    }

    public static int bytesToInt(byte[] bytes) {
        int n = 0;
        for (int i=0; i<4 && i<bytes.length; i++) {
            n += ((int)bytes[i] & 0xFF) << 8*i;
        }
        return n;
    }

    public static byte[] shortArrToBytes(short[] sArr) {
        byte[] bArr = new byte[sArr.length*2];
        ByteBuffer bb = ByteBuffer.allocate(sArr.length*2);
        for(int i=0; i<sArr.length; i++) {
            bb.putShort(sArr[i]);
        }
        return bb.array();
    }

    public static byte[] combineByteArrays(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
