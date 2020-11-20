package com.tinf19.musicparty.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorSpace;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.fragments.ShowSavedPlaylistsFragment;
import com.tinf19.musicparty.music.Playlist;
import com.tinf19.musicparty.server.HostActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class ShowSavedPlaylistRecycAdapter extends RecyclerView.Adapter<ShowSavedPlaylistRecycAdapter.ViewHolder> {

    private SharedPreferences savePlaylistMemory;
    private ArrayList<Playlist> playlists;
    private Context context;
    private ArrayList<String> idList;
    private GalleryCallback galleryCallback;
    private String playlistID;
    private FavoritePlaylistCallback favoritePlaylistCallback;
    private static final String TAG = ShowSavedPlaylistRecycAdapter.class.getName();
    private ShowSavedPlaylistsFragment showSavedPlaylistsFragment = new ShowSavedPlaylistsFragment();
    private View view;

    public interface GalleryCallback {
        void openGalleryForUpload(Intent intent, String playlistID);
    }

    public interface FavoritePlaylistCallback {
        void reloadFavoritePlaylistsFragment();
        void playFavoritePlaylist(String id, ArrayList<String> idList);
        void changePlaylistName(String name, String id);
        void deletePlaylist(String id);
    }

    public ShowSavedPlaylistRecycAdapter(ArrayList<Playlist> playlists, GalleryCallback gCallback, FavoritePlaylistCallback fCallback) {
        this.playlists = playlists;
        this.galleryCallback = gCallback;
        this.favoritePlaylistCallback = fCallback;
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView headerTextView;
        public EditText headerEditText;
        public ImageView coverImageView;
        public ViewSwitcher switcher;


        ViewHolder(View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.favoriteHeaderTextView);
            headerEditText = itemView.findViewById(R.id.favoriteHeaderEditText);
            coverImageView = itemView.findViewById(R.id.favoriteCoverImageView);
            switcher = itemView.findViewById(R.id.favoriteSwitcher);
        }
    }

    public void setPlaylists(ArrayList<Playlist> playlists, ArrayList<String> idList) {
        this.playlists = playlists;
        this.idList = idList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        view = inflater.inflate(R.layout.favorite_grid_cell_layout, parent, false);
        StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        if(showSavedPlaylistsFragment.getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT)
            lp.height = parent.getMeasuredHeight() / 2;

        else
            lp.height = parent.getMeasuredHeight();
        view.setLayoutParams(lp);
        savePlaylistMemory = context.getSharedPreferences("savePlaylistMemory", Context.MODE_PRIVATE);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextView headerTV = holder.headerTextView;
        EditText headerET = holder.headerEditText;
        ImageView coverIV = holder.coverImageView;
        ViewSwitcher switcher = holder.switcher;
        String name = playlists.get(position).getName();
        String coverURL = playlists.get(position).getCoverURL();
        String id = playlists.get(position).getId();
        if (headerTV != null && headerET != null && coverIV != null && switcher != null) {
            holder.headerTextView.setText(name);
            new DownloadImageTask(holder.coverImageView).execute(coverURL);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int color = convertToBitmap(new URL(coverURL));
                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {color, Color.rgb((int)(Color.red(color) * 0.4), (int)(Color.green(color) * 0.4), (int)(Color.blue(color) * 0.4))});
                        gradientDrawable.setCornerRadius(20);
                        ((HostActivity) context).runOnUiThread( () -> {
                            holder.itemView.setBackground(gradientDrawable);
                            int textColor = 0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color) > 130 ? Color.BLACK : Color.WHITE;
                            holder.headerTextView.setTextColor(textColor);
                            holder.headerEditText.setTextColor(textColor);
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            View.OnClickListener onclick = new  View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!(switcher.getCurrentView() instanceof EditText)) {
                        new AlertDialog.Builder((context))
                                .setTitle(name)
                                .setMessage(context.getString(R.string.text_favoritePlaylistsDialogWindow))
                                .setPositiveButton("", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new AlertDialog.Builder(context)
                                                .setTitle(context.getString(R.string.text_editPlaylist_dialog))
                                                .setMessage(context.getString(R.string.text_chooseEditOption_dialog))
                                                .setPositiveButton("", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        switcher.showNext();
                                                    }
                                                })
                                                .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_edit_button))
                                                .setNegativeButton("", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                                        photoPickerIntent.setType("image/*");
                                                        galleryCallback.openGalleryForUpload(photoPickerIntent, id);
                                                    }
                                                })
                                                .setNegativeButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_edit_playlistname_button))
                                                .show();
                                    }
                                })
                                .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_edit_button))
                                .setNeutralButton("", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        favoritePlaylistCallback.playFavoritePlaylist(id, idList);
                                    }
                                })
                                .setNeutralButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_play_track_button))
                                .setNegativeButton("", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        new AlertDialog.Builder(context)
                                                .setTitle(context.getString(R.string.text_delete))
                                                .setMessage(context.getString(R.string.text_chooseDeleteOption_dialog))
                                                .setPositiveButton("", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        //window is closing
                                                    }
                                                })
                                                .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_deny_button))
                                                .setNegativeButton("", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        SharedPreferences.Editor editor = savePlaylistMemory.edit();
                                                        editor.remove("" + position);
                                                        editor.apply();
                                                        if (position < 8) {
                                                            int counter = position;
                                                            while (counter <= 8) {
                                                                String nextPlaylist = savePlaylistMemory.getString("" + (counter + 1), "");
                                                                if (!nextPlaylist.equals(""))
                                                                    editor.putString("" + counter, nextPlaylist);
                                                                else {
                                                                    editor.remove("" + counter);
                                                                }
                                                                editor.apply();
                                                                counter++;
                                                            }
                                                        }
                                                        String toastMessage = name + context.getString(R.string.text_toastPlaylistDeleted);
                                                        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
                                                        favoritePlaylistCallback.reloadFavoritePlaylistsFragment();
                                                        favoritePlaylistCallback.deletePlaylist(id);
                                                    }
                                                })
                                                .setNegativeButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_accept_button))
                                                .show();
                                    }
                                })
                                .setNegativeButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_trash_can_button))
                                .show();
                    } else {
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.text_editPlaylist_dialog)
                                .setMessage(context.getString(R.string.text_acceptEditOption_dialog))
                                .setPositiveButton("", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String newName = headerET.getText().toString();
                                        if (!newName.equals("")) {
                                            JSONObject playlist = new JSONObject();
                                            try {
                                                playlist.put("name", newName);
                                                playlist.put("id", id);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            SharedPreferences.Editor editor = savePlaylistMemory.edit();
                                            editor.putString("" + position, playlist.toString());
                                            editor.apply();
                                            favoritePlaylistCallback.changePlaylistName(newName, id);
                                            headerTV.setText(newName);
                                        }
                                        switcher.showNext();
                                    }
                                })
                                .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.ic_edit_button))
                                .show();
                    }
                }
            };
            holder.itemView.setOnClickListener(onclick);
            holder.coverImageView.setOnClickListener(onclick);
        }
    }

    public static int convertToBitmap(URL url_value) throws IOException {
        Bitmap mIcon1 =
                BitmapFactory.decodeStream(url_value.openConnection().getInputStream());
        Palette palette = Palette.from(mIcon1).maximumColorCount(30).generate();
        return palette.getDominantColor(0xFFFFFFFF);
    }


    @Override
    public int getItemCount() {
        return playlists.size();
    }
}


