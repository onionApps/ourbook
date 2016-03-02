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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ProfilePage extends BasePage {

    public static final int REQUEST_PHOTO = 3;
    public static Row[] rows = {
            new Row("name", "Name", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME),
            new Row("location", "Location", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("lang", "Languages", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("occupation", "Occupation", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("interests", "Interests", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("hobbies", "Hobbies", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("website", "Website", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("about", "About me", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
            new Row("bio", "Bio", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE),
    };
    LinearLayout contentView;
    int REQUEST_TAKE_PHOTO = 19;

    public ProfilePage(final MainActivity activity) {
        super(activity);
        inflate(activity, R.layout.profile_page, this);
        contentView = (LinearLayout) findViewById(R.id.contentView);

        findViewById(R.id.choose_photo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("image/*");
                //activity.startActivityForResult(intent, REQUEST_PHOTO);
                startImageChooser(REQUEST_PHOTO);
            }
        });
        findViewById(R.id.choose_photo).setVisibility(address.isEmpty() ? View.VISIBLE : View.GONE);

        findViewById(R.id.delete_photo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(activity)
                        .setTitle("Delete Photo")
                        .setMessage("Do you really want to delete this photo?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ItemDatabase.getInstance(getContext()).put(new Item.Builder("photo", "", "").build());
                                ItemDatabase.getInstance(getContext()).put(new Item.Builder("thumb", "", "").build());
                                activity.load();
                                FriendTool.getInstance(context).requestUpdates();
                            }
                        })
                        .show();
            }
        });
        findViewById(R.id.delete_photo).setVisibility(View.GONE);

        findViewById(R.id.take_photo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                activity.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        });
        findViewById(R.id.take_photo).setVisibility(address.isEmpty() ? View.VISIBLE : View.GONE);

        //load();
    }

    @Override
    public String getTitle() {
        return "Profile";
    }

    @Override
    public int getIcon() {
        //return R.drawable.ic_account_circle_white_36dp;
        return R.drawable.ic_assignment_ind_white_36dp;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;

        if (requestCode == REQUEST_PHOTO || requestCode == REQUEST_TAKE_PHOTO) {
            //try {

                //Uri uri = data.getData();

                Bitmap bmp;

                if (requestCode == REQUEST_PHOTO) {
                    //bmp = ThumbnailUtils.extractThumbnail(MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri), 320, 320);
                    bmp = getActivityResultBitmap(data);
                } else { // REQUEST_TAKE_PHOTO
                    //bmp = ThumbnailUtils.extractThumbnail((Bitmap) data.getExtras().get("data"), 320, 320);
                    bmp = (Bitmap) data.getExtras().get("data");
                }

                if(bmp == null) {
                    return;
                }

                bmp = ThumbnailUtils.extractThumbnail(bmp, 320, 320);

                View view = activity.getLayoutInflater().inflate(R.layout.profile_photo_dialog, null);
                ((ImageView) view.findViewById(R.id.imageView)).setImageBitmap(bmp);

                final Bitmap fbmp = bmp;

                new AlertDialog.Builder(activity)
                        .setTitle("Set Photo")
                        .setView(view)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                {
                                    String v = Utils.encodeImage(fbmp);
                                    ItemDatabase.getInstance(getContext()).put(new Item.Builder("photo", "", "").put("photo", v).build());
                                }

                                {
                                    String v = Utils.encodeImage(ThumbnailUtils.extractThumbnail(fbmp, 84, 84));
                                    ItemDatabase.getInstance(getContext()).put(new Item.Builder("thumb", "", "").put("thumb", v).build());
                                }

                                Snackbar.make(contentView, "Photo changed", Snackbar.LENGTH_SHORT).show();

                                activity.load();

                                FriendTool.getInstance(context).requestUpdates();

                            }
                        })
                        .show();

            /*} catch (IOException ex) {
                Snackbar.make(this, "Error", Snackbar.LENGTH_SHORT).show();
            }*/
        }
    }

    void save(JSONObject o, String key, String val) {
        if (val != null) {
            val = val.trim();
        }
        try {
            o.put(key, val);
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
        ItemDatabase.getInstance(getContext()).put(new Item("info", "", "", o));

        if (key.equals("name")) {
            ItemDatabase.getInstance(getContext()).put(new Item.Builder("name", "", "").put("name", val).build());
        }

        activity.load();

        FriendTool.getInstance(context).requestUpdates();
    }

    void clear(JSONObject o, String key) {
        save(o, key, "name".equals(key) ? "" : null);
    }

    public void load() {

        findViewById(R.id.delete_photo).setVisibility(View.GONE);
        new ItemTask(getContext(), address, "photo") {
            @Override
            protected void onProgressUpdate(ItemResult... values) {
                ImageView photoview = ((ImageView) findViewById(R.id.profilephoto));
                photoview.setOnClickListener(null);
                photoview.setClickable(false);
                try {
                    ItemResult itemResult = values[0];
                    String str = itemResult.one().json().optString("photo");
                    str = str.trim();
                    if (!str.isEmpty()) {
                        byte[] photodata = Base64.decode(str, Base64.DEFAULT);
                        if (photodata.length == 0) throw new Exception();
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(photodata, 0, photodata.length);
                        photoview.setImageBitmap(bitmap);
                        if (address.isEmpty()) {
                            findViewById(R.id.delete_photo).setVisibility(View.VISIBLE);
                        }
                        photoview.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                activity.lightbox(bitmap);
                            }
                        });
                        photoview.setClickable(true);
                    } else {
                        ((ImageView) findViewById(R.id.profilephoto)).setImageResource(R.drawable.nophoto);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ((ImageView) findViewById(R.id.profilephoto)).setImageResource(R.drawable.nophoto);
                }
            }
        }.execute2();

        new ItemTask(getContext(), address, "info") {

            @Override
            protected void onProgressUpdate(ItemResult... values) {


                ItemResult itemResult = values[0];


                final JSONObject o = itemResult.one().json(activity, activity.address);


                Log.i(TAG, "onProgressUpdate: " + o);


                findViewById(R.id.is_friend_bot).setVisibility(o.optBoolean("friendbot", false) ? View.VISIBLE : View.GONE);


                contentView.removeAllViews();


                findViewById(R.id.offline).setVisibility(!itemResult.ok() && !itemResult.loading() ? View.VISIBLE : View.GONE);
                findViewById(R.id.loading).setVisibility(itemResult.loading() ? View.VISIBLE : View.GONE);


                if (address.isEmpty()) {

                    View v = activity.getLayoutInflater().inflate(R.layout.profile_item, contentView, false);
                    //v.findViewById(R.id.edit).setVisibility(View.GONE);
                    ((ImageView) v.findViewById(R.id.edit)).setImageResource(R.drawable.ic_link_black_24dp);
                    TextView keyview = ((TextView) v.findViewById(R.id.key));
                    TextView valview = ((TextView) v.findViewById(R.id.val));
                    keyview.setText("ID");
                    valview.setText(activity.getID());
                    v.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            activity.showId();
                        }
                    });
                    contentView.addView(v);

                } else {

                    View v = activity.getLayoutInflater().inflate(R.layout.profile_item, contentView, false);
                    v.findViewById(R.id.edit).setVisibility(View.GONE);
                    TextView keyview = ((TextView) v.findViewById(R.id.key));
                    TextView valview = ((TextView) v.findViewById(R.id.val));
                    keyview.setText("ID");
                    valview.setText(activity.getID());
                    v.setClickable(false);
                    contentView.addView(v);

                }


                for (final Row row : rows) {

                    View v = activity.getLayoutInflater().inflate(R.layout.profile_item, contentView, false);

                    v.findViewById(R.id.edit).setVisibility(address.isEmpty() ? View.VISIBLE : View.GONE);

                    TextView keyview = ((TextView) v.findViewById(R.id.key));
                    TextView valview = ((TextView) v.findViewById(R.id.val));

                    final String label = row.label;
                    keyview.setText(label);

                    final String key = row.key;

                    final String val = o.optString(key);
                    if (val == null || val.isEmpty()) {

                        if (!address.isEmpty() && !"name".equals(key)) {
                            continue;
                        }

                        valview.setText("Unknown");
                        valview.setTextColor(0xffbbbbbb);
                    } else {
                        valview.setText(val.trim());
                        valview.setTextColor(0xff000000);
                    }

                    if ((row.type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
                        valview.setSingleLine(false);
                    }

                    if (address.isEmpty()) {

                        v.setClickable(true);

                        v.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                if (key.equals("birth")) {

                                    int y = 0, m = 0, d = 0;
                                    try {
                                        String[] ss = val.split("-");
                                        y = Integer.parseInt(ss[0]);
                                        m = Integer.parseInt(ss[1]);
                                        d = Integer.parseInt(ss[2]);
                                    } catch (Exception ex) {
                                    }

                                    Dialog dialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
                                        @Override
                                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                            save(o, key, year + "-" + monthOfYear + "-" + dayOfMonth);
                                        }
                                    }, y, m, d);

                                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            clear(o, key);
                                        }
                                    });

                                    dialog.show();

                                    return;

                                }

                                View dialogView = activity.getLayoutInflater().inflate(R.layout.profile_dialog, null, false);
                                final EditText textEdit = (EditText) dialogView.findViewById(R.id.text);
                                textEdit.setText(val);
                                textEdit.setInputType(row.type);
                                AlertDialog.Builder dlg = new AlertDialog.Builder(getContext())
                                        .setView(dialogView)
                                        .setTitle("Change " + label)
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                            }
                                        })
                                        .setPositiveButton("Publish", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                save(o, key, textEdit.getText().toString());
                                            }
                                        })
                                        .setNeutralButton("Clear", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                clear(o, key);
                                                /*try {
                                                    o.put(key, null);
                                                } catch (JSONException ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                                ItemDatabase.getInstance(getContext()).put(new Item("info", "", "", o));*/
                                                activity.load();
                                            }
                                        });
                                dlg.show();

                            }
                        });

                    }


                    contentView.addView(v);

                }

            }

        }.execute2();

    }

    public static class Row {
        String key, label;
        int type;

        public Row(String key, String label, int type) {
            this.key = key;
            this.label = label;
            this.type = type;
        }
    }


}










