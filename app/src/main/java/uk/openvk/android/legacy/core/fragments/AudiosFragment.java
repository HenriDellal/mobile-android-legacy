package uk.openvk.android.legacy.core.fragments;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import uk.openvk.android.legacy.Global;
import uk.openvk.android.legacy.OvkApplication;
import uk.openvk.android.legacy.R;
import uk.openvk.android.legacy.api.entities.Account;
import uk.openvk.android.legacy.api.entities.Audio;
import uk.openvk.android.legacy.core.activities.AppActivity;
import uk.openvk.android.legacy.core.activities.AudioPlayerActivity;
import uk.openvk.android.legacy.core.fragments.base.ActiviableFragment;
import uk.openvk.android.legacy.databases.AudioCacheDB;
import uk.openvk.android.legacy.receivers.AudioPlayerReceiver;
import uk.openvk.android.legacy.services.AudioPlayerService;
import uk.openvk.android.legacy.ui.list.adapters.AudiosListAdapter;
import uk.openvk.android.legacy.ui.utils.WrappedGridLayoutManager;
import uk.openvk.android.legacy.ui.utils.WrappedLinearLayoutManager;

import static android.content.Context.BIND_AUTO_CREATE;
import static uk.openvk.android.legacy.services.AudioPlayerService.ACTION_PLAYER_CONTROL;
import static uk.openvk.android.legacy.services.AudioPlayerService.ACTION_UPDATE_CURRENT_TRACKPOS;
import static uk.openvk.android.legacy.services.AudioPlayerService.ACTION_UPDATE_PLAYLIST;

/*  Copyleft © 2022, 2023 OpenVK Team
 *  Copyleft © 2022, 2023 Dmitry Tretyakov (aka. Tinelix)
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
 */

