package com.tinf19.musicparty.server.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class HostPlaylistItemMoveHelper extends ItemTouchHelper.Callback {

    private final HostPlaylistItemMoveHelperCallback mAdapter;

    public interface HostPlaylistItemMoveHelperCallback {
        void onRowMoved(int fromPosition, int toPosition);
        void onRowSelected(HostPlaylistAdapter.MyViewHolder myViewHolder);
        void onRowClear(HostPlaylistAdapter.MyViewHolder myViewHolder);
        void onRowDeleted(int position);
    }

    public HostPlaylistItemMoveHelper(HostPlaylistItemMoveHelperCallback adapter) { this.mAdapter = adapter; }


    //Android move methods

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.LEFT;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if(actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if(viewHolder instanceof HostPlaylistAdapter.MyViewHolder) {
                HostPlaylistAdapter.MyViewHolder myViewHolder = (HostPlaylistAdapter.MyViewHolder) viewHolder;
                mAdapter.onRowSelected(myViewHolder);
            }
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if(viewHolder instanceof HostPlaylistAdapter.MyViewHolder) {
            HostPlaylistAdapter.MyViewHolder myViewHolder = (HostPlaylistAdapter.MyViewHolder) viewHolder;
            mAdapter.onRowClear(myViewHolder);
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.onRowDeleted(viewHolder.getAdapterPosition());
    }
}
