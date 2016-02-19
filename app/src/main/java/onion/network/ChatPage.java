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

import android.database.Cursor;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

public class ChatPage extends BasePage implements ChatClient.OnMessageSentListener, ChatServer.OnMessageReceivedListener {

    String TAG = "chat";
    ChatAdapter adapter;
    ChatDatabase chatDatabase;
    Tor tor;
    Cursor cursor;
    RecyclerView recycler;
    ChatServer chatServer;
    ChatClient chatClient;
    Timer timer;
    long idLastLast = -1;
    Item nameItem = new Item();

    public ChatPage(final MainActivity activity) {
        super(activity);

        chatDatabase = ChatDatabase.getInstance(activity);
        chatServer = ChatServer.getInstance(activity);
        chatClient = ChatClient.getInstance(activity);
        tor = Tor.getInstance(activity);

        inflate(activity, R.layout.chat_page, this);

        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new ChatAdapter();
        recycler.setAdapter(adapter);

        final View send = findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sender = tor.getID();
                if (sender == null || sender.trim().equals("")) {
                    sendPendingAndUpdate();
                    Log.i(TAG, "no sender id");
                    return;
                }

                String message = ((EditText) findViewById(R.id.editmessage)).getText().toString();
                message = message.trim();
                if (message.equals("")) return;

                Log.i(TAG, "sender " + sender);
                Log.i(TAG, "address " + address);
                Log.i(TAG, "message " + message);
                chatDatabase.addMessage(sender, address, message, System.currentTimeMillis(), false, true);
                Log.i(TAG, "sent");

                ((EditText) findViewById(R.id.editmessage)).setText("");

                sendPendingAndUpdate();

                recycler.smoothScrollToPosition(Math.max(0, cursor.getCount() - 1));

                UpdateScheduler.getInstance(context).put(address);
            }
        });

        //load();

        sendPendingAndUpdate();
    }

    void log(String s) {
        Log.i(TAG, s);
    }

    @Override
    public void onNameItem(Item item) {
        nameItem = item;
        load();

        findViewById(R.id.offline).setVisibility(!activity.nameItemResult.ok() && !activity.nameItemResult.loading() ? View.VISIBLE : View.GONE);
        findViewById(R.id.loading).setVisibility(activity.nameItemResult.loading() ? View.VISIBLE : View.GONE);
    }

    @Override
    public String getTitle() {
        return "Chat";
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_question_answer_white_36dp;
        //return R.drawable.ic_mail_outline_white_36dp;
    }

    void sendPendingAndUpdate() {

        new Thread() {
            @Override
            public void run() {
                sendUnsent();
                post(new Runnable() {
                    @Override
                    public void run() {
                        load();
                    }
                });
            }
        }.start();

        load();

    }

    synchronized void sendUnsent() {
        chatClient.sendUnsent(address);
    }

    @Override
    public void onResume() {
        super.onResume();
        chatServer.addOnMessageReceivedListener(this);
        chatClient.addOnMessageSentListener(this);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                log("send unsent");
                sendUnsent();
                //RequestTool.getInstance(context).
            }
        }, 0, 1000 * 60);
    }

    @Override
    public void onPause() {
        timer.cancel();
        timer.purge();
        timer = null;
        chatServer.removeOnMessageReceivedListener(this);
        chatClient.removeOnMessageSentListener(this);
        super.onPause();
    }

    @Override
    public void onMessageSent() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                load();
                activity.initTabs();
            }
        });
    }

    @Override
    public void onMessageReceived() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                load();
                activity.initTabs();
            }
        });
    }

    @Override
    public void onTabSelected() {
        log("onTabSelected");
        if (isPageShown()) {
            load();
        }
    }

    void markReadIfVisible() {
        log("markReadIfVisible");
        if (isPageShown()) {
            log("markRead");
            chatDatabase.markRead(address);
        }
    }

    @Override
    public String getBadge() {
        int n = chatDatabase.getIncomingMessageCount(activity.address);
        if (n <= 0) return "";
        if (n >= 10) return "X";
        return "" + n;
    }

    @Override
    public void load() {

        log("load");

        boolean nochat = nameItem.json().optBoolean("nochat");
        findViewById(R.id.no_msg).setVisibility(nochat ? View.VISIBLE : View.GONE);

        String acceptmessages = Settings.getPrefs(context).getString("acceptmessages", "");
        findViewById(R.id.msg_from_none).setVisibility(!nochat && "none".equals(acceptmessages) ? View.VISIBLE : View.GONE);
        findViewById(R.id.msg_from_friends).setVisibility(!nochat && "friends".equals(acceptmessages) && !itemDatabase.hasKey("friend", address) ? View.VISIBLE : View.GONE);

        Cursor oldCursor = cursor;

        markReadIfVisible();

        cursor = chatDatabase.getMessages(address);
        if (oldCursor != null)
            oldCursor.close();
        adapter.notifyDataSetChanged();

        cursor.moveToLast();
        long idLast = -1;
        int i = cursor.getColumnIndex("_id");
        if (i >= 0 && cursor.getCount() > 0) {
            idLast = cursor.getLong(i);
        }
        if (idLast != idLastLast) {
            idLastLast = idLast;
            if (oldCursor == null || oldCursor.getCount() == 0)
                recycler.scrollToPosition(Math.max(0, cursor.getCount() - 1));
            else
                recycler.smoothScrollToPosition(Math.max(0, cursor.getCount() - 1));
        }

        activity.initTabs();

    }

    @Override
    public String getPageIDString() {
        return "chat";
    }

    class ChatHolder extends RecyclerView.ViewHolder {
        public TextView message, time, status;
        public View left, right;
        public CardView card;
        public View abort;

        public ChatHolder(View v) {
            super(v);
            message = (TextView) v.findViewById(R.id.message);
            time = (TextView) v.findViewById(R.id.time);
            status = (TextView) v.findViewById(R.id.status);
            left = v.findViewById(R.id.left);
            right = v.findViewById(R.id.right);
            card = (CardView) v.findViewById(R.id.card);
            abort = v.findViewById(R.id.abort);
        }
    }

    class ChatAdapter extends RecyclerView.Adapter<ChatHolder> {

        @Override
        public ChatHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ChatHolder(activity.getLayoutInflater().inflate(R.layout.chat_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ChatHolder holder, int position) {
            if (cursor == null) return;

            cursor.moveToFirst();
            cursor.moveToPosition(position);

            final long id = cursor.getLong(cursor.getColumnIndex("_id"));
            String content = cursor.getString(cursor.getColumnIndex("content"));
            String sender = cursor.getString(cursor.getColumnIndex("sender"));
            String time = Utils.date(cursor.getString(cursor.getColumnIndex("time")));
            boolean pending = cursor.getInt(cursor.getColumnIndex("outgoing")) > 0;
            boolean tx = sender.equals(tor.getID());

            if (sender.equals(tor.getID())) sender = "You";

            if (tx) {
                holder.left.setVisibility(View.VISIBLE);
                holder.right.setVisibility(View.GONE);
            } else {
                holder.left.setVisibility(View.GONE);
                holder.right.setVisibility(View.VISIBLE);
            }

            if (pending)
                holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
            else
                holder.card.setCardElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()));

            String status = "";
            if (sender.equals(address)) {
                if (activity.name.isEmpty())
                    status = address;
                else
                    status = activity.name;
            } else {
                if (pending) {
                    status = "Pending...";
                } else {
                    status = "Sent";
                }
            }


            if (pending) {
                holder.abort.setVisibility(View.VISIBLE);
                holder.abort.setClickable(true);
                holder.abort.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean ok = chatDatabase.abortPendingMessage(id);
                        load();
                        Toast.makeText(activity, ok ? "Pending message aborted." : "Error: Message already sent.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.abort.setVisibility(View.GONE);
                holder.abort.setClickable(false);
                holder.abort.setOnClickListener(null);
            }

            int color = pending ? 0xff000000 : 0xff888888;
            holder.time.setTextColor(color);
            holder.status.setTextColor(color);

            holder.message.setMovementMethod(LinkMovementMethod.getInstance());
            holder.message.setText(Utils.linkify(context, content));

            holder.time.setText(time);

            holder.status.setText(status);
        }

        @Override
        public int getItemCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

    }

}
