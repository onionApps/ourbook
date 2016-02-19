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

public class ChatBot {

    private static ChatBot instance;
    String TAG = "ChatBot";
    Context context;

    public ChatBot(Context context) {
        this.context = context;
    }

    synchronized public static ChatBot getInstance(Context context) {
        if (instance == null)
            instance = new ChatBot(context.getApplicationContext());
        return instance;
    }

    void log(String s) {
        Log.i(TAG, s);
    }

    public String addr() {
        if (!Settings.getPrefs(context).getBoolean("chatbot", false)) {
            return null;
        }
        String addr = Settings.getPrefs(context).getString("chatbotaddr", "");
        if (addr == null || addr.trim().isEmpty()) {
            return null;
        }
        return addr;
    }


    public boolean handle(final String address, final String name, final String message, final long time, final boolean saveResponse) {

        final String addr = addr();
        if (addr == null) return false;

        if (!StatusTool.getInstance(context).isOnline(address)) {
            log("chat partner offline");
            return false;
        }

        new Thread() {
            @Override
            public void run() {

                // get chat response from chatbot
                final Uri botUri = Uri.parse(addr).buildUpon()
                        .appendQueryParameter("a", address)
                        .appendQueryParameter("n", name)
                        .appendQueryParameter("t", "" + time)
                        .appendQueryParameter("m", message)
                        .build();
                log("" + botUri);
                final String response;
                try {
                    response = HttpClient.getNoTor(context, botUri);
                } catch (IOException ex) {
                    log("failed to get response from bot");
                    ex.printStackTrace();
                    return;
                }

                // send message
                String respstr = null;
                try {
                    respstr = HttpClient.get(context, ChatClient.getInstance(context).makeUri(
                            Tor.getInstance(context).getID(),
                            address,
                            response,
                            Math.max(time + 100, System.currentTimeMillis()
                            )));
                } catch (IOException ex) {
                    log("failed to send response message");
                }

                // read response
                boolean sendSuccess = "1".equals(respstr);

                // handle response
                if (sendSuccess) {
                    log("response sent");
                    if (saveResponse) {
                        long t = Math.max(time + 100, System.currentTimeMillis());
                        ChatDatabase.getInstance(context).addMessage(
                                Tor.getInstance(context).getID(),
                                address,
                                response,
                                t,
                                false,
                                false
                        );
                        log("response saved to db");
                    }
                } else {
                    log("failed to send response");
                }

                //return sendSuccess;
            }
        }.start();

        return true;

    }

}
