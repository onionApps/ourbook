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
import java.util.HashSet;

public class ChatClient {

    private static ChatClient instance;
    ChatDatabase chatDatabase;
    private String TAG = "chatclient";
    private Context context;
    private Tor tor;
    private HashSet<OnMessageSentListener> listeners = new HashSet<OnMessageSentListener>();

    public ChatClient(Context context) {
        this.context = context;
        this.tor = Tor.getInstance(context);
        this.chatDatabase = ChatDatabase.getInstance(context);
    }

    synchronized public static ChatClient getInstance(Context context) {
        if (instance == null)
            instance = new ChatClient(context.getApplicationContext());
        return instance;
    }

    private void log(String s) {
        Log.i(TAG, s);
    }

    private boolean sendOne(Cursor cursor) {
        String sender = cursor.getString(cursor.getColumnIndexOrThrow("sender"));
        String receiver = cursor.getString(cursor.getColumnIndexOrThrow("receiver"));
        String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
        long time = cursor.getLong(cursor.getColumnIndexOrThrow("time"));
        return sendOne(sender, receiver, content, time);
    }

    public Uri makeUri(String sender, String receiver, String content, long time) {

        content = Utils.base64encode(content.getBytes(Utils.utf8));

        String sig = Utils.base64encode(tor.sign((receiver + " " + sender + " " + time + " " + content).getBytes(Utils.utf8)));

        String name = ItemDatabase.getInstance(context).getstr("name");
        if (name == null) name = "";

        String uri = "http://" + receiver + ".onion/m?";
        uri += "a=" + Uri.encode(sender) + "&";
        uri += "b=" + Uri.encode(receiver) + "&";
        uri += "t=" + Uri.encode("" + time) + "&";
        uri += "m=" + Uri.encode(content) + "&";
        uri += "p=" + Uri.encode(Utils.base64encode(tor.pubkey())) + "&";
        uri += "s=" + Uri.encode(sig) + "&";
        uri += "n=" + Uri.encode(name);

        return Uri.parse(uri);
    }

    public boolean sendOne(String sender, String receiver, String content, long time) {

        Uri uri = makeUri(sender, receiver, content, time);
        log("" + uri);

        try {
            boolean rs = HttpClient.get(context, uri).trim().equals("1");
            if (!rs) {
                log("declined");
                return false;
            }
        } catch (IOException ex) {
            log("exception");
            log("" + ex.getMessage());
            ex.printStackTrace();
            return false;
        }

        log("message sent");

        chatDatabase.touch(sender, receiver, time);

        callOnMessageSentListeners();

        return true;

    }

    private void sendAll(Cursor cursor) {
        while (cursor.moveToNext()) {
            sendOne(cursor);
        }
        cursor.close();
    }

    public boolean hasUnsent(String address) {
        Cursor c = chatDatabase.getUnsent(address);
        boolean ret = c.moveToNext();
        c.close();
        return ret;
    }

    public void sendUnsent() {
        sendAll(chatDatabase.getUnsent());
    }

    public void sendUnsent(String address) {
        sendAll(chatDatabase.getUnsent(address));
    }

    synchronized public void addOnMessageSentListener(OnMessageSentListener l) {
        listeners.add(l);
    }

    synchronized public void removeOnMessageSentListener(OnMessageSentListener l) {
        listeners.remove(l);
    }

    synchronized private void callOnMessageSentListeners() {
        for (OnMessageSentListener l : listeners) {
            l.onMessageSent();
        }
    }

    public interface OnMessageSentListener {
        void onMessageSent();
    }

}
