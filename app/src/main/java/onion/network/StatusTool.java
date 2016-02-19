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

import java.io.IOException;

public class StatusTool {

    private static StatusTool instance;
    Context context;

    public StatusTool(Context c) {
        context = c;
    }

    synchronized public static StatusTool getInstance(Context context) {
        if (instance == null)
            instance = new StatusTool(context.getApplicationContext());
        return instance;
    }

    public boolean isOnline(String address) {
        try {
            String rs = HttpClient.get(context, new ItemTask(context, address, "name").getUrl());
            if (rs == null || rs.isEmpty()) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

}
