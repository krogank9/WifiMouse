package vivid.designs.wifimouse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class ScreenMirrorUtils {
    public static Rect cropRect = new Rect();
    public static float curQualityPct = 0.0f;

    static byte[] bitmapBytes;
    static Bitmap screenMirrorBitmap;
    static long lastScreenFetch;
    static long screenFetchCounter = 0;

    final static int wifiMaxQuality = 33;
    final static int bluetoothMaxQuality = 16;

    static long curScreenFetch = screenFetchCounter;
    static Runnable bitmapUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            while(System.currentTimeMillis() - lastScreenFetch < 1000) {
                if(curScreenFetch != screenFetchCounter) {
                    try {
                        screenMirrorBitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                        curScreenFetch = screenFetchCounter;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        // got array index out of bounds exception before here. maybe bitmapBytes was null
                    }
                }
            }
        }
    };
    static Thread bitmapUpdateThread = new Thread();

    public static void getScreenMirror(NetworkConnection connection) {
        lastScreenFetch = System.currentTimeMillis();

        Rect curRect = cropRect; // incase it gets set to null by fragment mid execution

        curQualityPct = Math.max( Math.min(1.0f, curQualityPct), 0.0f );
        int quality = (int) (curQualityPct*(connection instanceof BluetoothConnection? bluetoothMaxQuality:wifiMaxQuality));
        String crop = "";
        if(curRect != null)
            crop = " "+curRect.left+" "+ curRect.top+" "+ curRect.width()+" "+ curRect.height();
        connection.sendStringOverNetwork("ScreenMirror "+quality+crop, true);
        bitmapBytes = connection.readDataEncrypted();

        if(bitmapBytes != null && bitmapBytes.length > 0)
            screenFetchCounter++;
        else
            return;

        if(!bitmapUpdateThread.isAlive()) {
            bitmapUpdateThread = new Thread(bitmapUpdateRunnable);
            bitmapUpdateThread.start();
        }
    }
}