public class AudiosFragment extends ActiviableFragment implements AudioPlayerService.AudioPlayerListener {
    private RecyclerView audiosView;
    private Account account;
    private View view;
    private String instance;
    private ArrayList<Audio> audios;
    private AudiosListAdapter audiosAdapter;
    private Context parent;
    private MediaPlayer mediaPlayer;
    public boolean isBoundAP;
    private AudioPlayerReceiver audioPlayerReceiver;
    private AudioPlayerService audioPlayerService;
    private int currentTrackPos;
    private Intent serviceIntent;
    private ServiceConnection audioPlayerConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            audioPlayerService.removeListener(AudiosFragment.this);
            isBoundAP = false;
            audioPlayerService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            isBoundAP = true;
            AudioPlayerService.AudioPlayerBinder mLocalBinder =
                    (AudioPlayerService.AudioPlayerBinder) service;
            audioPlayerService = mLocalBinder.getService();
            audioPlayerService.addListener(AudiosFragment.this);
        }
    };
    private Menu fragment_menu;
    private ArrayList<Audio> search_results;
    public SearchView searchView;
    private TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if(editable.length() > 2) {
                search_results = audiosAdapter.findItems(audios, editable.toString());
                createSearchResultsAdapter(search_results);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_audios, container, false);
        audiosView = view.findViewById(R.id.audios_listview);
        instance = ((OvkApplication) getContext().getApplicationContext()).getCurrentInstance();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.audio, menu);
        fragment_menu = menu;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        createSearchItem(menu);
    }

    private void createSearchItem(Menu menu) {
        SearchManager searchManager =
                (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        searchView = null;
        float dp = getResources().getDisplayMetrics().scaledDensity;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if(searchManager != null) {
                searchView = (SearchView) menu.findItem(R.id.audio_search)
                        .getActionView();
                int[] searchViewIds = Global.generateSearchViewIds(getResources());
                final ImageView search_btn = searchView.findViewById(searchViewIds[0]);
                final View search_plate = searchView.findViewById(searchViewIds[1]);
                final TextView query_tv = searchView.findViewById(searchViewIds[2]);
                final ImageView search_mag_icon = searchView.findViewById(searchViewIds[3]);
                final ImageView search_close_btn = searchView.findViewById(searchViewIds[4]);
                search_btn.setImageResource(R.drawable.ic_ab_search);
                searchView.setSearchableInfo(
                        searchManager.getSearchableInfo(getActivity().getComponentName()));
                final ActionBar ab = getActivity().getActionBar();
                searchView.setOnSearchClickListener(new View.OnClickListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View view) {
                        search_plate.setBackgroundDrawable(
                                getResources().getDrawable(R.drawable.login_fields)
                        );
                        search_mag_icon.setImageDrawable(
                                getResources().getDrawable(R.drawable.ic_ab_search_light)
                        );
                        search_close_btn.setImageDrawable(
                                getResources().getDrawable(R.drawable.ic_search_clear)
                        );
                        query_tv.setTextColor(Color.BLACK);

                        if(!((OvkApplication) getContext().getApplicationContext()).isTablet) {
                            if (ab != null)
                                ab.getCustomView()
                                        .findViewById(R.id.custom_ab_layout)
                                        .setVisibility(View.GONE);
                        }
                    }
                });
                searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public boolean onClose() {
                        if(ab != null)
                            ab.getCustomView()
                                    .findViewById(R.id.custom_ab_layout)
                                    .setVisibility(View.VISIBLE);
                        createSearchResultsAdapter(audios);
                        return false;
                    }
                });
                final SearchView.OnQueryTextListener queryTextListener =
                        new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextChange(String newText) {
                                if(newText.length() > 2) {
                                    search_results = audiosAdapter.findItems(audios, newText);
                                    createSearchResultsAdapter(search_results);
                                }
                                return true;
                            }

                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                return true;
                            }
                        };
                if(((OvkApplication) getContext().getApplicationContext()).isTablet) {
                    searchView.setMaxWidth((int) (320 * dp));
                } else {
                    searchView.setMaxWidth(
                            (int) (
                                    getResources().getDisplayMetrics().widthPixels -
                                            ((44 * dp))
                            )
                    );
                }
                searchView.setPadding(0, (int)(4 * dp), 0, (int)(4 * dp));
                searchView.setQueryHint(getResources().getString(R.string.search));
                searchView.setOnQueryTextListener(queryTextListener);
            }
        } else {
            final dev.tinelix.retro_ab.ActionBar actionBar = getActivity().findViewById(R.id.actionbar);
            actionBar.removeAllActions();
            final EditText search_edit = ((EditText) actionBar.findViewById(R.id.search_view));
            search_edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    // TODO Auto-generated method stub
                    if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                        String query = search_edit.getText().toString();
                        if(query.length() > 2) {
                            search_results = audiosAdapter.findItems(audios, query);
                            createSearchResultsAdapter(search_results);
                        }
                    }
                    return true;
                }
            });
            actionBar.addAction(new dev.tinelix.retro_ab.ActionBar.Action() {
                @Override
                public int getDrawable() {
                    return R.drawable.ic_ab_search;
                }

                @Override
                public void performAction(View view) {
                    actionBar.findViewById(R.id.title_ab).setVisibility(View.GONE);
                    actionBar.findViewById(R.id.search_ab).setVisibility(View.VISIBLE);
                    actionBar.removeAllActions();
                }
            });
        }
    }

    public void closeSearchItem() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getActivity().getActionBar();
            if(ab != null)
                ab.getCustomView()
                        .findViewById(R.id.custom_ab_layout)
                        .setVisibility(View.VISIBLE);
            searchView.setIconified(true);
            createSearchResultsAdapter(audios);
        } else {
            final dev.tinelix.retro_ab.ActionBar actionBar = getActivity().findViewById(R.id.actionbar);
            final EditText search_edit = ((EditText) actionBar.findViewById(R.id.search_view));
            actionBar.findViewById(R.id.title_ab).setVisibility(View.VISIBLE);
            actionBar.findViewById(R.id.search_ab).setVisibility(View.GONE);
            search_edit.setText("");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.audio_search:
                return false;
            default:
                break;
        }

        return false;
    }

    public void createAdapter(Context ctx, ArrayList<Audio> audios) {
        this.parent = ctx;
        this.audios = audios;
        OvkApplication app = ((OvkApplication)getContext().getApplicationContext());
        audioPlayerReceiver = new AudioPlayerReceiver(getContext());
        IntentFilter intentFilter = new IntentFilter(ACTION_PLAYER_CONTROL);
        intentFilter.addAction(ACTION_UPDATE_PLAYLIST);
        intentFilter.addAction(ACTION_UPDATE_CURRENT_TRACKPOS);
        parent.registerReceiver(audioPlayerReceiver, intentFilter);
        if(app.audioPlayerService == null) {
            app.audioPlayerService = new AudioPlayerService();
        }
        startAudioPlayerService();
        if (audiosAdapter == null) {
            LinearLayout bottom_player_view = view.findViewById(R.id.audio_player_bar);
            audiosAdapter = new AudiosListAdapter(ctx, bottom_player_view, audios, false);
            if(app.isTablet && app.swdp >= 760) {
                LinearLayoutManager glm = new WrappedGridLayoutManager(ctx, 3);
                glm.setOrientation(LinearLayoutManager.VERTICAL);
                audiosView.setLayoutManager(glm);
            } else if(app.isTablet && app.swdp >= 600) {
                LinearLayoutManager glm = new WrappedGridLayoutManager(ctx, 2);
                glm.setOrientation(LinearLayoutManager.VERTICAL);
                audiosView.setLayoutManager(glm);
            } else {
                LinearLayoutManager llm = new WrappedLinearLayoutManager(ctx);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                audiosView.setLayoutManager(llm);
            }
            audiosView.setAdapter(audiosAdapter);
        } else {
            audiosAdapter.notifyDataSetChanged();
        }
        AudioCacheDB.clear(parent, false);
        AudioCacheDB.fillDatabase(parent, audios, false, false);
    }

    public void createSearchResultsAdapter(ArrayList<Audio> audios) {
        OvkApplication app = ((OvkApplication)getContext().getApplicationContext());
        LinearLayout bottom_player_view = view.findViewById(R.id.audio_player_bar);
        audiosAdapter = new AudiosListAdapter(parent, bottom_player_view, audios, true);
        if(app.isTablet && app.swdp >= 760) {
            LinearLayoutManager glm = new WrappedGridLayoutManager(parent, 3);
            glm.setOrientation(LinearLayoutManager.VERTICAL);
            audiosView.setLayoutManager(glm);
        } else if(app.isTablet && app.swdp >= 600) {
            LinearLayoutManager glm = new WrappedGridLayoutManager(parent, 2);
            glm.setOrientation(LinearLayoutManager.VERTICAL);
            audiosView.setLayoutManager(glm);
        } else {
            LinearLayoutManager llm = new WrappedLinearLayoutManager(parent);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            audiosView.setLayoutManager(llm);
        }
        audiosView.setAdapter(audiosAdapter);
        AudioCacheDB.clear(parent, true);
        AudioCacheDB.fillDatabase(parent, audios, false, true);
    }

    public void startAudioPlayerService() {
        serviceIntent = new Intent(getContext().getApplicationContext(), AudioPlayerService.class);
        if(!isBoundAP) {
            OvkApplication app = ((OvkApplication) getContext().getApplicationContext());
            Log.d(OvkApplication.APP_TAG, "Creating AudioPlayerService intent");
            serviceIntent.putExtra("action", "PLAYER_CREATE");
        } else {
            serviceIntent.putExtra("action", "PLAYER_GET_CURRENT_POSITION");
        }
        parent.getApplicationContext().startService(serviceIntent);
        parent.getApplicationContext().bindService(serviceIntent, audioPlayerConnection, BIND_AUTO_CREATE);
    }

    public void setAudioPlayerState(int position, int status, boolean fromList) {
        String action = "";
        switch (status) {
            case AudioPlayerService.STATUS_STARTING:
                action = "PLAYER_START";
                break;
            case AudioPlayerService.STATUS_PLAYING:
                action = "PLAYER_PLAY";
                break;
            case AudioPlayerService.STATUS_PAUSED:
                action = "PLAYER_PAUSE";
                break;
            default:
                action = "PLAYER_STOP";
                break;
        }
        serviceIntent = new Intent(parent.getApplicationContext(), AudioPlayerService.class);
        serviceIntent.putExtra("action", action);
        if(status == AudioPlayerService.STATUS_STARTING) {
            serviceIntent.putExtra("position", position);
            if(audiosAdapter.isSearchResults() && fromList) {
                serviceIntent.putExtra("from", "search");
            }
        }
        Log.d(OvkApplication.APP_TAG, "Setting AudioPlayerService state");
        parent.getApplicationContext().startService(serviceIntent);
        parent.getApplicationContext().bindService(serviceIntent, audioPlayerConnection, BIND_AUTO_CREATE);
    }

    public void setScrollingPositions(Context ctx, boolean b) {
    }

    public void receivePlayerStatus(String action, int status, int track_position, Bundle data) {
        if(audios != null && audios.size() > 0) {
            audiosAdapter.setTrackState(track_position, status);
            if (parent instanceof AppActivity) {
                AppActivity activity = ((AppActivity) parent);
                if (status == AudioPlayerService.STATUS_STARTING) {
                    activity.notifMan.createAudioPlayerChannel();
                }
                if (status != AudioPlayerService.STATUS_STOPPED) {
                    activity.notifMan.buildAudioPlayerNotification(
                            getContext(), audios, track_position
                    );
                    showBottomPlayer(audios.get(track_position));
                } else {
                    audiosAdapter.setTrackState(track_position, 0);
                    activity.notifMan.clearAudioPlayerNotification();
                    if (view != null) {
                        view.findViewById(R.id.audio_player_bar).setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    public void showBottomPlayer(final AudiosListAdapter.Holder holder, final Audio track) {
        LinearLayout bottom_player_view = view.findViewById(R.id.audio_player_bar);
        bottom_player_view.setVisibility(View.VISIBLE);
        TextView title_tv = bottom_player_view.findViewById(R.id.audio_panel_title);
        TextView artist_tv = bottom_player_view.findViewById(R.id.audio_panel_artist);
        final ImageView cover_view = bottom_player_view.findViewById(R.id.audio_panel_cover);
        final ImageView play_btn = bottom_player_view.findViewById(R.id.audio_panel_play);
        title_tv.setText(track.title);
        artist_tv.setText(track.artist);
        title_tv.setSelected(true);
        artist_tv.setSelected(true);
        bottom_player_view.findViewById(R.id.audio_panel_prev).setVisibility(View.GONE);
        bottom_player_view.findViewById(R.id.audio_panel_next).setVisibility(View.GONE);
        cover_view.setImageDrawable(
                getResources().getDrawable(R.drawable.aplayer_cover_placeholder)
        );
        if(track.status == 0 || track.status == 3) {
            play_btn.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_audio_panel_play)
            );
        } else if(track.status == 2) {
            play_btn.setImageDrawable(
                    getResources().getDrawable(R.drawable.ic_audio_panel_pause)
            );
        }
        play_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomPlayer(holder, track);
                holder.playAudioTrack(audiosAdapter.getCurrentTrackPosition());
            }
        });
        bottom_player_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), AudioPlayerActivity.class);
                startActivity(intent);
            }
        });
    }

    private void showBottomPlayer(Audio track) {
        LinearLayout bottom_player_view = view.findViewById(R.id.audio_player_bar);
        bottom_player_view.setVisibility(View.VISIBLE);
        TextView title_tv = bottom_player_view.findViewById(R.id.audio_panel_title);
        TextView artist_tv = bottom_player_view.findViewById(R.id.audio_panel_artist);
        final ImageView cover_view = bottom_player_view.findViewById(R.id.audio_panel_cover);
        final ImageView play_btn = bottom_player_view.findViewById(R.id.audio_panel_play);
        title_tv.setText(track.title);
        artist_tv.setText(track.artist);
    }

    public void updateCurrentTrackPosition(int track_pos, int status) {
        audiosAdapter.setTrackState(audiosAdapter.getCurrentTrackPosition(), status);
    }

    public void refreshOptionsMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActivity().invalidateOptionsMenu();
        } else {
            final dev.tinelix.retro_ab.ActionBar actionBar = getActivity().findViewById(R.id.actionbar);
            actionBar.removeAllActions();
            final EditText search_edit = ((EditText) actionBar.findViewById(R.id.search_view));
            search_edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    // TODO Auto-generated method stub
                    if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                        String query = search_edit.getText().toString();
                        if(query.length() > 2) {
                            search_results = audiosAdapter.findItems(audios, query);
                            createSearchResultsAdapter(search_results);
                        }
                    }
                    return true;
                }
            });
            actionBar.addAction(new dev.tinelix.retro_ab.ActionBar.Action() {
                @Override
                public int getDrawable() {
                    return R.drawable.ic_ab_search;
                }

                @Override
                public void performAction(View view) {
                    actionBar.findViewById(R.id.title_ab).setVisibility(View.GONE);
                    actionBar.findViewById(R.id.search_ab).setVisibility(View.VISIBLE);
                    actionBar.removeAllActions();
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        if(audioPlayerReceiver != null) {
            parent.unregisterReceiver(audioPlayerReceiver);
        }
        if(audioPlayerService != null) {
            parent.getApplicationContext().unbindService(audioPlayerConnection);
            parent.getApplicationContext().stopService(serviceIntent);
            isBoundAP = false;
            if (parent instanceof AppActivity) {
                AppActivity activity = ((AppActivity) parent);
                activity.notifMan.clearAudioPlayerNotification();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onChangeAudioPlayerStatus(String action, int status, int track_pos, Bundle data) {

    }

    @Override
    public void onReceiveCurrentTrackPosition(int track_pos, int status) {

    }

    @Override
    public void onUpdateSeekbarPosition(int position, int duration, double buffer_length) {

    }

    @Override
    public void onAudioPlayerError(int what, int extra, int current_track_pos) {
        try {
            // The main thing is that this workaround should force the AudioPlayerService service
            // to switch/play audio tracks without fail.
            if(what == -38 && extra == 0) {
                setAudioPlayerState(current_track_pos, AudioPlayerService.STATUS_STARTING, false);
            } else {
                Toast.makeText(
                        getContext(),
                        getResources().getString(R.string.audio_play_error),
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onActivated() {
        super.onActivated();
        refreshOptionsMenu();
    }

    @Override
    public void onDeactivated() {
        super.onDeactivated();
        closeSearchItem();
    }
}
