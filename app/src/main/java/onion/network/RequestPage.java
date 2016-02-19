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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RequestPage extends BasePage {

    private Cursor cursor;
    private RecyclerView recycler;
    private View empty;
    private RequestDatabase db;

    public RequestPage(MainActivity a) {
        super(a);
        activity.getLayoutInflater().inflate(R.layout.request_page, this, true);
        db = RequestDatabase.getInstance(activity);

        empty = findViewById(R.id.empty);

        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(new RecyclerView.Adapter<ContactViewHolder>() {
            @Override
            public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                final ContactViewHolder viewHolder = new ContactViewHolder(activity.getLayoutInflater().inflate(R.layout.request_item, parent, false));
                viewHolder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String addr = viewHolder.address.getText().toString();
                        getContext().startActivity(new Intent(getContext(), MainActivity.class).putExtra("address", addr));
                    }
                });
                viewHolder.accept.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String addr = viewHolder.address.getText().toString();
                        String name = viewHolder.name.getText().toString();
                        RequestDatabase.getInstance(activity).removeIncoming(addr);
                        activity.addFriend(addr, name);
                        load();
                    }
                });
                viewHolder.decline.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String addr = viewHolder.address.getText().toString();
                        RequestDatabase.getInstance(activity).addDeclined(addr);
                        RequestDatabase.getInstance(activity).removeIncoming(addr);
                        load();
                    }
                });
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(ContactViewHolder holder, int position) {
                cursor.moveToPosition(position);
                holder.address.setText(cursor.getString(0));
                String name = cursor.getString(1);
                if (name == null || name.equals("")) name = "Anonymous";
                holder.name.setText(name);
            }

            @Override
            public int getItemCount() {
                return cursor != null ? cursor.getCount() : 0;
            }
        });

        RequestDatabase.getInstance(activity).setOnIncomingRequestListener(new RequestDatabase.OnIncomingRequestListener() {
            @Override
            public void onIncomingRequest(String address, String name) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        load();
                    }
                });
            }
        });

    }

    @Override
    public int getFab() {
        return R.id.friendFab;
    }

    @Override
    public void onFab() {
        activity.showAddFriend();
    }

    @Override
    public String getBadge() {

        int n = 0;

        if (cursor != null && !cursor.isClosed())
            n = cursor.getCount();

        if (n <= 0)
            return "";
        else if (n >= 10)
            return "X";
        else
            return "" + n;

    }

    @Override
    public String getTitle() {
        return "Requests";
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_group_add_white_36dp;
    }

    public void load() {

        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        cursor = db.getIncoming();
        recycler.getAdapter().notifyDataSetChanged();
        empty.setVisibility(cursor.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);

        activity.initTabs();

    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView address, name;
        View accept, decline;

        public ContactViewHolder(View view) {
            super(view);
            address = (TextView) view.findViewById(R.id.address);
            name = (TextView) view.findViewById(R.id.name);
            accept = view.findViewById(R.id.accept);
            decline = view.findViewById(R.id.decline);
        }
    }

}
