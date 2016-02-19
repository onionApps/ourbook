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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class FriendPage extends BasePage {

    LinearLayout contentView;
    int count = 8;

    public FriendPage(MainActivity activity) {
        super(activity);
        activity.getLayoutInflater().inflate(R.layout.friend_page, this, true);
        contentView = (LinearLayout) findViewById(R.id.contentView);
    }

    @Override
    public String getTitle() {
        return "Friends";
    }

    @Override
    public int getIcon() {
        return R.drawable.ic_people_white_36dp;
    }

    @Override
    public void load() {
        load(0, "");
    }

    @Override
    public int getFab() {
        return R.id.friendFab;
    }

    @Override
    public void onFab() {
        activity.showAddFriend();
    }

    void load(final int i, String s) {

        new ItemTask(getContext(), address, "friend", s, count) {
            @Override
            protected void onProgressUpdate(ItemResult... x) {

                ItemResult itemResult = x[0];

                if (contentView.getChildCount() > i) {
                    contentView.removeViews(i, contentView.getChildCount() - i);
                }

                for (int i = 0; i < itemResult.size(); i++) {

                    final Item item = itemResult.at(i);

                    JSONObject o = item.json(context, address);

                    View v = activity.getLayoutInflater().inflate(R.layout.friend_item, contentView, false);

                    TextView address = ((TextView) v.findViewById(R.id.address));
                    TextView name = ((TextView) v.findViewById(R.id.name));
                    ImageView thumb = (ImageView) v.findViewById(R.id.thumb);
                    View it = v.findViewById(R.id.item);

                    final String addr = o.optString("addr");

                    String n = o.optString("name");

                    if (n == null || n.isEmpty()) {
                        n = "Anonymous";
                    }

                    name.setText(n);
                    address.setText(addr);

                    it.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getContext().startActivity(new Intent(getContext(), MainActivity.class).putExtra("address", addr));
                        }
                    });

                    Bitmap th = item.bitmap("thumb");
                    if (th != null) {
                        thumb.setImageBitmap(th);
                    }

                    if (activity.address.isEmpty()) {
                        it.setOnLongClickListener(new OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {

                                new AlertDialog.Builder(context)
                                        .setTitle("Delete Friend?")
                                        .setMessage("Do you really want to delete this friend?")
                                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                FriendTool.getInstance(context).unfriend(item.key());
                                                load();
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .show();

                                return true;
                            }
                        });
                    }

                    contentView.addView(v);

                }

                if (i == 0 && itemResult.size() == 0 && !itemResult.loading()) {
                    View v = activity.getLayoutInflater().inflate(R.layout.friend_empty, contentView, false);
                    contentView.addView(v);
                }

                findViewById(R.id.offline).setVisibility(!itemResult.ok() && !itemResult.loading() ? View.VISIBLE : View.GONE);
                findViewById(R.id.loading).setVisibility(itemResult.loading() ? View.VISIBLE : View.GONE);

            }

            @Override
            protected void onPostExecute(ItemResult itemResult) {

                final String more = itemResult.more();
                if (more == null) {
                    return;
                }

                View v = activity.getLayoutInflater().inflate(R.layout.friend_more, contentView, false);
                v.findViewById(R.id.card).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        load(i + count, more);
                    }
                });
                contentView.addView(v);

            }
        }.execute2();
    }

}
