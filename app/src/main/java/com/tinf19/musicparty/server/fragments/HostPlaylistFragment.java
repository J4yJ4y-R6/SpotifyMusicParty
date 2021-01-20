package com.tinf19.musicparty.server.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;
import com.tinf19.musicparty.server.adapter.HostPlaylistItemMoveHelper;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.DownloadImageTask;
import com.tinf19.musicparty.server.adapter.HostPlaylistAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment where the host can see the current queue state. Also he can change it by swapping two
 * items or deleting one by swiping it to the left and commit this action with a click on the trash
 * can. These actions are managed by the
 * {@link com.tinf19.musicparty.server.adapter.HostPlaylistItemMoveHelper}
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostPlaylistFragment extends Fragment {

    private static final String TAG = HostPlaylistFragment.class.getName();
    private TextView currentSongTitleTextView;
    private TextView currentSongArtistTextView;
    private ImageView currentSongCoverImageView;
    private HostPlaylistAdapter hostPlaylistAdapter;
    private HostPlaylistCallback hostPlaylistCallback;
    private HostPlaylistAdapter.HostPlaylistAdapterCallback hostPlaylistAdapterCallback;
    private HostPlaylistItemMoveHelper hostPlaylistItemMoveHelper;
    private int displayWidth;

    public interface HostPlaylistCallback {
        void showPlaylist();
        Track getCurrentPlaying();
    }

    /**
     * Constructor to set the callbacks
     * @param hostPlaylistCallback Communication callback for
     *                             {@link com.tinf19.musicparty.server.HostActivity}.
     * @param hostPlaylistAdapterCallback Communication callback which is given by the
     *                                    {@link HostPlaylistAdapter}
     */
    public HostPlaylistFragment(int displayWidth, HostPlaylistCallback hostPlaylistCallback, HostPlaylistAdapter.HostPlaylistAdapterCallback hostPlaylistAdapterCallback) {
        this.hostPlaylistCallback = hostPlaylistCallback;
        this.hostPlaylistAdapterCallback = hostPlaylistAdapterCallback;
        this.displayWidth = displayWidth;
    }

    /**
     * Empty-Constructor which is necessary at fragments
     */
    public HostPlaylistFragment() { }



    //Android lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        hostPlaylistCallback.showPlaylist();
        Track currentPlaying = hostPlaylistCallback.getCurrentPlaying();
        if(currentPlaying != null) {
            Log.d(TAG, "set current track to " + currentPlaying.getName());
            if (currentSongTitleTextView != null)
                currentSongTitleTextView.setText(currentPlaying.getName());
            if (currentSongArtistTextView != null)
                currentSongArtistTextView.setText(currentPlaying.getArtist(0).getName());
            if (currentSongCoverImageView != null) {
                String coverURL = Constants.IMAGE_URI + currentPlaying.getCoverFull();
                new DownloadImageTask(currentSongCoverImageView).execute(coverURL);
            }
        } else {
            Log.d(TAG, "no song has been started yet");
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof HostPlaylistAdapter.HostPlaylistAdapterCallback) {
            hostPlaylistAdapterCallback = (HostPlaylistAdapter.HostPlaylistAdapterCallback) context;
        }
        if(context instanceof HostPlaylistCallback)
            hostPlaylistCallback = (HostPlaylistCallback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host_playlist, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.hostPlaylistRecyclerView);
        if(recyclerView != null) {
            hostPlaylistAdapter = new HostPlaylistAdapter(new ArrayList<Track>(), hostPlaylistAdapterCallback);
            hostPlaylistItemMoveHelper = new HostPlaylistItemMoveHelper(hostPlaylistAdapter,
                    getContext(), displayWidth);
            ItemTouchHelper touchHelper = new ItemTouchHelper(hostPlaylistItemMoveHelper);
            touchHelper.attachToRecyclerView(recyclerView);
            recyclerView.setAdapter(hostPlaylistAdapter);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(view.getContext());
            recyclerView.setLayoutManager(layoutManager);
        }

        currentSongTitleTextView = view.findViewById(R.id.currentSongTitleHostTextView);
        currentSongArtistTextView = view.findViewById(R.id.currentSongArtistHostTextView);
        currentSongCoverImageView = view.findViewById(R.id.currentSongCoverHostImageView);

        return view;
    }

    /**
     * Display the current queue state with all songs currently in the queue.
     * @param tracks {@link List} of type {@link Track} of all songs currently in the queue
     */
    public void showResult(List<Track> tracks) {
        if(hostPlaylistAdapter != null) {
            Log.d(TAG, "playlist has been updated with new size: " + tracks.size());
            hostPlaylistAdapter.setDataset(tracks);
            hostPlaylistAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Reloading the fragment because the queue has been updated by the client.
     */
    public void updateRecyclerView() {
        Log.d(TAG, "playlist has been updated - notify RecyclerView");
        hostPlaylistAdapter.notifyDataSetChanged();
    }
}