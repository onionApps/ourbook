/*
 * Network.onion - fully distributed p2p social network using onion routing
 *
 * http://play.google.com/store/apps/details?id=onion.network
 * http://onionapps.github.io/Network.onion/
 * http://github.com/onionApps/Network.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class HostService extends Service {

    String TAG = "HostService";
    Timer timer;
    Server server;
    Tor tor;
    PowerManager.WakeLock wakeLock;

    public HostService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Server.getInstance(this);
        Tor.getInstance(this);

        return START_STICKY;
    }

    void log(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void onCreate() {

        log("onCreate");

        super.onCreate();

        server = Server.getInstance(this);
        tor = Tor.getInstance(this);

        PowerManager pMgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLock");
        wakeLock.acquire();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {

                log("update");

                ChatClient.getInstance(getApplicationContext()).sendUnsent();

                RequestTool.getInstance(getApplicationContext()).sendAllRequests();

            }
        }, 0, 1000 * 60 * 60); // TODO: set correct delay

        WallBot.getInstance(this);

    }

    @Override
    public void onDestroy() {

        log("onDestroy");

        timer.cancel();
        timer.purge();
        timer = null;

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

        super.onDestroy();

    }
}

