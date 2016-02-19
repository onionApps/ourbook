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

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class WallBot {

    private static WallBot instance;
    String TAG = "WallBot";
    Context context;
    Timer timer;

    public WallBot(Context context) {
        this.context = context;
        init();
    }

    synchronized public static WallBot getInstance(Context context) {
        if (instance == null)
            instance = new WallBot(context.getApplicationContext());
        return instance;
    }

    void log(String s) {
        Log.i(TAG, s);
    }

    public void init() {

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }


        if (Settings.getPrefs(context).getBoolean("wallbot", false)) {

            long intervalSeconds = 60;
            try {
                intervalSeconds = Long.parseLong(Settings.getPrefs(context).getString("wallbotinterval", "60"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            log("intervalSeconds " + intervalSeconds);

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                update();
                            } catch (Exception ex) {
                                log(ex.toString());
                            }
                        }
                    }.start();
                }
            }, 0, 1000 * intervalSeconds);

        }

    }

    private String[] fetchRss(String addr) {

        try {
            byte[] data;

            try {
                data = HttpClient.getext(context, Uri.parse(addr));
                log(new String(data));
            } catch (IOException ex) {
                log("feed error");
                log(ex.toString());
                return null;
            }

            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = documentBuilder.parse(new ByteArrayInputStream(data));

            ArrayList<String> ret = new ArrayList<>();

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Node itemNode = items.item(i);
                if (!(itemNode instanceof Element)) {
                    continue;
                }

                Element item = (Element) itemNode;

                String title = getTag(item, "title");
                String link = getTag(item, "link");

                if (title == null) continue;

                String post = title.trim();
                if (link != null) {
                    Uri uri = Uri.parse(link);
                    if (uri != null) {
                        if (uri.getQueryParameter("url") != null) {
                            link = uri.getQueryParameter("url");
                        }
                    }
                    post += "\n" + link.trim();
                }

                ret.add(post);
            }

            return ret.toArray(new String[ret.size()]);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private void update() throws Exception {
        log("update");

        ItemDatabase db = ItemDatabase.getInstance(context);

        String[] ss = fetchRss(Settings.getPrefs(context).getString("wallbotfeed", ""));

        if (ss == null) return;

        log(ss.length + " items");

        if (ss.length == 0) return;

        String[] ids = new String[ss.length];
        String[] delkeys = new String[ss.length];
        for (int i = 0; i < ss.length; i++) {
            ids[i] = Utils.digest(ss[i]);
            delkeys[i] = "bot" + ids[i];
        }

        String q = "itemtype='post' AND itemkey LIKE 'bot%'";
        for (String id : ids) {
            q += " AND itemkey != ?";
        }
        int n = db.getWritableDatabase().delete("items", q, delkeys);
        log("del " + n);

        for (int si = 0; si < ss.length; si++) {

            long k = System.currentTimeMillis();
            long i = 100000000000000l - k;

            JSONObject o = new JSONObject();
            o.put("text", ss[si]);
            o.put("date", k);

            db.putIgnore(new Item("post", "bot" + ids[si], "" + i, o));

        }

        log("ready");
    }

    String getTag(Element tag, String name) {
        NodeList l = tag.getElementsByTagName(name);
        if (l == null || l.getLength() == 0) return null;
        if (!(l.item(0) instanceof Element)) return null;
        return ((Element) l.item(0)).getTextContent();
    }

}
