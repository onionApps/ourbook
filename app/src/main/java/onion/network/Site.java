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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class Site {

    static Charset utf8 = Utils.utf8;
    private static Site instance;
    SiteCache cache;
    String tsrc, tpage, tpost, tmore, tfriend, tempty, tprofile, tprofilerow, tnotfound, tconnect;
    Context context;
    TimeCache timeCache = new TimeCache();
    TagCache tagCache = new TagCache();

    public Site(Context context) {
        this.context = context;

        tsrc = rawstr(R.raw.page);
        log(tsrc);

        tpage = tsrc;
        tpage = tremove(tpage, "post");
        tpage = tremove(tpage, "more");
        tpage = tremove(tpage, "friend");
        tpage = tremove(tpage, "empty");
        tpage = tremove(tpage, "profile");
        tpage = tremove(tpage, "notfound");
        tpage = tremove(tpage, "connect");
        log(tpage);

        tpost = textract(tsrc, "post");
        tmore = textract(tsrc, "more");
        tfriend = textract(tsrc, "friend");
        tempty = textract(tsrc, "empty");
        tprofile = textract(tsrc, "profile");
        tprofilerow = textract(tsrc, "profilerow");
        tnotfound = textract(tsrc, "notfound");
        tconnect = textract(tsrc, "connect");

        cache = SiteCache.getInstance(context);
    }

    synchronized public static Site getInstance(Context context) {
        if (instance == null) {
            instance = new Site(context.getApplicationContext());
        }
        return instance;
    }

    static void log(String str) {
        //Log.i("Site", str);
        for (String s : str.split("\n")) {
            Log.i("Site", s);
        }
    }

    static String textract(String str, String key) {
        {
            String k = "<" + key + ">";
            int i = str.indexOf(k);
            if (i > 0) {
                str = str.substring(i + k.length());
            }
        }
        {
            String k = "</" + key + ">";
            int i = str.indexOf(k);
            if (i > 0) {
                str = str.substring(0, i);
            }
        }
        return str;
    }

    static String treplace(String str, String key, String with) {
        String ka = "<" + key + ">";
        String kb = "</" + key + ">";
        int ia = str.indexOf(ka);
        int ib = str.indexOf(kb);
        if (ia < 0 || ib < ia) return str;
        return treplace(str.substring(0, ia) + with + str.substring(ib + kb.length()), key, with);
    }

    static String tremove(String src, String key) {
        return treplace(src, key, "");
    }

    static String html(String str) {
        //return Html.escapeHtml(str);
        return TextUtils.htmlEncode(str);
    }

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("sha1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String rawstr(int id) {
        try {
            return Utils.str(context.getResources().openRawResource(id));
        } catch (Exception ex) {
            return "";
        }
    }

    public String htmlthumb(String data, int width, int height) {

        int quality = 60;

        MessageDigest digest = sha1();
        String key = Base64.encodeToString(digest.digest(data.getBytes()), Base64.NO_WRAP | Base64.NO_PADDING);
        key += "-" + width + "-" + height + "-" + quality;

        {
            String v = cache.get(key);
            if (v != null) {
                log("img from cache " + key + " " + v.length());
                return v;
            }
        }

        Bitmap bmp = Utils.decodeImage(data);
        if (bmp == null) {
            bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.nothumb2);
        }

        double fw = bmp.getWidth();
        double fh = bmp.getHeight();

        if (fw > width) {
            double s = (double) width / fw;
            fw *= s;
            fh *= s;
        }

        if (fh > height) {
            double s = (double) height / fh;
            fw *= s;
            fh *= s;
        }

        int w = (int) fw;
        int h = (int) fh;

        //if(w != width || h != height) {
        bmp = Bitmap.createScaledBitmap(bmp, w, h, true);
        //}

        log("r " + w + " " + h);
        log("s " + bmp.getWidth() + " " + bmp.getHeight());

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
        data = "data:image/jpeg;base64," + data;
        //data = data.replaceAll("\\s+","");

        log("img to cache " + key + " " + data.length());
        cache.put(key, data);

        return data;

    }

    public String htmlname(String str) {
        if (str == null || "".equals(str)) {
            return "Anonymous";
        }
        return html(str);
    }

    public String htmlbr(String str) {
        //return html(str).replace("\n", "<br>");
        String[] ss = str.split("\n");
        for (int i = 0; i < ss.length; i++)
            ss[i] = html(ss[i]);
        return TextUtils.join("<br>", ss);
    }

    public String htmlbrlinkify(String str) {
        str = htmlbr(str);

        SpannableStringBuilder b = new SpannableStringBuilder(str);
        Linkify.addLinks(b, Linkify.WEB_URLS);
        URLSpan[] urls = b.getSpans(0, b.length(), URLSpan.class);
        for (URLSpan span : urls) {
            b.insert(b.getSpanStart(span), "<a class=\"link\" target=\"_blank\" rel=\"nofollow,noreferrer\" href=\"" + html(span.getURL()) + "\">");
            b.insert(b.getSpanEnd(span), "</a>");
        }
        str = b.toString();

        return str;
    }

    public String digest(ItemResult itemResult) {
        MessageDigest digest = sha1();
        digest.update(("" + itemResult.size()).getBytes());
        for (int i = 0; i < itemResult.size(); i++) {
            Item item = itemResult.at(i);
            digest.update(item.type().getBytes(Utils.utf8));
            digest.update(item.key().getBytes(Utils.utf8));
            digest.update(item.index().getBytes(Utils.utf8));
            digest.update(item.data());
        }
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public Response get(Uri uri) {

        log("get " + uri);

        String p = uri.getPath().toLowerCase();
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);

        String key;
        if (p.equals("/network.onion/wall") || p.equals("/network.onion")) {
            key = "wall";
        } else if (p.equals("/network.onion/friends")) {
            key = "friends";
        } else if (p.equals("/network.onion/profile")) {
            key = "profile";
        } else if (p.equals("/network.onion/connect")) {
            key = "connect";
        } else {
            key = "404";
        }

        {
            Response r = timeCache.get(key);
            if (r != null) {
                return r;
            }
        }

        ItemDatabase db = ItemDatabase.getInstance(context);
        Tor tor = Tor.getInstance(context);

        String addr = tor.getID();

        String name = db.getstr("name");
        if (name == null || name.isEmpty()) {
            name = "Anonymous";
        }

        Response response = new Response();

        String content = "";

        String tag = "";

        boolean more = false;
        boolean empty = false;

        if (key.equals("wall")) {
            ItemResult rs = db.get("post", "", 8);
            {
                tag = digest(rs);
                Response r = tagCache.get(key, tag);
                if (r != null) {
                    timeCache.put(key, r);
                    return r;
                }
            }
            for (int i = 0; i < rs.size(); i++) {
                Item item = rs.at(i);
                JSONObject o = item.json(context, addr);
                String s = tpost;
                s = treplace(s, "name", htmlname(o.optString("name")));
                s = treplace(s, "addr", html(o.optString("addr")));
                s = treplace(s, "text", htmlbrlinkify(o.optString("text")));
                s = treplace(s, "date", html(Utils.date(o.optString("date"))));
                s = treplace(s, "thumb", htmlthumb(o.optString("thumb"), 56, 56));

                String url = "http://" + uri.getHost().replace(addr, o.optString("addr")) + "/network.onion";
                s = treplace(s, "url", html(url));

                if (o.has("img")) {
                    s = treplace(s, "img", htmlthumb(o.optString("img"), 320, 160));
                } else {
                    s = tremove(s, "imgtag");
                }
                content += s;
            }
            empty = rs.size() == 0;
            more = rs.more() != null;
            more = true;
        } else if (key.equals("friends")) {
            ItemResult rs = db.get("friend", "", 8);
            {
                tag = digest(rs);
                Response r = tagCache.get(key, tag);
                if (r != null) {
                    timeCache.put(key, r);
                    return r;
                }
            }
            for (int i = 0; i < rs.size(); i++) {
                Item item = rs.at(i);
                JSONObject o = item.json(context, addr);
                String s = tfriend;
                s = treplace(s, "name", htmlname(o.optString("name")));
                s = treplace(s, "addr", html(o.optString("addr")));

                String url = "http://" + uri.getHost().replace(addr, o.optString("addr")) + "/network.onion";
                s = treplace(s, "url", html(url));

                //s = treplace(s, "thumb", htmlthumb(o.optString("thumb")));
                s = treplace(s, "img", htmlthumb(ItemCache.getInstance(context).get(o.optString("addr"), "thumb").onestr("thumb"), 48, 48));
                content += s;
            }
            empty = rs.size() == 0;
            more = rs.more() != null;
        } else if (key.equals("profile")) {
            ItemResult rs = db.get("info", "", 1);
            {
                tag = digest(rs);
                Response r = tagCache.get(key, tag);
                if (r != null) {
                    timeCache.put(key, r);
                    return r;
                }
            }
            {
                String s = tprofilerow;
                s = treplace(s, "key", "ID");
                s = treplace(s, "val", html(addr));
                content += s;
            }
            JSONObject o = rs.one().json();
            for (ProfilePage.Row row : ProfilePage.rows) {
                if (!"name".equals(row.key) && !o.has(row.key)) {
                    continue;
                }
                String val = o.optString(row.key);
                if ("name".equals(row.key)) {
                    val = htmlname(val);
                } else {
                    val = htmlbr(val);
                }
                String s = tprofilerow;
                s = treplace(s, "key", row.label);
                s = treplace(s, "val", val);
                content += s;
            }
            content = treplace(tprofile, "profilerow", content);
        } else if (key.equals("connect")) {

            String ik = "QRconnect1";

            content = tconnect;

            String data = cache.get(ik);
            if (data == null) {
                Bitmap bmp = QR.make("network.onion " + addr + " " + name);
                log("QR " + bmp.getWidth() + " " + bmp.getHeight());
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 50, stream);
                data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
                data = "data:image/png;base64," + data;
                if (addr != null && !addr.isEmpty()) {
                    cache.put(ik, data);
                }
            }

            content = treplace(content, "qr", data);

        } else {
            {
                tag = "";
                Response r = tagCache.get(key, tag);
                if (r != null) {
                    timeCache.put(key, r);
                    return r;
                }
            }
            //content = "Error 404: Not Found";
            content = tnotfound;
            response.setStatus(404, "Not Found");
        }



        if (empty) {
            content += tempty;
        }
        else
        if (more) {
            content += tmore;
        }


        String page = tpage;

        page = treplace(page, "content", content);

        {
            String url = "http://" + uri.getHost() + "/network.onion";
            page = treplace(page, "url", html(url));
        }

        {
            //String url = "http://network.onion/" + uri.getHost();
            String url = "onionnet:" + Tor.getInstance(context).getID();
            page = treplace(page, "url2", html(url));
        }

        page = treplace(page, "name", htmlname(name));
        page = treplace(page, "addr", html(addr));
        page = treplace(page, "img", htmlthumb(db.getstr("thumb"), 75, 75));

        page = page.replaceAll("\\s+", " ");

        response.setText(page);
        response.setMimeType("text/html");

        log("response size " + response.getData().length);

        tagCache.put(key, response, tag);
        timeCache.put(key, response);

        return response;
    }

    public static class Response {

        private byte[] data;
        private String mimeType;
        private String charset;
        private int statusCode = 200;
        private String statusMessage = "OK";

        public void setText(String text) {
            data = text.getBytes(Charset.forName("UTF-8"));
            charset = "utf-8";
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getCharset() {
            return charset;
        }

        public void setCharset(String charset) {
            this.charset = charset;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public void setStatus(int statusCode, String statusMessage) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
        }

        public String digest() {
            MessageDigest digest = sha1();
            digest.update((charset + " " + mimeType + " " + statusCode + " " + statusMessage + " ").getBytes(utf8));
            if (data != null) digest.update(data);
            //return Base64.encodeToString(digest.digest(), Base64.NO_WRAP | Base64.NO_PADDING);
            return org.spongycastle.util.encoders.Hex.toHexString(data);
        }

    }

    static class TimeCache {
        //long maxAge = 5000;
        long maxAge = 2000;
        HashMap<String, Pair<Response, Long>> data = new HashMap<>();

        synchronized public void put(String key, Response response) {
            data.put(key, new Pair<>(response, System.currentTimeMillis()));
        }

        synchronized public Response get(String key) {
            Pair<Response, Long> v = data.get(key);
            if (v == null) return null;
            if (System.currentTimeMillis() > v.second + maxAge) return null;
            log("from time cache");
            return v.first;
        }
    }

    static class TagCache {
        HashMap<String, Pair<Response, String>> data = new HashMap<>();

        synchronized public void put(String key, Response response, String tag) {
            //data.put(key, new Pair<>(response, tag));
        }

        synchronized public Response get(String key, String tag) {
            return null;
            /*Pair<Response, String> v = data.get(key);
            if(v == null) return null;
            if(!v.second.equals(tag)) return null;
            log("from tag cache");
            return v.first;*/
        }
    }

}
