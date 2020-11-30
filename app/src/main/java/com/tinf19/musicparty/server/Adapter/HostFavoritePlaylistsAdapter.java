package com.tinf19.musicparty.server.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.server.fragments.HostFavoritePlaylistsFragment;
import com.tinf19.musicparty.music.Playlist;
import com.tinf19.musicparty.server.HostActivity;
import com.tinf19.musicparty.util.DownloadImageTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class HostFavoritePlaylistsAdapter extends RecyclerView.Adapter<HostFavoritePlaylistsAdapter.ViewHolder> {

    private static final String TAG = HostFavoritePlaylistsAdapter.class.getName();
    private final GalleryCallback galleryCallback;
    private final HostFavoritePlaylistAdapterCallback hostFavoritePlaylistCallback;
    private final HostFavoritePlaylistsFragment hostFavoritePlaylistsFragment = new HostFavoritePlaylistsFragment();
    private SharedPreferences savePlaylistMemory;
    private ArrayList<Playlist> playlists;
    private Context context;
    private ArrayList<String> idList;

    public interface GalleryCallback {
        void openGalleryForUpload(Intent intent, String playlistID);
    }

    public interface HostFavoritePlaylistAdapterCallback {
        void playFavoritePlaylist(String id, ArrayList<String> idList);
        void changePlaylistName(String name, String id);
        void deletePlaylist(String id);
    }

    public HostFavoritePlaylistsAdapter(ArrayList<Playlist> playlists, GalleryCallback gCallback, HostFavoritePlaylistAdapterCallback fCallback) {
        this.playlists = playlists;
        this.galleryCallback = gCallback;
        this.hostFavoritePlaylistCallback = fCallback;
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



    //Android lifecycle methods

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_host_favorite_playlist, parent, false);
        StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
        if(hostFavoritePlaylistsFragment.getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT)
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
            new Thread(() -> {
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
            }).start();
            View.OnClickListener onclick = v -> {
                if (!(switcher.getCurrentView() instanceof EditText)) {
                    new AlertDialog.Builder((context))
                            .setTitle(name)
                            .setMessage(context.getString(R.string.text_favoritePlaylistsDialogWindow))
                            .setPositiveButton("", (dialog, which) -> new AlertDialog.Builder(context)
                                    .setTitle(context.getString(R.string.text_editPlaylist_dialog))
                                    .setMessage(context.getString(R.string.text_chooseEditOption_dialog))
                                    .setPositiveButton("", (dialog1, which1) -> switcher.showNext())
                                    .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_edit_pen))
                                    .setNegativeButton("", (dialog12, which12) -> {
                                        Log.d(TAG, "open gallery for picking an image");
                                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                        photoPickerIntent.setType("image/*");
                                        galleryCallback.openGalleryForUpload(photoPickerIntent, id);
                                    })
                                    .setNegativeButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_edit_image))
                                    .show())
                            .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_edit_pen))
                            .setNeutralButton("", (dialog, which) -> hostFavoritePlaylistCallback.playFavoritePlaylist(id, idList))
                            .setNeutralButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_play_cycle))
                            .setNegativeButton("", (dialog, which) -> new AlertDialog.Builder(context)
                                    .setTitle(context.getString(R.string.text_delete))
                                    .setMessage(context.getString(R.string.text_chooseDeleteOption_dialog))
                                    .setPositiveButton("", (dialog13, which13) -> { })
                                    .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_x))
                                    .setNegativeButton("", (dialog14, which14) -> {
                                        SharedPreferences.Editor editor = savePlaylistMemory.edit();
                                        Log.d(TAG, "removing " + name + "from SharedPreferences");
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
                                        notifyDataSetChanged();
                                        hostFavoritePlaylistCallback.deletePlaylist(id);
                                    })
                                    .setNegativeButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_check))
                                    .show())
                            .setNegativeButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_trash_can))
                            .show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.text_editPlaylist_dialog)
                            .setMessage(context.getString(R.string.text_acceptEditOption_dialog))
                            .setPositiveButton("", (dialog, which) -> {
                                String newName = headerET.getText().toString();
                                Log.d(TAG, "changed playlist name to " + newName);
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
                                    hostFavoritePlaylistCallback.changePlaylistName(newName, id);
                                    headerTV.setText(newName);
                                }
                                switcher.showNext();
                            })
                            .setPositiveButtonIcon(ContextCompat.getDrawable(context, R.drawable.icon_edit_pen))
                            .show();
                }
            };
            holder.itemView.setOnClickListener(onclick);
            holder.coverImageView.setOnClickListener(onclick);
        }
    }

    @Override
    public int getItemCount() { return playlists.size(); }



    public void setPlaylists(ArrayList<Playlist> playlists, ArrayList<String> idList) {
        this.playlists = playlists;
        this.idList = idList;
    }

    public static int convertToBitmap(URL url_value) throws IOException {
        Bitmap mIcon1 =
                BitmapFactory.decodeStream(url_value.openConnection().getInputStream());
        Palette palette = Palette.from(mIcon1).maximumColorCount(30).generate();
        return palette.getDominantColor(0xFFFFFFFF);
    }
}


