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

import java.util.HashSet;

public class RequestDatabase extends SQLiteOpenHelper {

    private static RequestDatabase instance;
    Context context;
    OnIncomingRequestListener onIncomingRequestListener;
    HashSet<String> declined = new HashSet<>();

    public RequestDatabase(Context context) {
        super(context, "dbrq", null, 1);
        this.context = context.getApplicationContext();
    }

    synchronized public static RequestDatabase getInstance(Context context) {
        if (instance == null)
            instance = new RequestDatabase(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE incoming ( i INTEGER PRIMARY KEY, a TEXT UNIQUE, n TEXT )");
        db.execSQL("CREATE TABLE outgoing ( i INTEGER PRIMARY KEY, a TEXT UNIQUE )");
    }

    public synchronized void removeOutgoing(String address) {
        getWritableDatabase().delete("outgoing", "a=?", new String[]{address});
    }

    public synchronized void addOutgoing(String address) {
        ContentValues v = new ContentValues();
        v.put("a", address);
        getWritableDatabase().insertWithOnConflict("outgoing", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getOutgoing() {
        return getReadableDatabase().query("outgoing", new String[]{"a"}, null, null, null, null, "i DESC");
    }

    public boolean hasOutgoing(String addr) {
        Cursor c = getReadableDatabase().query("outgoing", new String[]{"a"}, "a=?", new String[]{addr}, null, null, "i DESC");
        ;
        boolean b = c.moveToNext();
        c.close();
        return b;
    }

    public synchronized void addIncoming(String address, String name) {
        if (name == null) name = "";

        ContentValues v = new ContentValues();
        v.put("a", address);
        v.put("n", name);
        getWritableDatabase().insertWithOnConflict("incoming", null, v, SQLiteDatabase.CONFLICT_REPLACE);

        OnIncomingRequestListener l = onIncomingRequestListener;
        if (l != null) {
            l.onIncomingRequest(address, name);
        }
    }

    public synchronized boolean removeIncoming(String address) {
        return getWritableDatabase().delete("incoming", "a=?", new String[]{address}) > 0;
    }

    public Cursor getIncoming() {
        return getReadableDatabase().query("incoming", new String[]{"a", "n"}, null, null, null, null, "i DESC");
    }

    public void setOnIncomingRequestListener(OnIncomingRequestListener l) {
        onIncomingRequestListener = l;
    }

    public void addDeclined(String addr) {
        declined.add(addr);
    }

    public boolean isDeclined(String addr) {
        return declined.contains(addr);
    }

    public interface OnIncomingRequestListener {
        void onIncomingRequest(String address, String name);
    }

}
