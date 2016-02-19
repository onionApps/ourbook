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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ItemDatabase extends SQLiteOpenHelper {

    private static ItemDatabase instance;
    private Context context;

    public ItemDatabase(Context context) {
        super(context, "db7", null, 1);
        this.context = context.getApplicationContext();
    }

    synchronized public static ItemDatabase getInstance(Context context) {
        if (instance == null)
            instance = new ItemDatabase(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    void log(String str) {
        Log.i("ItemDatabase", str);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items ( itemtype TEXT NOT NULL, itemkey TEXT NOT NULL, itemindex TEXT NOT NULL, itemdata BLOB NOT NULL, PRIMARY KEY(itemtype, itemkey) )");
        db.execSQL("CREATE INDEX itemindex ON items ( itemtype, itemindex )");
    }

    public void delete(String type, String key) {
        getWritableDatabase().delete("items", "itemtype=? AND itemkey=?", new String[]{type, key});
    }

    public void putWithOnConflict(Item item, int onConflict) {
        log("put t:" + item.type() + " k:" + item.key() + " i:" + item.index() + " d:" + item.text());
        ContentValues values = new ContentValues();
        values.put("itemtype", item.type());
        values.put("itemkey", item.key());
        values.put("itemindex", item.index());
        values.put("itemdata", item.data());
        getWritableDatabase().insertWithOnConflict("items", null, values, onConflict);
    }

    public void put(Item item) {
        putWithOnConflict(item, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void putIgnore(Item item) {
        putWithOnConflict(item, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Item getByKey(String type, String key) {

        Cursor cursor = getReadableDatabase().query("items",
                new String[]{"itemtype", "itemkey", "itemindex", "itemdata"},
                "itemtype=? AND itemkey>=?",
                new String[]{type, key},
                null,
                null,
                null,
                "1");

        if (!cursor.moveToNext()) {
            cursor.close();
            return null;
        } else {
            String t = cursor.getString(0);
            String k = cursor.getString(1);
            String i = cursor.getString(2);
            byte[] d = cursor.getBlob(3);
            cursor.close();
            return new Item(t, k, i, d);
        }

    }

    private List<Item> get0(String type, String index, int count) {

        String query = "itemtype=? AND itemindex>=?";

        Cursor cursor = getReadableDatabase().query("items",
                new String[]{"itemtype", "itemkey", "itemindex", "itemdata"},
                query,
                new String[]{type, index},
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

    public ItemResult get(String type, String index, int count) {

        List<Item> l = get0(type, index, count + 1);

        String more = null;
        if (l.size() == count + 1) {
            more = l.get(count).index();
            l = l.subList(0, l.size() - 1);
        }

        ItemResult result = new ItemResult(l, more, true, false);

        return result;

    }

    public String getstr(String key) {
        return get(key, "", 1).onestr(key);
    }

    public boolean hasKey(String type, String key) {

        Cursor cursor = getReadableDatabase().query("items",
                new String[]{"itemkey"},
                "itemtype=? AND itemkey=?",
                new String[]{type, key},
                null,
                null,
                null,
                "1");

        return cursor.moveToNext();

    }

}
