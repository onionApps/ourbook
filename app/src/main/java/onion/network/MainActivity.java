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
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance = null;
    String address = "";
    String name = "";
    ItemDatabase db;

    WallPage wallPage;
    FriendPage friendPage;
    BasePage[] pages;
    int REQUEST_QR = 12;
    String TAG = "Activity";
    TabLayout tabLayout;
    ItemResult nameItemResult = new ItemResult();
    Timer timer = null;
    private ViewPager viewPager;

    public static void addFriendItem(final Context context, String a, String name) {

        final String address = a.trim().toLowerCase();

        if (ItemDatabase.getInstance(context).hasKey("friend", address)) return;

        JSONObject o = new JSONObject();
        try {
            o.put("addr", address);
            if (name != null && !name.isEmpty()) o.put("name", name);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
        ItemDatabase.getInstance(context).put(new Item("friend", address, "" + (100000000000000l - System.currentTimeMillis()), o));

    }

    synchronized public static MainActivity getInstance() {
        return instance;
    }

    static void prefetch(Context context, String address) {
        new ItemTask(context, address, "name").execute2();
        prefetchExtra(context, address);
    }

    static void prefetchExtra(Context context, String address) {
        new ItemTask(context, address, "thumb").execute2();
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    @Override
    public Intent getParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    private Intent getParentActivityIntentImpl() {
        return new Intent(this, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        address = getIntent().getStringExtra("address");
        Intent intent = getIntent();
        if (intent != null) {
            Log.i(TAG, intent.toString());
            Uri uri = intent.getData();
            if (uri != null) {

                Log.i(TAG, uri.toString());

                if (uri.getHost().equals("network.onion")) {
                    List<String> pp = uri.getPathSegments();
                    address = pp.size() > 0 ? pp.get(0) : null;
                    name = pp.size() > 1 ? pp.get(1) : "";
                    Log.i(TAG, "ONION NETWORK URI " + address);
                }

                if (uri.getHost().endsWith(".onion") || uri.getHost().contains(".onion.")) {
                    if (uri.getPath().equalsIgnoreCase("/network.onion") || uri.getPath().toLowerCase().startsWith("/network.onion/")) {
                        for (String s : uri.getHost().split("\\.")) {
                            if (s.length() == 16) {
                                address = s;
                                Log.i(TAG, "ONION NETWORK URI " + address);
                                break;
                            }
                        }
                    }
                }

            }
        }
        if (address == null) address = "";
        address = address.trim().toLowerCase();
        if (address.equals(Tor.getInstance(this).getID())) address = "";


        db = ItemDatabase.getInstance(this);

        startService(new Intent(this, HostService.class));

        //String[] headers;


        wallPage = new WallPage(this);
        friendPage = new FriendPage(this);

        if (address.isEmpty()) {
            pages = new BasePage[]{
                    wallPage,
                    friendPage,
                    new RequestPage(this),
                    new ConversationPage(this),
                    new ProfilePage(this),
            };
        } else {
            pages = new BasePage[]{
                    wallPage,
                    friendPage,
                    new ChatPage(this),
                    new ProfilePage(this),
            };
        }

        //headers = new String[] { "Wall", "Friends", "Profile", "Requests", };

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (!address.isEmpty()) {

            setTitle(address);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return pages.length;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(final ViewGroup container, int position) {
                View v = pages[position];
                container.addView(v);
                return v;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }


        });
        //viewPager.setOffscreenPageLimit(3)

        tabLayout = (TabLayout) findViewById(R.id.tabs);


        tabLayout.setupWithViewPager(viewPager);


        for (int i = 0; i < pages.length; i++) {

            int icon = pages[i].getIcon();

            TabLayout.Tab tab = tabLayout.getTabAt(i);

            tab.setCustomView(R.layout.tab_item);
            ((ImageView) tab.getCustomView().findViewById(R.id.icon)).setImageResource(icon);

            tab.setContentDescription(pages[i].getTitle());

        }

        initTabs();

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition(), true);
                fabvis();
                pages[tab.getPosition()].onTabSelected();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });


        String pagestr = getIntent().getStringExtra("page");
        if (pagestr != null && !pagestr.isEmpty()) {
            for (int i = 0; i < pages.length; i++) {
                if (pages[i].getPageIDString().equals(pagestr)) {
                    viewPager.setCurrentItem(i);
                }
            }
        }


        if (Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().getType() != null) {

            if ("text/plain".equals(getIntent().getType())) {
                String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
                if (text != null && !text.isEmpty()) {
                    wallPage.writePost(text, null);
                }
            }

            if (getIntent().getType().startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    Bitmap image = null;
                    try {
                        image = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                    if (image != null) {
                        wallPage.writePost(null, image);
                    }
                }
            }

        }


        new Thread() {
            @Override
            public void run() {
                try {
                    Site.getInstance(getApplicationContext());
                } catch (Exception t) {
                    t.printStackTrace();
                }
            }
        }.start();


        fabvis();

        WallBot.getInstance(this);

    }

    void showEnterId() {
        View dialogView = getLayoutInflater().inflate(R.layout.friend_dialog, null);
        final EditText addressEdit = (EditText) dialogView.findViewById(R.id.address);
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Enter ID")
                .setView(dialogView)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String address = addressEdit.getText().toString().trim().toLowerCase();
                        if (address.length() != 16) {
                            snack("Invalid ID");
                            return;
                        }
                        startActivity(new Intent(MainActivity.this, MainActivity.class).putExtra("address", address));

                    }
                })
                .show();
    }

    void initTabs() {

        if (pages == null) {
            return;
        }

        for (int i = 0; i < pages.length; i++) {

            BasePage page = pages[i];

            TabLayout.Tab tab = tabLayout.getTabAt(i);

            TextView badge = (TextView) tab.getCustomView().findViewById(R.id.badge);

            String t = page.getBadge();
            if (t == null) t = "";

            badge.setVisibility("".equals(t) ? View.GONE : View.VISIBLE);
            badge.setText(t);

        }

    }

    void showAddFriend() {
        new AlertDialog.Builder(this)
                .setTitle("Add Friend")
                .setItems(new String[]{
                        "Scan QR",
                        "Enter ID",
                        "Show my QR",
                        "Show my ID",
                        "Show URL",
                        "Invite Friends",
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) scanQR();
                        if (which == 1) showEnterId();

                        if (which == 2) showQR();
                        if (which == 3) showId();

                        if (which == 4) showUri();

                        if (which == 5) inviteFriend();
                    }
                })
                .show();
    }

    void scanQR() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_QR);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        for (BasePage page : pages) {
            page.onActivityResult(requestCode, resultCode, data);
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_QR) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");

            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            bitmap.recycle();
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            try {
                Result result = reader.decode(bBitmap);
                String str = result.getText();
                Log.i("ID", str);

                String[] tokens = str.split(" ", 3);

                if (tokens.length < 2 || !tokens[0].equals("network.onion")) {
                    snack("QR Code Invalid, incompatible ID");
                    return;
                }

                String id = tokens[1].toLowerCase();

                if (id.length() != 16) {
                    snack("QR Code Invalid");
                    return;
                }

                String name = "";
                if (tokens.length > 2) {
                    name = tokens[2];
                }

                contactDialog(id, name);

                return;

            } catch (Exception ex) {
                snack("QR Code Invalid");
                ex.printStackTrace();
            }
        }

    }

    void contactDialog(String id, String name) {

        boolean isFriend = db.hasKey("friend", id);

        if (name.isEmpty()) {
            name = "Anonymous";
        }

        if (isFriend) {
            name += "  (friend)";
        }

        AlertDialog.Builder a = new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(id);

        prefetch(this, id);

        final String n = name;
        final String i = id;

        if (!isFriend) {
            a.setPositiveButton("Add Friend", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    addFriend(i, n);
                    viewPager.setCurrentItem(1, true);
                    contactDialog(i, n);
                }
            });
        }

        a.setNeutralButton("Close", null);

        a.setNegativeButton("View Profile", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(MainActivity.this, MainActivity.class).putExtra("address", i));
            }
        });

        a.show();
    }

    String getID() {
        if (address != null && !address.isEmpty()) return address.trim().toLowerCase();
        return Tor.getInstance(this).getID();
    }

    void showQR() {
        String txt = "network.onion " + getID() + " " + name;
        txt = txt.trim();

        Bitmap bitmap = QR.make(txt);

        ImageView view = new ImageView(this);
        view.setImageBitmap(bitmap);

        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        view.setPadding(pad, pad, pad, pad);

        Rect displayRectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        int s = (int) (Math.min(displayRectangle.width(), displayRectangle.height()) * 0.9);
        view.setMinimumWidth(s);
        view.setMinimumHeight(s);
        new AlertDialog.Builder(this)
                .setView(view)
                .show();
    }

    void addFriend(final String address, String name) {

        if (db.hasKey("friend", address)) return;

        addFriendItem(this, address, name);

        RequestDatabase.getInstance(this).addOutgoing(address);
        new Thread() {
            @Override
            public void run() {
                RequestTool.getInstance(MainActivity.this).sendRequest(address);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        load();
                    }
                });
            }
        }.start();

        prefetch(this, address);
        load();

        UpdateScheduler.getInstance(this).put(address);
    }

    public void load() {

        updateMenu();

        initTabs();

        {

            final String a = address;

            new ItemTask(this, a, "name", "", 1) {
                @Override
                protected void onProgressUpdate(ItemResult... values) {
                    Item item = values[0].one();
                    nameItemResult = values[0];
                    for (BasePage page : pages) {
                        page.onNameItem(item);
                    }
                    if (!address.isEmpty()) {
                        name = item.json().optString("name");
                        if (name.isEmpty()) {
                            getSupportActionBar().setTitle(a);
                            getSupportActionBar().setSubtitle(null);
                        } else {
                            getSupportActionBar().setTitle(name);
                            getSupportActionBar().setSubtitle(a);
                        }
                        if (db.hasKey("friend", a)) {
                            Item it = db.getByKey("friend", address);
                            if (it != null) {
                                JSONObject o = it.json();
                                try {
                                    o.remove("name");
                                    if (name != null && !name.isEmpty()) o.put("name", name);
                                } catch (JSONException ex) {
                                    throw new RuntimeException(ex);
                                }
                                db.put(new Item(it.type(), it.key(), it.index(), o));
                            }
                        }
                    }
                }
            }.execute2();

            prefetchExtra(this, a);

        }

        for (BasePage page : pages) {
            page.load();
        }

        fabvis();

    }

    BasePage currentPage() {
        if (viewPager == null) return null;
        Object o = viewPager.getCurrentItem();
        //Log.i(TAG, "" + o + " " + o.getClass().toString());
        return pages[(Integer) o];
    }

    void fabvis() {

        initTabs();

        int cFab = currentPage().getFab();
        for (final BasePage page : pages) {
            FloatingActionButton fab = (FloatingActionButton) findViewById(page.getFab());
            if (fab != null) {
                if (fab.getId() == cFab && "".equals(address)) {
                    if (!fab.isShown()) {
                        fab.show();
                    }
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            page.onFab();
                        }
                    });
                } else {
                    if (fab.isShown()) {
                        fab.hide();
                        fab.setOnClickListener(null);
                    }
                }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_friend).setVisible(!address.isEmpty() && !db.hasKey("friend", address));
        menu.findItem(R.id.action_friends).setVisible(!address.isEmpty() && db.hasKey("friend", address));
        return true;
    }

    void updateMenu() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_friends) {
            new AlertDialog.Builder(this)
                    .setTitle("Friends")
                    .setMessage("You are friends with this user")
                    .setNeutralButton("Remove friend", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FriendTool.getInstance(MainActivity.this).unfriend(address);
                            load();
                            snack("Contact removed.");
                        }
                    })
                    .show();
            return true;
        }

        if (id == R.id.action_add_friend) {
            addFriend(address, name);
            snack("Added friend");
            return true;
        }

        if (id == R.id.action_refresh) {
            load();
            return true;
        }


        if (id == R.id.action_menu_enter_id) {
            showEnterId();
            return true;
        }
        if (id == R.id.action_menu_show_my_id) {
            showId();
            return true;
        }
        if (id == R.id.action_menu_show_uri) {
            showUri();
        }

        if (id == R.id.action_menu_scan_qr) {
            scanQR();
            return true;
        }
        if (id == R.id.action_menu_show_my_qr) {
            showQR();
            return true;
        }

        if (id == R.id.action_menu_invite_friends) {
            inviteFriend();
            return true;
        }

        if (id == R.id.action_menu_rate) {
            rateThisApp();
            return true;
        }

        if (id == R.id.action_menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void rateThisApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            PackageManager pm = getPackageManager();
            for (ApplicationInfo packageInfo : pm.getInstalledApplications(0)) {
                if (packageInfo.packageName.equals("com.android.vending"))
                    intent.setPackage("com.android.vending");
            }
            startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    void showId() {
        final String id = getID();
        new AlertDialog.Builder(this)
                .setTitle("" + id)
                .setNegativeButton("Close", null)
                .setNeutralButton("Copy to clipboard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(id);
                        snack("ID copied to clipboard.");
                    }
                })
                .show();
    }

    void showUri() {
        final String uri = "http://" + getID() + ".onion/network.onion";
        new AlertDialog.Builder(this)
                .setTitle(uri)
                .setNeutralButton("Copy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(uri);
                        snack("URI copied to clipboard.");
                    }
                })
                .setPositiveButton("View", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                    }
                })
                .setNegativeButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, uri).setType("text/plain"));
                    }
                })
                .show();
    }

    void publishPost(JSONObject o) {
        long k = System.currentTimeMillis();
        long i = 100000000000000l - k;
        db.put(new Item("post", "" + k, "" + i, o));
    }

    public void sharePost(JSONObject o) {
        try {
            o.put("date", null);
            String k = "" + o.toString().hashCode();
            o.put("date", "" + System.currentTimeMillis());
            long i = 100000000000000l - System.currentTimeMillis();
            db.put(new Item("post", k, "" + i, o));
            snack("Post republished");
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    void snack(String str) {
        Snackbar.make(viewPager, str, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (BasePage page : pages) {
            page.onResume();
        }
        instance = this;
        load();

        Notifier.getInstance(this).clr();

        {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    Log.i(TAG, "send unsent requests");
                    RequestTool.getInstance(MainActivity.this).sendUnsentReq(address);
                }
            }, 0, 1000 * 60 * 5);
        }
    }

    @Override
    protected void onPause() {
        try {

            super.onPause();

            {
                timer.cancel();
                timer.purge();
                timer = null;
            }

            for (BasePage page : pages) {
                page.onPause();
            }

        } finally {
            if (instance == this) {
                instance = null;
            }
        }
    }


    public void lightbox(Bitmap bitmap) {
        final ImageView v = (ImageView) findViewById(R.id.lightbox);
        v.setImageBitmap(bitmap);
        v.setVisibility(View.VISIBLE);
        v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.lightbox_show));
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View vv) {
                lightboxHide();
            }
        });
    }

    private void lightboxHide() {
        final ImageView v = (ImageView) findViewById(R.id.lightbox);
        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.lightbox_hide);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        v.startAnimation(animation);
    }

    @Override
    public void onBackPressed() {
        final ImageView v = (ImageView) findViewById(R.id.lightbox);
        if (v.getVisibility() == View.VISIBLE) {
            lightboxHide();
            return;
        }
        super.onBackPressed();
    }

    void inviteFriend() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);

        String url = "https://play.google.com/store/apps/details?id=" + getPackageName();

        intent.putExtra(Intent.EXTRA_REFERRER, url);
        intent.putExtra("customAppUri", url);

        intent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.invitation_text), url, Tor.getInstance(this).getID(), Uri.encode(name)));
        intent.setType("text/plain");

        startActivity(intent);
    }

}
