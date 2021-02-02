package com.tinf19.musicparty.adapter;

import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.util.Constants;

import org.jetbrains.annotations.NotNull;

/**
 * SearchSongsSoutputItemTouchHelper is managing the touch events in the RecyclerView of
 * {@link com.tinf19.musicparty.fragments.SearchSongsOutputFragment}
 * It enables the possibility to add a song to queue by swiping it to the right
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class SearchSongsOutputItemTouchHelper extends ItemTouchHelper.Callback {

    private final SearchSongOutputItemTouchHelperCallback mAdapter;
    private final SearchSongsOutputAdapter adapter;

    public interface SearchSongOutputItemTouchHelperCallback {
        void sendToPlaylist(int position);
    }

    public interface SearchSongOutputItemTouchHelperViewHolderCallback {
        void onItemSelected();
        void onItemClear();
    }

    /**
     * Constructor
     * @param adapter Adapter of the RecyclerView in {@link com.tinf19.musicparty.fragments.SearchSongsOutputFragment}
     */
    public SearchSongsOutputItemTouchHelper(SearchSongOutputItemTouchHelperCallback adapter) {
        this.mAdapter = adapter;
        this.adapter = (SearchSongsOutputAdapter) adapter;
    }


    //Android movement methods

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.sendToPlaylist(viewHolder.getAdapterPosition());
        adapter.notifyItemChanged(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(@NotNull Canvas c, @NotNull RecyclerView recyclerView,
                            @NotNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            final float alpha = Constants.ALPHA_FULL - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
            viewHolder.itemView.setAlpha(alpha);
            viewHolder.itemView.setTranslationX(dX);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX , dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof SearchSongOutputItemTouchHelperViewHolderCallback) {
                SearchSongOutputItemTouchHelperViewHolderCallback itemViewHolder =
                        (SearchSongOutputItemTouchHelperViewHolderCallback) viewHolder;
                itemViewHolder.onItemSelected();
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NotNull RecyclerView recyclerView,
                          @NotNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        viewHolder.itemView.setAlpha(Constants.ALPHA_FULL);

        if (viewHolder instanceof SearchSongOutputItemTouchHelperViewHolderCallback) {
            SearchSongOutputItemTouchHelperViewHolderCallback itemViewHolder = (SearchSongOutputItemTouchHelperViewHolderCallback) viewHolder;
            itemViewHolder.onItemClear();
        }
    }
}
