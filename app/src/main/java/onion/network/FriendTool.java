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
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;

public class FriendTool {

    private static FriendTool instance;
    Context context;

    public FriendTool(Context c) {
        context = c;
    }

    synchronized public static FriendTool getInstance(Context context) {
        if (instance == null)
            instance = new FriendTool(context.getApplicationContext());
        return instance;
    }

    static byte[] buildUnfriendMessage(String dest, String addr) {
        return ("unfriend " + dest + " " + addr).getBytes(Charset.forName("UTF-8"));
    }

    private void log(String str) {
        Log.i("FriendTool", str);
    }

    public boolean handleUnfriend(Uri uri) {

        String dest = uri.getQueryParameter("dest");
        String addr = uri.getQueryParameter("addr");
        String sign = uri.getQueryParameter("sign");
        String pkey = uri.getQueryParameter("pkey");

        if (dest == null || addr == null || sign == null || pkey == null) {
            log("Parameter missing");
            return false;
        }

        if (!dest.equals(Tor.getInstance(context).getID())) {
            log("Wrong destination");
            return false;
        }

        if (!Tor.getInstance(context).checksig(addr, Utils.base64decode(pkey), Utils.base64decode(sign), buildUnfriendMessage(dest, addr))) {
            log("Invalid signature");
            return false;
        }
        log("Signature OK");

        ItemDatabase.getInstance(context).delete("friend", addr);

        return true;

    }

    public boolean doSendUnfriend(String dest) {

        String addr = Tor.getInstance(context).getID();

        String sign = Utils.base64encode(Tor.getInstance(context).sign(buildUnfriendMessage(dest, addr)));

        String pkey = Utils.base64encode(Tor.getInstance(context).pubkey());

        String uri = "http://" + dest + ".onion/u?";
        uri += "dest=" + Uri.encode(dest) + "&";
        uri += "addr=" + Uri.encode(addr) + "&";
        uri += "sign=" + Uri.encode(sign) + "&";
        uri += "pkey=" + Uri.encode(pkey);

        log(uri);

        try {
            byte[] rs = HttpClient.getbin(context, uri);
            log("sendUnfriend OK");
            return true;
        } catch (IOException ex) {
            log("sendUnfriend err");
            return false;
        }

    }

    public void startSendUnfriend(final String dest) {
        new Thread() {
            @Override
            public void run() {
                doSendUnfriend(dest);
            }
        }.start();
    }

    public void unfriend(String address) {
        RequestDatabase.getInstance(context).removeOutgoing(address);
        RequestDatabase.getInstance(context).removeIncoming(address);
        ItemDatabase.getInstance(context).delete("friend", address);
        FriendTool.getInstance(context).startSendUnfriend(address);
    }

}
