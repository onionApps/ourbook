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
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;

public class ListPreferenceEx extends ListPreference {

    public ListPreferenceEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ListPreferenceEx(Context context) {
        super(context);
        init();
    }

    private void init() {
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference p, Object a) {
                p.setSummary(getEntry());
                return true;
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return super.getEntry();
    }

}
