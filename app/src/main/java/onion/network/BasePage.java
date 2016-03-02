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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.widget.FrameLayout;

import java.io.IOException;

public abstract class BasePage extends FrameLayout {

    public MainActivity activity;
    public String address;
    public Context context;
    public ItemDatabase itemDatabase;

    public BasePage(MainActivity activity) {

        super(activity);

        this.context = activity;

        this.activity = activity;

        this.address = activity.address;

        itemDatabase = ItemDatabase.getInstance(context);

    }

    protected boolean isPageShown() {
        MainActivity a = activity;
        if (a == null) a = MainActivity.getInstance();
        if (a == null) return false;
        return activity.currentPage() == this;
    }

    public void onTabSelected() {

    }

    public String getBadge() {
        return null;
    }

    public void onNameItem(Item item) {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void load() {
    }

    public abstract String getTitle();

    public int getFab() {
        return -1;
    }

    public void onFab() {
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public int getIcon() {
        return 0;
    }

    public String getPageIDString() {
        return "";
    }

    public void toast(String s) {
        activity.toast(s);
    }

    public Bitmap getActivityResultBitmap(Intent intent) {
        try {
            return MediaStore.Images.Media.getBitmap(activity.getContentResolver(), intent.getData());
        } catch (IOException ex) {
            toast("File not found");
            return null;
        } catch(SecurityException ex) {
            toast("Access denied");
            return null;
        }
    }

    private void log(String s) {
        activity.log(s);
    }

    protected void startImageChooser(int id) {
        log("image chooser " + id);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        //Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, id);
    }

}
