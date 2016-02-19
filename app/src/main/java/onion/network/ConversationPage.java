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

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ConversationPage extends BasePage implements ChatClient.OnMessageSentListener, ChatServer.OnMessageReceivedListener {

    ChatServer chatServer;
    ChatClient chatClient;
    ConversationAdapter adapter;
    RecyclerView recycler;
    View empty;
    Tor tor;
    Cursor cursor;

    public ConversationPage(MainActivity activity) {
        super(activity);

        tor = Tor.getInstance(activity);

        chatServer = ChatServer.getInstance(activity);
        chatClient = ChatClient.getInstance(activity);

        inflate(activity, R.layout.conversation_page, this);

        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new ConversationAdapter();
        recycler.setAdapter(adapter);

        empty = findViewById(R.id.empty);
    }

    @Override
    public String getPageIDString() {
        return "chat";
    }

    @Override
    public void onResume() {
        chatServer.addOnMessageReceivedListener(this);
        chatClient.addOnMessageSentListener(this);
    }

    @Override
    public void onPause() {
        chatServer.removeOnMessageReceivedListener(this);
        chatClient.removeOnMessageSentListener(this);
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
    public String getTitle() {
        return "Conversations";
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_question_answer_white_36dp;
    }

    @Override
    public void load() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

        cursor = ChatDatabase.getInstance(activity).getConversations();

        adapter.notifyDataSetChanged();

        empty.setVisibility(cursor.getCount() <= 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public String getBadge() {
        int n = ChatDatabase.getInstance(context).getIncomingConversationCount();
        if (n <= 0) return "";
        if (n >= 10) return "X";
        return "" + n;
    }

    class ConversationHolder extends RecyclerView.ViewHolder {
        public TextView name, address, message;
        public ImageView thumb, direction;
        public View data;

        public ConversationHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.name);
            address = (TextView) v.findViewById(R.id.address);
            message = (TextView) v.findViewById(R.id.message);
            thumb = (ImageView) v.findViewById(R.id.thumb);
            direction = (ImageView) v.findViewById(R.id.direction);
            data = v.findViewById(R.id.data);
        }
    }

    class ConversationAdapter extends RecyclerView.Adapter<ConversationHolder> {

        @Override
        public ConversationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConversationHolder(activity.getLayoutInflater().inflate(R.layout.conversation_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ConversationHolder holder, int position) {
            if (cursor == null) return;

            cursor.moveToFirst();
            cursor.moveToPosition(position);

            boolean incoming = cursor.getInt(cursor.getColumnIndex("incoming")) != 0;
            String content = cursor.getString(cursor.getColumnIndex("content"));
            String sender = cursor.getString(cursor.getColumnIndex("sender"));
            String receiver = cursor.getString(cursor.getColumnIndex("receiver"));
            String myid = tor.getID();
            final String remoteAddress = myid.equals(sender) ? receiver : sender;

            holder.message.setText(content);
            holder.address.setText(remoteAddress);

            Bitmap th = ItemCache.getInstance(activity).get(remoteAddress, "thumb").one().bitmap("thumb");
            if (th != null) {
                holder.thumb.setImageBitmap(th);
            } else {
                holder.thumb.setImageResource(R.drawable.nothumb);
            }

            holder.name.setText(ItemCache.getInstance(activity).get(remoteAddress, "name").one().json().optString("name", "Anonymous"));

            holder.itemView.setClickable(true);

            holder.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.startActivity(new Intent(activity, MainActivity.class).putExtra("address", remoteAddress).putExtra("page", "chat"));
                }
            });

            holder.direction.setImageResource(myid.equals(sender) ? R.drawable.ic_call_made_black_18dp : R.drawable.ic_call_received_black_18dp);

            holder.data.setAlpha(incoming ? 1.0f : 0.4f);
        }

        @Override
        public int getItemCount() {
            return cursor != null ? cursor.getCount() : 0;
        }

    }
}
