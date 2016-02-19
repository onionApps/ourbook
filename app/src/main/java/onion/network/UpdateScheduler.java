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

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class UpdateScheduler {

    final static String TAG = "UpdateScheduler";

    private static UpdateScheduler instance;
    Context context;
    Handler handler = new Handler();

    public UpdateScheduler(Context context) {
        this.context = context;
    }

    synchronized public static UpdateScheduler getInstance(Context context) {
        if (instance == null)
            instance = new UpdateScheduler(context.getApplicationContext());
        return instance;
    }

    private void log(String s) {
        Log.i(TAG, s);
    }

    private void clear(String address) {
        log("clear");
        handler.removeCallbacksAndMessages(address);
    }

    private boolean hasPending(String address) {
        boolean pending = false;
        if (RequestDatabase.getInstance(context).hasOutgoing(address)) {
            log("still has pending requests");
            pending = true;
        }
        if (ChatClient.getInstance(context).hasUnsent(address)) {
            log("still has pending messages");
            pending = true;
        }
        return pending;
    }

    public void put(final String address) {

        clear(address);

        log("pu");

        if (!hasPending(address)) {
            log("nothing to send");
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                new Thread() {
                    @Override
                    public void run() {

                        log("update");

                        if (hasPending(address)) {
                            log("still some pending");
                        } else {
                            log("ok");
                            clear(address);
                            return;
                        }

                        try {
                            RequestTool.getInstance(context).sendUnsentReq(address);
                            ChatClient.getInstance(context).sendUnsent(address);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        if (hasPending(address)) {
                            log("still some pending");
                        } else {
                            log("ok");
                            clear(address);
                            return;
                        }

                    }
                }.start();

            }
        };

        long t0 = SystemClock.uptimeMillis();
        handler.postAtTime(runnable, address, t0 + 1000 * 12);
        handler.postAtTime(runnable, address, t0 + 1000 * 30);
        handler.postAtTime(runnable, address, t0 + 1000 * 60 * 1);
        handler.postAtTime(runnable, address, t0 + 1000 * 60 * 2);
        handler.postAtTime(runnable, address, t0 + 1000 * 60 * 5);
        handler.postAtTime(runnable, address, t0 + 1000 * 60 * 10);
        handler.postAtTime(runnable, address, t0 + 1000 * 60 * 20);

    }

}
