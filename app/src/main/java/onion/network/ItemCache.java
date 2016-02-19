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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ItemCache extends SQLiteOpenHelper {

    private static ItemCache instance;

    public ItemCache(Context context) {
        super(context, "itemcache1", null, 1);
    }

    synchronized public static ItemCache getInstance(Context context) {
        if (instance == null)
            instance = new ItemCache(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE itemcache ( address TEXT NOT NULL, itemtype TEXT NOT NULL, itemkey TEXT NOT NULL, itemindex TEXT NOT NULL, itemdata BLOB NOT NULL, PRIMARY KEY(address, itemtype, itemindex) )");
    }

    public void delete(String address, String type, String begin, String end) {
        if (end != null) {
            getWritableDatabase().delete("itemcache", "address=? AND itemtype=? AND itemindex>=? AND itemindex<?", new String[]{address, type, begin, end});
        } else {
            getWritableDatabase().delete("itemcache", "address=? AND itemtype=? AND itemindex>=?", new String[]{address, type, begin});
        }
    }

    public void put(String address, Item item) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("itemtype", item.type());
        values.put("itemkey", item.key());
        values.put("itemindex", item.index());
        values.put("itemdata", item.data());
        getWritableDatabase().insertWithOnConflict("itemcache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void clearCache() {
        getWritableDatabase().delete("itemcache", null, null);
        getWritableDatabase().execSQL("VACUUM");
    }

    private List<Item> get0(String address, String type, String index, int count) {
        Cursor cursor = getReadableDatabase().query("itemcache",
                new String[]{"itemtype", "itemkey", "itemindex", "itemdata"},
                "address=? AND itemtype=? AND itemindex>=?",
                new String[]{address, type, index},
                null,
                null,
                "itemindex",
                "" + count);
        ArrayList<Item> ret = new ArrayList<>();
        while (cursor.moveToNext()) {
            String t = cursor.getString(0);
            String k = cursor.getString(1);
            String i = cursor.getString(2);
            byte[] d = cursor.getBlob(3);
            ret.add(new Item(t, k, i, d));
        }
        return ret;
    }

    public ItemResult get(String address, String type, String index, int count) {
        List<Item> l = get0(address, type, index, count + 1);
        String more = null;
        if (l.size() == count + 1) {
            more = l.get(count).index();
            l = l.subList(0, l.size() - 1);
        }
        ItemResult result = new ItemResult(l, more, true, false);
        return result;
    }

    public ItemResult get(String address, String type) {
        return get(address, type, "", 1);
    }

}
