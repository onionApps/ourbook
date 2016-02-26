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

public class ChatDatabase extends SQLiteOpenHelper {

    private static ChatDatabase instance;
    String TAG = "chatdb";
    private Context context;

    public ChatDatabase(Context context) {
        super(context, "cdb4", null, 1);
        this.context = context;
    }

    synchronized public static ChatDatabase getInstance(Context context) {
        if (instance == null)
            instance = new ChatDatabase(context.getApplicationContext());
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE messages ( _id INTEGER PRIMARY KEY, sender TEXT NOT NULL, receiver TEXT NOT NULL, content TEXT NOT NULL, time INTEGER NOT NULL, incoming INTEGER NOT NULL, outgoing INTEGER NOT NULL, UNIQUE(sender, receiver, time) )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        Cursor c = db.rawQuery("PRAGMA secure_delete = true", null);
        while(c.moveToNext()) {
            for(int i = 0; i < c.getColumnCount(); i++) {
                Log.i("secure_delete", "" + c.getColumnName(i) + " " + c.getString(i));
            }
        }
    }

    public synchronized void markRead(String address) {
        ContentValues v = new ContentValues();
        v.put("incoming", false);
        getWritableDatabase().update("messages", v, "sender=? AND incoming=1", new String[]{address});
    }

    public synchronized void addMessage(String sender, String receiver, String content, long time, boolean incoming, boolean outgoing) {
        ContentValues v = new ContentValues();
        v.put("sender", sender);
        v.put("receiver", receiver);
        v.put("content", content);
        v.put("time", time);
        v.put("incoming", incoming);
        v.put("outgoing", outgoing);
        getWritableDatabase().insertWithOnConflict("messages", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Cursor getMessages(String a) {
        String b = Tor.getInstance(context).getID();
        Log.i(TAG, "a " + a);
        Log.i(TAG, "b " + b);
        return getReadableDatabase().rawQuery("SELECT * FROM (SELECT * FROM messages WHERE ((sender=? AND receiver=?) OR (sender=? AND receiver=?)) ORDER BY _id DESC LIMIT 64) ORDER BY _id ASC", new String[]{a, b, b, a});
    }

    public synchronized void clearChat(String a) {
        Log.i(TAG, "clearChat");
        String b = Tor.getInstance(context).getID();
        Log.i(TAG, "a " + a);
        Log.i(TAG, "b " + b);
        getWritableDatabase().delete("messages", "((sender=? AND receiver=?) OR (sender=? AND receiver=?))", new String[]{a, b, b, a});
    }

    public Cursor getUnsent(String b) {
        String a = Tor.getInstance(context).getID();
        return getReadableDatabase().query("messages", null, "sender=? AND receiver=? AND outgoing=1", new String[]{a, b}, null, null, null, null);
    }

    public Cursor getUnsent() {
        String a = Tor.getInstance(context).getID();
        return getReadableDatabase().query("messages", null, "sender=? AND outgoing=1", new String[]{a}, null, null, null, null);
    }

    public synchronized void touch(String sender, String receiver, long time) {
        ContentValues v = new ContentValues();
        v.put("incoming", false);
        v.put("outgoing", false);
        getWritableDatabase().update("messages", v, "sender=? AND receiver=? AND time=?", new String[]{sender, receiver, "" + time});
    }

    public Cursor getConversations() {
        return getReadableDatabase().rawQuery("SELECT * FROM (SELECT * FROM (SELECT * FROM messages ORDER BY time ASC) GROUP BY MIN(sender, receiver), MAX(sender, receiver) ) ORDER BY incoming DESC, time DESC", new String[]{});
    }

    public synchronized boolean abortPendingMessage(long id) {
        return getWritableDatabase().delete("messages", "_id=? AND outgoing=1", new String[]{"" + id}) > 0;
    }


    public int getIncomingConversationCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM messages WHERE incoming=1 GROUP BY MIN(sender, receiver), MAX(sender, receiver)", new String[]{});
        int n = c.getCount();
        c.close();
        return n;
    }

    public int getIncomingMessageCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM messages WHERE incoming=1", new String[]{});
        int n = c.getCount();
        c.close();
        return n;
    }

    public int getIncomingMessageCount(String addr) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM messages WHERE incoming=1 AND sender=?", new String[]{addr});
        int n = c.getCount();
        c.close();
        return n;
    }

}
