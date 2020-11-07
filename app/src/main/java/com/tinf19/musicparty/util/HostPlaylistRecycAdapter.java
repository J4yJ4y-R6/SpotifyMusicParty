package com.tinf19.musicparty.util;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.music.Track;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HostPlaylistRecycAdapter extends RecyclerView.Adapter<HostPlaylistRecycAdapter.MyViewHolder> implements ItemMoveCallback.ItemTouchHelperContract {

    private List<Track> mdataset;

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView songTitleTextView;
        private TextView songArtistTextView;
        private View rowView;

        public MyViewHolder(View itemView) {
            super(itemView);
            rowView = itemView;
            songTitleTextView = itemView.findViewById(R.id.playlistSongTitleTextView);
            songArtistTextView = itemView.findViewById(R.id.playlistArtistNameTextView);
        }
    }

    public HostPlaylistRecycAdapter(ArrayList<Track> mDataset) {
        this.mdataset = mDataset;
    }

    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.client_playlist_recyc_view_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String title = mdataset.get(position).getName();
        String artist = mdataset.get(position).getArtist(0).getName();
        TextView songTitleTV = holder.songTitleTextView;
        if(songTitleTV != null) {
            songTitleTV.setText(title);
        }
        TextView artistTV = holder.songArtistTextView;
        if(artistTV != null) {
            artistTV.setText(artist);
        }
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        if(fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mdataset, i, i+1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mdataset, i, i-1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onRowSelected(MyViewHolder myViewHolder) {
        myViewHolder.rowView.setBackgroundColor(Color.GRAY);
    }

    @Override
    public void onRowClear(MyViewHolder myViewHolder) {
        myViewHolder.rowView.setBackgroundColor(Color.WHITE);
    }


    @Override
    public int getItemCount() {
        return mdataset.size();
    }

    public void setDataset(List<Track> mDataset) {
        this.mdataset = mDataset;
    }
}
