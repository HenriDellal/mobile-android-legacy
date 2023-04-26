package uk.openvk.android.legacy.ui.core.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Locale;

import dev.tinelix.retro_ab.ActionBar;
import uk.openvk.android.legacy.BuildConfig;
import uk.openvk.android.legacy.Global;
import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.R;
import uk.openvk.android.legacy.api.enumerations.HandlerMessages;
import uk.openvk.android.legacy.api.wrappers.DownloadManager;
import uk.openvk.android.legacy.ui.view.layouts.ProgressLayout;
import uk.openvk.android.legacy.ui.view.layouts.ZoomableImageView;
import uk.openvk.android.legacy.ui.wrappers.LocaleContextWrapper;

/** OPENVK LEGACY LICENSE NOTIFICATION
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU Affero General Public License as published by the Free Software Foundation, either
 *  version 3 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this
 *  program. If not, see https://www.gnu.org/licenses/.
 *
 *  Source code: https://github.com/openvk/mobile-android-legacy
 **/

public class PhotoViewerActivity extends Activity {
    private String access_token;
    private SharedPreferences instance_prefs;
    private int owner_id;
    private int post_id;
    private Bitmap bitmap;
    private Menu activity_menu;
    public Handler handler;
    private BitmapFactory.Options bfOptions;
    private DownloadManager downloadManager;
    private ActionBar actionBar;
    private PopupWindow popupMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance_prefs = getSharedPreferences("instance", 0);
        setContentView(R.layout.activity_photo_viewer);
        handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                Bundle data = message.getData();
                if(!BuildConfig.BUILD_TYPE.equals("release")) Log.d(OvkApplication.APP_TAG, String.format("Handling API message: %s", message.what));
                receiveState(message.what, data);
            }
        };
        actionBar = findViewById(R.id.actionbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                try {
                    getActionBar().setDisplayShowHomeEnabled(true);
                    getActionBar().setDisplayHomeAsUpEnabled(true);
                    getActionBar().setTitle(getResources().getString(R.string.photo));
                    getActionBar().hide();
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    getActionBar().setIcon(R.drawable.ic_ab_app);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            final ActionBar actionBar = findViewById(R.id.actionbar);
            actionBar.setTitle(R.string.photo);
            actionBar.setHomeLogo(R.drawable.ic_ab_app);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_actionbar_black_transparent));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAction(new ActionBar.AbstractAction(0) {
                @Override
                public void performAction(View view) {
                    onBackPressed();
                }
            });
            createActionPopupMenu(activity_menu);
        }

        ((ZoomableImageView) findViewById(R.id.picture_view)).setVisibility(View.GONE);
        ((ProgressLayout) findViewById(R.id.progress_layout)).setVisibility(View.VISIBLE);
        ((ProgressLayout) findViewById(R.id.progress_layout)).enableDarkTheme();
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                finish();
            } else {
                access_token = instance_prefs.getString("access_token", "");
                try {
                    if (extras.containsKey("original_link") && extras.getString("original_link").length() > 0) {
                        downloadManager = new DownloadManager(this, true);
                        downloadManager.downloadOnePhotoToCache(extras.getString("original_link"), String.format("original_photo_a%d_%d", extras.getLong("author_id"), extras.getLong("photo_id")), "original_photos");
                    } else {
                        finish();
                    }
                } catch (Exception ex) {
                    finish();
                }
            }
        } else {
            access_token = (String) savedInstanceState.getSerializable("access_token");
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale languageType = OvkApplication.getLocale(newBase);
        super.attachBaseContext(LocaleContextWrapper.wrap(newBase, languageType));
    }

    private void createActionPopupMenu(final Menu menu) {
        final View menu_container = (View) getLayoutInflater().inflate(R.layout.layout_popup_menu, null);
        final ActionBar actionBar = findViewById(R.id.actionbar);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void receiveState(int message, Bundle data) {
        if(message == HandlerMessages.ACCESS_DENIED_MARSHMALLOW) {
            allowPermissionDialog();
        } else if(message == HandlerMessages.ORIGINAL_PHOTO) {
            bfOptions = new BitmapFactory.Options();
            bfOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try {
                Bundle extras = getIntent().getExtras();
                bitmap = BitmapFactory.decodeFile(String.format("%s/photos_cache/original_photos/original_photo_a%d_%d", getCacheDir().getAbsolutePath(), extras.getLong("author_id"), extras.getLong("photo_id")), bfOptions);
                ((ZoomableImageView) findViewById(R.id.picture_view)).setImageBitmap(bitmap);
                ((ZoomableImageView) findViewById(R.id.picture_view)).enablePinchToZoom();
                ((ZoomableImageView) findViewById(R.id.picture_view)).setVisibility(View.VISIBLE);
                ((ProgressLayout) findViewById(R.id.progress_layout)).setVisibility(View.GONE);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    getActionBar().show();
                } else {
                    actionBar.setVisibility(View.VISIBLE);
                }
                ((ZoomableImageView) findViewById(R.id.picture_view)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            if(getActionBar().isShowing()) {
                                getActionBar().hide();
                            } else {
                                getActionBar().show();
                            }
                        } else {
                            if(actionBar.getVisibility() == View.VISIBLE) {
                                actionBar.setVisibility(View.GONE);
                            } else {
                                actionBar.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            } catch (OutOfMemoryError err) {
                finish();
            }
        } else if(message == 40000) {
            ((ZoomableImageView) findViewById(R.id.picture_view)).rescale();
        }
    }

    private void allowPermissionDialog() {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.allow_permisssion_in_storage_title));
        builder.setMessage(getResources().getString(R.string.allow_permisssion_in_storage));
        builder.setPositiveButton(getResources().getString(R.string.open_btn), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                return;
            }
        });
        dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if(item.getItemId() == android.R.id.home) {
                onBackPressed();
            }
        }
        if(item.getItemId() == R.id.save) {
            savePhoto();
        } else if(item.getItemId() == R.id.copy_link) {
            Bundle data = getIntent().getExtras();
            if(data.containsKey("original_link")) {
                if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(data.getString("original_link"));
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Photo URL", data.getString("original_link"));
                    clipboard.setPrimaryClip(clip);
                }
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void savePhoto() {
        Global global = new Global();
        final Bundle data = getIntent().getExtras();
        String cache_path = String.format("%s/photos_cache/original_photos/original_photo_a%d_%d", getCacheDir().getAbsolutePath(), getIntent().getExtras().getLong("author_id"), getIntent().getExtras().getLong("photo_id"));
        File file = new File(cache_path);
        String[] path_array = cache_path.split("/");
        String dest = String.format("%s/OpenVK/Photos/%s", Environment.getExternalStorageDirectory().getAbsolutePath(), path_array[path_array.length - 1]);
        String mime = bfOptions.outMimeType;
        if(bitmap != null) {
            FileChannel sourceChannel = null;
            FileChannel destChannel = null;
            if (mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/gif")) {
                try {
                    if(mime.equals("image/jpeg")) {
                        dest = dest + ".jpg";
                    } else if(mime.equals("image/png")) {
                        dest = dest + ".png";
                    } else if(mime.equals("image/gif")) {
                        dest = dest + ".gif";
                    }

                    File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "OpenVK");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenVK", "Photos");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    sourceChannel = new FileInputStream(file).getChannel();
                    destChannel = new FileOutputStream(dest).getChannel();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                        } else {
                            allowPermissionDialog();
                        }
                    } else {
                        destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                    }
                    Toast.makeText(getApplicationContext(), R.string.photo_save_ok, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        allowPermissionDialog();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
                    }
                } finally {
                    try {
                        if(sourceChannel != null && destChannel != null) {
                            sourceChannel.close();
                            destChannel.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.photo_viewer, menu);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            createActionPopupMenu(menu);
        }
        activity_menu = menu;
        return true;
    }
}