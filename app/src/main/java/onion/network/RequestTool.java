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
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;

public class RequestTool {

    private static RequestTool instance;
    Context context;

    public RequestTool(Context c) {
        context = c;
    }

    static byte[] msg(String dest, String addr, String name) {
        return ("add " + dest + " " + addr + " " + name).getBytes(Charset.forName("UTF-8"));
    }

    synchronized public static RequestTool getInstance(Context context) {
        if (instance == null)
            instance = new RequestTool(context.getApplicationContext());
        return instance;
    }

    public boolean sendUnsentReq(final String dest) {
        log("sendUnsentReq " + dest);
        if (dest == null || "".equals(dest)) {
            log("no unsent 0");
            return false;
        }
        if (!RequestDatabase.getInstance(context).hasOutgoing(dest)) {
            log("no unsent 1");
            return false;
        }
        log("sending unsent");
        return sendRequest(dest);
    }

    public boolean sendRequest(String dest) {

        String addr = Tor.getInstance(context).getID();
        String name = ItemDatabase.getInstance(context).get("name", "", 1).one().json().optString("name");
        String sign = Utils.base64encode(Tor.getInstance(context).sign(msg(dest, addr, name)));
        String pkey = Utils.base64encode(Tor.getInstance(context).pubkey());

        String uri = "http://" + dest + ".onion/f?";
        uri += "dest=" + Uri.encode(dest) + "&";
        uri += "addr=" + Uri.encode(addr) + "&";
        uri += "name=" + Uri.encode(name) + "&";
        uri += "sign=" + Uri.encode(sign) + "&";
        uri += "pkey=" + Uri.encode(pkey);
        log(uri);

        try {
            byte[] rs = HttpClient.getbin(context, uri);
            RequestDatabase.getInstance(context).removeOutgoing(dest);
            return true;
        } catch (IOException ex) {
            return false;
        }

    }

    public void sendAllRequests() {
        Cursor cursor = RequestDatabase.getInstance(context).getOutgoing();
        while (cursor.moveToNext()) {
            String address = cursor.getString(cursor.getColumnIndex("a"));
            sendRequest(address);
        }
        cursor.close();
    }

    private void log(String str) {
        Log.i("RequestTool", str);
    }

    public boolean handleRequest(Uri uri) {

        final String dest = uri.getQueryParameter("dest");
        final String addr = uri.getQueryParameter("addr");
        final String name = uri.getQueryParameter("name");
        final String sign = uri.getQueryParameter("sign");
        final String pkey = uri.getQueryParameter("pkey");

        if (dest == null || addr == null || name == null || sign == null || pkey == null) {
            log("Parameter missing");
            return false;
        }

        if (!dest.equals(Tor.getInstance(context).getID())) {
            log("Wrong destination");
            return false;
        }

        if (!Tor.getInstance(context).checksig(addr, Utils.base64decode(pkey), Utils.base64decode(sign), msg(dest, addr, name))) {
            log("Invalid signature");
            return false;
        }
        log("Signature OK");

        if (ItemDatabase.getInstance(context).hasKey("friend", addr)) {
            log("Already added as friend");
            return false;
        }

        if (!StatusTool.getInstance(context).isOnline(addr)) {
            log("remote hidden service not yet registered");
            throw new RuntimeException("remote hidden service not yet registered");
        }

        if (Settings.getPrefs(context).getBoolean("friendbot", false)) {
            MainActivity.addFriendItem(context, addr, name);
        } else {
            RequestDatabase.getInstance(context).addIncoming(addr, name);
        }

        //new ItemTask(context, addr, "thumb").execute2();
        MainActivity.prefetch(context, addr);
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                }
                //new ItemTask(context, addr, "thumb").execute2();
                MainActivity.prefetch(context, addr);
            }
        }.start();

        return true;

    }

}
