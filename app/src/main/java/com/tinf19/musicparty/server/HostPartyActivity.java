package com.tinf19.musicparty.server;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.fragments.ExitConnectionFragment;
import com.tinf19.musicparty.fragments.SearchBarFragment;
import com.tinf19.musicparty.fragments.SearchSongsOutputFragment;
import com.tinf19.musicparty.fragments.ShowSongFragment;
import com.tinf19.musicparty.fragments.ShowSongHostFragment;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.util.Constants;

import java.util.List;

public class HostPartyActivity extends AppCompatActivity implements SearchBarFragment.SearchForSongs, ShowSongFragment.PartyButtonClicked, SearchSongsOutputFragment.AddSongCallback, ExitConnectionFragment.ConfirmExit {

    private TextView mTextView;
    private ShowSongHostFragment showSongFragment;
    private SearchBarFragment searchBarFragment;
    private SearchSongsOutputFragment searchSongsOutputFragment;
    private ExitConnectionFragment exitConnectionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_party);

        searchBarFragment = new SearchBarFragment(this, getIntent().getStringExtra(Constants.TOKEN));
        showSongFragment = new ShowSongHostFragment();
        searchSongsOutputFragment = new SearchSongsOutputFragment(this);
        exitConnectionFragment = new ExitConnectionFragment(this);

        //Todo: Host Fragment erstellen
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, showSongFragment, "ShowSongHostFragment").commitAllowingStateLoss();
        getSupportFragmentManager().beginTransaction().
                replace(R.id.searchBarHostFragmentFrame, searchBarFragment, "SearchBarFragment").commitAllowingStateLoss();

    }

    @Override
    public void searchForSongs(List<Track> tracks) {
        Log.d("ShowSongFragment", "back to show");
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, searchSongsOutputFragment, "ShowSongFragment").commitAllowingStateLoss();
        this.runOnUiThread(() -> searchSongsOutputFragment.showResult(tracks));
    }

    @Override
    public void exitConnection() {
        getSupportFragmentManager().beginTransaction().
                replace(R.id.showSongHostFragmentFrame, exitConnectionFragment, "ExitConnectionFragment").commitAllowingStateLoss();
    }

    @Override
    public void showPlaylist() {

    }

    @Override
    public void addSong(Track track) {

    }

    @Override
    public void denyExit() {
        
    }

    @Override
    public void acceptExit() {

    }

    @Override
    public String getPartyName() {
        return null;
    }
}