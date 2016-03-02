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
import android.support.v4.util.LruCache;
import android.util.Log;

import org.apache.commons.codec.binary.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ItemCache {

    private Context context;
    private static ItemCache instance;
    private ItemDiskCache diskCache;
    private ItemCacheMem memCache;

    synchronized public static ItemCache getInstance(Context context) {
        if (instance == null)
            instance = new ItemCache(context.getApplicationContext());
        return instance;
    }

    public ItemCache(Context context) {
        this.context = context;
        diskCache = new ItemDiskCache(context);
        memCache = new ItemCacheMem();
    }



    private static String TAG = "ItemCache";
    private static void log(String s) {
        //Log.i(TAG, s);
    }


    public boolean isDiskCacheEnabled() {
        return Settings.getPrefs(context).getBoolean("diskcache", false);
    }


    public synchronized ItemResult get(String address, String type) {
        return get(address, type, "", 1);
    }

    public synchronized ItemResult get(String address, String type, String index, int count) {

        {
            Item memItem = memCache.get(address, type, index, count);
            if(memItem != null) {
                ArrayList<Item> l = new ArrayList<>();
                l.add(memItem);
                return new ItemResult(l, null, true, false);
            }
        }

        if(isDiskCacheEnabled()) {
            ItemResult rs = diskCache.get(address, type, index, count);
            if (count == 1 && rs.size() == 1 && "".equals(index) && "".equals(rs.one().key()) && "".equals(rs.one().index())) {
                memCache.put(address, rs.one());
            }
            return rs;
        }

        return new ItemResult(new ArrayList<Item>(), null, true, false);
    }

    public synchronized void delete(String address, String type, String begin, String end) {
        diskCache.delete(address, type, begin, end);
        memCache.delete(address, type);
    }

    public synchronized void put(String address, Item item) {
        if(isDiskCacheEnabled()) {
            diskCache.put(address, item);
        }
        memCache.put(address, item);
    }

    public synchronized void clearCache() {
        diskCache.clearCache();
        memCache.clearCache();
    }




    private static class ItemCacheMem {

        LruCache<String, Item> cache = new LruCache<String, Item>(1024 * 256) {
            @Override
            protected int sizeOf(String key, Item item) {
                return (key.length() + item.type().length()) * 2 + item.data().length;
            }
        };

        public synchronized void put(String address, Item item) {
            String key = "" + address + "/" + item.type();
            if("".equals(item.key()) &&
                    "".equals(item.index()) &&
                    Utils.isAlnum(address) &&
                    Utils.isAlnum(item.type()) &&
                    item.data() != null &&
                    item.data().length < 10000) {
                log("memcache put " + key);
                cache.put(key, item);
            } else {
                log("memcache put del " + key);
                cache.remove(key);
            }
        }

        public synchronized Item get(String address, String type, String index, int count) {
            if(!Utils.isAlnum(address)) return null;
            if(!Utils.isAlnum(type)) return null;
            if(!"".equals(index)) return null;
            if(count != 1) return null;
            String key = address + "/" + type;
            Item item = cache.get(key);
            if(item != null) {
                log("memcache found " + key);
            } else {
                log("memcache not found " + key);
            }
            return item;
        }

        public synchronized void delete(String address, String type) {
            if(!Utils.isAlnum(address)) return;
            if(!Utils.isAlnum(type)) return;
            String key = address + "/" + type;
            log("memcache delete " + key);
            cache.remove(key);
        }

        public synchronized void clearCache() {
            log("memcache clear");
            cache.evictAll();
        }

    }




    private static class ItemDiskCache extends SQLiteOpenHelper  {

        public ItemDiskCache(Context context) {
            super(context, "itemcache1", null, 1);
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

    }



}
