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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.getPrefs(this);

        setContentView(R.layout.prefs);

        getFragmentManager().beginTransaction().add(R.id.content, new SettingsFragment()).commit();

    }

    @Override
    protected void onPause() {
        super.onPause();
        WallBot.getInstance(this).init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.prefs, menu);
        return true;
    }

    void doreset() {

        Settings.getPrefs(SettingsActivity.this).edit().clear().commit();
        Settings.getPrefs(SettingsActivity.this);

        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);

        Snackbar.make(findViewById(R.id.content), "All settings reset", Snackbar.LENGTH_SHORT).show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_reset) {
            doreset();
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);

            getPreferenceManager().findPreference("clearcache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Clear Cache")
                            .setMessage("Remove all cache data?")
                            .setNegativeButton("No", null)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ItemCache.getInstance(getActivity()).clearCache();
                                    SiteCache.getInstance(getActivity()).clearCache();
                                    Snackbar.make(getView(), "Cache cleared.", Snackbar.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                    return true;
                }
            });

            getPreferenceManager().findPreference("licenses").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showLibraries();
                    return true;
                }
            });
        }


        void showLibraries() {
            final String[] items;
            try {
                items = getResources().getAssets().list("licenses");
            } catch (IOException ex) {
                throw new Error(ex);
            }
            new AlertDialog.Builder(getActivity())
                    .setTitle("Third party software used by this app (click to view license)")
                    .setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showLicense(items[which]);
                        }
                    })
                    .show();
        }

        void showLicense(String name) {
            String text;
            try {
                text = Utils.str(getResources().getAssets().open("licenses/" + name));
            } catch (IOException ex) {
                throw new Error(ex);
            }
            new AlertDialog.Builder(getActivity())
                    .setTitle(name)
                    .setMessage(text)
                    .show();
        }

    }

}
