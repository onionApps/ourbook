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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Notifier {

    private static Notifier instance;
    int id = 5;
    private Context context;

    private Notifier(Context context) {
        context = context.getApplicationContext();
        this.context = context;
    }

    synchronized public static Notifier getInstance(Context context) {
        context = context.getApplicationContext();
        if (instance == null) {
            instance = new Notifier(context);
        }
        return instance;
    }

    private void log(String s) {
        Log.i("Notifier", s);
    }

    public void clr() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    public void msg() {

        if (MainActivity.getInstance() != null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(context.getResources().getColor(R.color.colorNotification))
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText("New Message")
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class).putExtra("page", "chat"), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_notification);

        notificationManager.notify(id, b.build());
    }

}
