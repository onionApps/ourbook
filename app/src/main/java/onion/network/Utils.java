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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static Charset utf8 = Charset.forName("UTF-8");

    public static String base64encode(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static byte[] base64decode(String str) {
        return Base64.decode(str, Base64.NO_WRAP);
    }

    public static byte[] bin(InputStream is) throws IOException {
        try {
            byte[] data = new byte[0];
            while (true) {
                byte[] buf = new byte[1024];
                int n = is.read(buf);
                if (n < 0) return data;
                byte[] newdata = new byte[data.length + n];
                System.arraycopy(data, 0, newdata, 0, data.length);
                System.arraycopy(buf, 0, newdata, data.length, n);
                data = newdata;
            }
        } finally {
            is.close();
        }
    }

    public static String str(InputStream is) throws IOException {
        return new String(bin(is), utf8);
    }

    public static byte[] filebin(File f) {
        try {
            return bin(new FileInputStream(f));
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    public static String filestr(File f) {
        return new String(filebin(f), utf8);
    }


    public static String date(String str) {
        long t = 0;
        try {
            t = Long.parseLong(str);
        } catch (Exception ex) {
            return "";
        }
        DateFormat sdf = new SimpleDateFormat("HH:mm  yyyy-MM-dd");
        return sdf.format(new Date(t));
    }


    public static String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.WEBP, 50, stream);

        byte[] bytes = stream.toByteArray();
        Log.i("PHOTO SIZE", "" + bytes.length);

        String v = Base64.encodeToString(bytes, Base64.NO_WRAP);

        return v;
    }


    public static Bitmap decodeImage(String str) {
        try {
            str = str.trim();
            if (str.isEmpty()) return null;
            byte[] photodata = Base64.decode(str, Base64.DEFAULT);
            if (photodata.length == 0) return null;
            return BitmapFactory.decodeByteArray(photodata, 0, photodata.length);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("sha1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }


    public static String digest(String input) {
        MessageDigest digest = sha1();
        if (input != null) digest.update(input.getBytes(utf8));
        return org.spongycastle.util.encoders.Hex.toHexString(digest.digest());
    }


    public static CharSequence linkify(final Context context, String s) {
        SpannableStringBuilder b = new SpannableStringBuilder(s);
        SpannableStringBuilder r = new SpannableStringBuilder(s);
        Linkify.addLinks(b, Linkify.WEB_URLS);
        URLSpan[] urls = b.getSpans(0, b.length(), URLSpan.class);
        for (int i = 0; i < urls.length && i < 8; i++) {
            URLSpan span = urls[i];
            int start = b.getSpanStart(span);
            int end = b.getSpanEnd(span);
            int flags = b.getSpanFlags(span);
            final String url = span.getURL();
            ClickableSpan s2 = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    new AlertDialog.Builder(context)
                            .setTitle(url)
                            .setMessage("Open link in external app?")
                            .setNegativeButton("No", null)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                }
                            })
                            .show();
                }
            };
            r.setSpan(s2, start, end, flags);
        }
        return r;
    }

    public static String getAppName(Context context) {
        return context.getString(R.string.app_name);
    }

    public static boolean isAlnum(String s) {
        if(s == null) return false;
        for(int i = 0; i < s.length(); i++) {
            if(!Character.isLetterOrDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

}