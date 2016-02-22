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

import java.nio.charset.Charset;
import java.util.HashSet;

public class ChatServer {

    private static ChatServer instance;
    String TAG = "chatserver";
    ChatDatabase chatDatabase;
    private Context context;
    private Tor tor;
    private HashSet<OnMessageReceivedListener> listeners = new HashSet<OnMessageReceivedListener>();

    public ChatServer(Context context) {
        this.context = context;
        this.tor = Tor.getInstance(context);
        this.chatDatabase = ChatDatabase.getInstance(context);
    }

    synchronized public static ChatServer getInstance(Context context) {
        if (instance == null)
            instance = new ChatServer(context.getApplicationContext());
        return instance;
    }

    void log(String s) {
        Log.i(TAG, s);
    }


    public boolean handle(Uri uri) {

        log("handle " + uri);


        // get & check params

        final String sender = uri.getQueryParameter("a");
        final String receiver = uri.getQueryParameter("b");
        final String time = uri.getQueryParameter("t");
        String m = uri.getQueryParameter("m");
        final String pubkey = uri.getQueryParameter("p");
        final String signature = uri.getQueryParameter("s");
        final String name = uri.getQueryParameter("n") != null ? uri.getQueryParameter("n") : "";

        if (!receiver.equals(tor.getID())) {
            log("message wrong address");
            return false;
        }
        log("message address ok");

        if (!tor.checksig(
                sender,
                Utils.base64decode(pubkey),
                Utils.base64decode(signature),
                (receiver + " " + sender + " " + time + " " + m).getBytes(Charset.forName("UTF-8")))) {
            log("message invalid signature");
            return false;
        }
        log("message signature ok");

        final String content = new String(Utils.base64decode(m), Charset.forName("UTF-8"));

        final long ltime;
        try {
            ltime = Long.parseLong(time);
        } catch (Exception ex) {
            log("failed to parse time");
            return false;
        }


        // get chat mode

        boolean acceptMessage = true;
        String acceptmessages = Settings.getPrefs(context).getString("acceptmessages", "");
        if ("none".equals(acceptmessages)) {
            log("accept none. message blocked.");
            acceptMessage = false;
        }
        if ("friends".equals(acceptmessages)) {
            log("friends only");
            if (!ItemDatabase.getInstance(context).hasKey("friend", sender)) {
                log("not a friend. message blocked.");
                if (!RequestDatabase.getInstance(context).isDeclined(sender)) {
                    RequestDatabase.getInstance(context).addIncoming(sender, name);
                    log("friend request added.");
                } else {
                    log("friend request already declined. message to auto friend request blocked.");
                }
                acceptMessage = false;
            }
        }


        // handle message

        if (acceptMessage) {
            chatDatabase.addMessage(sender, receiver, content, ltime, true, false);
            callOnMessageReceivedListeners();
            Notifier.getInstance(context).msg(sender);
        }

        ChatBot chatBot = ChatBot.getInstance(context);
        if (chatBot.addr() != null) {
            acceptMessage = chatBot.handle(sender, name, content, ltime, acceptMessage);
        }


        log("message received");

        return acceptMessage;
    }


    synchronized public void addOnMessageReceivedListener(OnMessageReceivedListener l) {
        listeners.add(l);
    }

    synchronized public void removeOnMessageReceivedListener(OnMessageReceivedListener l) {
        listeners.remove(l);
    }

    synchronized private void callOnMessageReceivedListeners() {
        for (OnMessageReceivedListener l : listeners) {
            l.onMessageReceived();
        }
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived();
    }

}
