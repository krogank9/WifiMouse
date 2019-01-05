package vivid.designs.wifimouse;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class NetworkService extends Service {
    WifiMouseApplication application;

    @Override
    public void onCreate()
    {
        super.onCreate();
        application = (WifiMouseApplication) this.getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(application.networkSearchThread.isAlive() == false) {
            application.networkSearchThread = new Thread(application.networkSearchRunnable);
            application.networkSearchThread.start();
        }

        if(WifiMouseApplication.networkConnectionThread.isAlive() == false) {
            WifiMouseApplication.networkConnectionThread = new Thread(WifiMouseApplication.networkConnectionRunnable);
            WifiMouseApplication.networkConnectionThread.start();
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        application.networkSearchThread.interrupt();
        application.networkConnectionThread.interrupt();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }
}
