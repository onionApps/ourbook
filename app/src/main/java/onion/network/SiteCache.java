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

public class SiteCache extends SQLiteOpenHelper {

    private static SiteCache instance;
    private Context context;

    public SiteCache(Context context) {
        super(context, "sitecache", null, 1);
        this.context = context.getApplicationContext();
    }

    synchronized public static SiteCache getInstance(Context context) {
        if (instance == null)
            instance = new SiteCache(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sitecache ( k TEXT NOT NULL PRIMARY KEY, v TEXT NOT NULL )");
    }

    public void clearCache() {
        getWritableDatabase().delete("sitecache", null, null);
        getWritableDatabase().execSQL("VACUUM");
    }

    public void put(String key, String value) {
        ContentValues data = new ContentValues();
        data.put("k", key);
        data.put("v", value);
        getWritableDatabase().insertWithOnConflict("sitecache", null, data, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String get(String key) {
        Cursor cursor = getReadableDatabase().query("sitecache", new String[]{"v"}, "k=?", new String[]{key}, null, null, null);
        if (!cursor.moveToNext()) {
            return null;
        }
        return cursor.getString(0);
    }

    public boolean has(String key) {
        return get(key) != null;
    }

}