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
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class ItemTask extends AsyncTask<Void, ItemResult, ItemResult> {

    private static Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    };
    ItemDatabase database;
    Context context;
    String address;
    String type;
    String index;
    int count;
    String TAG = "ItemTask";

    public ItemTask(Context context, String address, String type) {
        this(context, address, type, "", 1);
    }

    public ItemTask(Context context, String address, String type, String index, int count) {
        if (address == null) address = "";
        if (type == null) type = "";
        if (index == null) index = "";
        this.context = context;
        this.address = address;
        this.type = type;
        this.index = index;
        this.count = count;
        this.database = ItemDatabase.getInstance(context);
    }

    private void publishLoading() {
        publishProgress(new ItemResult(new ArrayList<Item>(), null, false, true));
    }

    ItemResult process(byte[] data, boolean ok, boolean loading) {

        try {

            String str = new String(data, Charset.forName("UTF-8"));
            Log.i(TAG, "doInBackground: " + str);
            ArrayList<Item> items = new ArrayList<Item>();
            JSONObject o = new JSONObject(str);
            JSONArray ii = o.getJSONArray("items");
            for (int i = 0; i < ii.length(); i++) {
                JSONObject oo = ii.getJSONObject(i);
                items.add(new Item(type, oo.getString("k"), oo.getString("i"), oo.getJSONObject("d")));
            }
            String more = o.optString("more", null);
            return new ItemResult(items, more, ok, loading);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    public String getUrl() {
        return "http://" + address + ".onion/a?t=" + type + "&i=" + index + "&n=" + (count + 1);
    }

    @Override
    protected ItemResult doInBackground(Void... params) {

        if (address == null || address.equals(Tor.getInstance(context).getID())) {
            address = "";
        }

        publishLoading();

        if (address.isEmpty()) {

            // local request

            ItemResult result = database.get(type, index, count);


            try {
                Thread.sleep(50);
            } catch (Exception ex) {
            }


            publishProgress(result);

            return result;

        } else {


            /*
            // remote request

            ItemCache itemCache = ItemCache.getInstance(context);

            // view current cache contents
            {
                ItemResult itemResult = itemCache.get(address, type, index, count);
                itemResult.setLoading(true);
                itemResult.setOk(true);
                publishProgress(itemResult);
            }

            // try to load and view real data
            boolean loadOk = false;
            try {
                String url = getUrl();
                byte[] data = HttpClient.getbin(context, url);
                ItemResult itemResult = process(data, true, false);
                if (itemResult != null) {
                    itemCache.delete(address, type, index, itemResult.more());
                    for (int i = 0; i < itemResult.size(); i++) {
                        itemCache.put(address, itemResult.at(i));
                    }
                    loadOk = true;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // publish final result
            {
                ItemResult itemResult = itemCache.get(address, type, index, count);
                itemResult.setLoading(false);
                itemResult.setOk(loadOk);
                publishProgress(itemResult);
                return itemResult;
            }
            */




            // remote request

            ItemCache itemCache = ItemCache.getInstance(context);

            ItemResult finalResult = null;

            // view current cache contents
            {
                ItemResult itemResult = itemCache.get(address, type, index, count);
                itemResult.setLoading(true);
                itemResult.setOk(true);
                publishProgress(itemResult);
                finalResult = itemResult;
            }

            // try to load and view real data
            boolean loadOk = false;
            try {
                String url = getUrl();
                byte[] data = HttpClient.getbin(context, url);
                ItemResult itemResult = process(data, true, false);
                if (itemResult != null) {
                    itemCache.delete(address, type, index, itemResult.more());
                    for (int i = 0; i < itemResult.size(); i++) {
                        itemCache.put(address, itemResult.at(i));
                    }
                    loadOk = true;
                    finalResult = itemResult;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // publish final result
            {
                //ItemResult itemResult = itemCache.get(address, type, index, count);
                ItemResult itemResult = finalResult;
                itemResult.setLoading(false);
                itemResult.setOk(loadOk);
                publishProgress(itemResult);
                return itemResult;
            }


        }

    }

    public void execute2() {
        //executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        executeOnExecutor(executor);
    }

}
