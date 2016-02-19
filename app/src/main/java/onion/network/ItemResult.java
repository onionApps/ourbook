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

import java.util.ArrayList;
import java.util.List;

public class ItemResult {

    private List<Item> _data;
    private String _more;
    private boolean _ok;
    private boolean _loading;

    public ItemResult() {
        _data = new ArrayList<>();
        _more = null;
        _ok = true;
        _loading = false;
    }

    public ItemResult(List<Item> data, String more, boolean ok, boolean loading) {
        _data = data;
        _more = more;
        _ok = ok;
        _loading = loading;
    }

    public Item at(int i) {
        return _data.get(i);
    }

    public int size() {
        return _data.size();
    }

    public String more() {
        return _more;
    }

    public boolean ok() {
        return _ok;
    }

    public boolean loading() {
        return _loading;
    }

    public void setOk(boolean v) {
        _ok = v;
    }

    public void setLoading(boolean v) {
        _loading = v;
    }

    public Item one() {
        if (size() > 0)
            return at(0);
        else
            return new Item();
    }

    public String onestr(String key) {
        return one().json().optString(key);
    }


}
