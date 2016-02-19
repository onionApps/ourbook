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
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

public class Item {

    private String _type;
    private String _key;
    private String _index;
    private byte[] _data;

    public Item() {
        this("", "", "", "");
    }

    public Item(String type, String key, String index, byte[] data) {
        _type = type;
        _key = key;
        _index = index;
        _data = data;
    }

    private Item(String type, String key, String index, String data) {
        this(type, key, index, data == null ? new byte[0] : data.getBytes(Utils.utf8));
    }

    public Item(String type, String key, String index, JSONObject json) {
        this(type, key, index, json.toString());
    }

    public String type() {
        return _type;
    }

    public String key() {
        return _key;
    }

    public String index() {
        return _index;
    }

    public byte[] data() {
        return _data;
    }

    public String text() {
        return new String(_data, Utils.utf8);
    }

    public JSONObject json() {
        try {
            return new JSONObject(text());
        } catch (JSONException ex) {
            return new JSONObject();
        }
    }

    public JSONObject json(Context context, String address) {

        if (address == null) {
            address = "";
        }
        if (address.equals(Tor.getInstance(context).getID())) {
            address = "";
        }
        address = address.trim();

        JSONObject json = json();

        if ("post".equals(_type)) {

            // set addr if it's my post
            if (!json.has("addr") || json.optString("addr").trim().isEmpty()) {
                if ("".equals(address)) {
                    address = Tor.getInstance(context).getID();
                }
                try {
                    json.put("addr", address);
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            }

            // update name and thumb if it's my post
            if (json.optString("addr").equals(Tor.getInstance(context).getID())) {

                {
                    String name = ItemDatabase.getInstance(context).get("name", "", 1).one().json().optString("name");
                    try {
                        json.put("name", name);
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                {
                    String thumb = ItemDatabase.getInstance(context).get("thumb", "", 1).one().json().optString("thumb");
                    try {
                        json.put("thumb", thumb);
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }

            }
        }

        if ("friend".equals(_type)) {
            ItemResult rs = ItemCache.getInstance(context).get(json.optString("addr"), "thumb", "", 1);
            if (rs.size() > 0) {
                String thumb = rs.one().json().optString("thumb");
                try {
                    json.put("thumb", thumb);
                } catch (JSONException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        // set flags if it's my meta
        if ("".equals(address)) {
            if ("name".equals(_type) || "info".equals(_type)) {
                json.remove("nochat");
                json.remove("friendbot");

                String acceptmessages = Settings.getPrefs(context).getString("acceptmessages", "");
                boolean chatbotmode = Settings.getPrefs(context).getBoolean("chatbot", false);
                if ("none".equals(acceptmessages) && chatbotmode == false) {
                    try {
                        json.put("nochat", true);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }

                if (Settings.getPrefs(context).getBoolean("friendbot", false)) {
                    try {
                        json.put("friendbot", true);
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        _data = json.toString().getBytes(Utils.utf8);

        return json;

    }


    public Bitmap bitmap(String key) {
        try {
            String str = json().optString(key);
            str = str.trim();
            if (str.isEmpty()) return null;
            byte[] photodata = Base64.decode(str, Base64.DEFAULT);
            if (photodata.length == 0) return null;
            return BitmapFactory.decodeByteArray(photodata, 0, photodata.length);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static class Builder {
        private JSONObject o = new JSONObject();
        private String type, key, index;

        public Builder(String type, String key, String index) {
            this.type = type;
            this.key = key;
            this.index = index;
        }

        public Builder put(String key, String val) {
            try {
                o.put(key, val);
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
            return this;
        }

        public Item build() {
            return new Item(type, key, index, o);
        }
    }


}














