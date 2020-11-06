//package com.tinf19.musicparty.server;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.ImageButton;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.tinf19.musicparty.R;
//import com.tinf19.musicparty.fragments.ClientPlaylistFragment;
//import com.tinf19.musicparty.fragments.ExitConnectionFragment;
//import com.tinf19.musicparty.fragments.SearchBarFragment;
//import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
//import com.tinf19.musicparty.fragments.SettingsHostFragment;
//import com.tinf19.musicparty.fragments.ShowSongFragment;
//import com.tinf19.musicparty.fragments.ShowSongHostFragment;
//import com.tinf19.musicparty.music.Track;
//import com.tinf19.musicparty.util.Constants;
//
//import java.util.List;
//
//public class HostPartyActivity extends AppCompatActivity implements SearchBarFragment.SearchForSongs, ShowSongHostFragment.OpenHostFragments, SearchSongsOutputFragment.AddSongCallback, ExitConnectionFragment.ConfirmExit {
//
//    private static final String TAG = HostPartyActivity.class.getName();
//    private ShowSongHostFragment showSongFragment;
//    private SearchBarFragment searchBarFragment;
//    private SearchSongsOutputFragment searchSongsOutputFragment;
//    private ExitConnectionFragment exitConnectionFragment;
//    private SettingsHostFragment settingsHostFragment;
//    private ClientPlaylistFragment clientPlaylistFragment;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_host_party);
//
//        searchBarFragment = new SearchBarFragment(this, getIntent().getStringExtra(Constants.TOKEN));
//        showSongFragment = new ShowSongHostFragment(this);
//        searchSongsOutputFragment = new SearchSongsOutputFragment(this);
//        exitConnectionFragment = new ExitConnectionFragment(this);
//        settingsHostFragment = new SettingsHostFragment(getIntent().getStringExtra(Constants.PASSWORD), getIntent().getStringExtra(Constants.ADDRESS));
//        clientPlaylistFragment = new ClientPlaylistFragment();
//
//        //Todo: Host Fragment erstellen
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.showSongHostFragmentFrame, showSongFragment, "ShowSongHostFragment").commitAllowingStateLoss();
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.searchBarHostFragmentFrame, searchBarFragment, "SearchBarFragment").commitAllowingStateLoss();
//
//    }
//
//    @Override
//    public void searchForSongs(List<Track> tracks) {
//        Log.d("ShowSongFragment", "back to show");
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.showSongHostFragmentFrame, searchSongsOutputFragment, "ShowSongFragment").commitAllowingStateLoss();
//        this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
//    }
//
//
//    @Override
//    public void addSong(Track track) {
//
//    }
//
//    @Override
//    public void denyExit() {
//
//    }
//
//    @Override
//    public void acceptExit() {
//
//    }
//
//    @Override
//    public String getPartyName() {
//        return null;
//    }
//
//    @Override
//    public void openSettingsFragment() {
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.showSongHostFragmentFrame, settingsHostFragment, "SettingsHostFragment").commitAllowingStateLoss();
//    }
//
//    @Override
//    public void openPeopleFragment() {
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.showSongHostFragmentFrame, settingsHostFragment, "SettingsHostFragment").commitAllowingStateLoss();
//    }
//
//    @Override
//    public void openPlaylistFragment() {
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.showSongHostFragmentFrame, clientPlaylistFragment, "SettingsHostFragment").commitAllowingStateLoss();
//    }
//
//    @Override
//    public void openExitFragment() {
//        getSupportFragmentManager().beginTransaction().
//                replace(R.id.showSongHostFragmentFrame, exitConnectionFragment, "ExitConnectionFragment").commitAllowingStateLoss();
//    }
//}