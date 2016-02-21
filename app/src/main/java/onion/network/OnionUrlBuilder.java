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

import android.net.Uri;

public class OnionUrlBuilder {

    Uri.Builder uriBuilder;

    public OnionUrlBuilder(String address, String method) {
        uriBuilder = Uri.parse("http://" + address + ".onion/" + method).buildUpon();
    }

    public OnionUrlBuilder arg(String key, String val) {
        uriBuilder.appendQueryParameter(key, val);
        return this;
    }

    public Uri build() {
        return uriBuilder.build();
    }

}
