package com.tinf19.musicparty.server.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.ButtonState;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG;
import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE;


/**
 * The HostPlaylistItemMoveHelper is managing all touch events in the RecylcerView of
 * {@link com.tinf19.musicparty.server.fragments.HostPlaylistFragment}.
 * It enables the host to delete songs from the queue by swiping to the left and confirm this by
 * clicking the trash can. Also by holding the item and swapping it with another one, the host can
 * change the sequence of the queue.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class HostPlaylistItemMoveHelper extends ItemTouchHelper.Callback {

    private final HostPlaylistItemMoveHelperCallback mAdapter;
    private final HostPlaylistAdapter adapter;
    private boolean swipeBack = false;
    private ButtonState buttonShowedState = ButtonState.GONE;
    private RectF buttonInstance = null;
    private RecyclerView.ViewHolder currentItemViewHolder = null;
    private static final float buttonWidth = 200;
    private final Context context;
    private final int displayWidth;

    public interface HostPlaylistItemMoveHelperCallback {
        void onRowMoved(int fromPosition, int toPosition);
        void onRowSelected(HostPlaylistAdapter.MyViewHolder myViewHolder);
        void onRowClear(HostPlaylistAdapter.MyViewHolder myViewHolder);
        void onRowDeleted(int position);
    }

    /**
     * Constructor to assign the {@link RecyclerView.Adapter} which gets managed by this helper
     * @param adapter {@link RecyclerView.Adapter} which is assigning all songs to the view
     */
    public HostPlaylistItemMoveHelper(HostPlaylistItemMoveHelperCallback adapter, Context context,
                                      int displayWidth) {
        this.mAdapter = adapter;
        this.adapter = (HostPlaylistAdapter) adapter;
        this.context = context;
        this.displayWidth = displayWidth;
    }


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
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mAdapter.onRowDeleted(viewHolder.getAdapterPosition());
        adapter.notifyItemChanged(viewHolder.getAdapterPosition());
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
        if(actionState == ACTION_STATE_DRAG) {
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
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
/*        if (actionState == ACTION_STATE_SWIPE) {
            if (buttonShowedState != ButtonState.GONE) {
                if (buttonShowedState == ButtonState.RIGHT_VISIBLE) dX = (Math.min(dX, -buttonWidth) / 10);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            else {*/

//                dX = dX / 10;
/*            }
        }
        else dX = dX / 10;*/
        setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        currentItemViewHolder = viewHolder;
    }

    private void setTouchListener(Canvas c, RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                  int actionState, boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
            if(actionState == ACTION_STATE_SWIPE) {
                int color = Color.RED;
                viewHolder.itemView.setBackgroundColor(Color.argb((int) ((dX / displayWidth) * (-255)), Color.red(color), Color.green(color), Color.blue(color)));
            }
            if (swipeBack) {
                if (buttonShowedState != ButtonState.GONE && actionState == ACTION_STATE_SWIPE) {
                    setTouchDownListener(c, recyclerView, viewHolder, 0F, dY, actionState, isCurrentlyActive);
                    setItemsClickable(recyclerView, false);
                }
            }
            return false;
        });
    }

    private void setTouchDownListener(final Canvas c, final RecyclerView recyclerView,
                                      final RecyclerView.ViewHolder viewHolder, final float dX,
                                      final float dY, final int actionState,
                                      final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTouchUpListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            return false;
        });
        recyclerView.performClick();
    }

    private void setTouchUpListener(final Canvas c, final RecyclerView recyclerView,
                                    final RecyclerView.ViewHolder viewHolder, final float dX,
                                    final float dY, final int actionState,
                                    final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                HostPlaylistItemMoveHelper.super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                recyclerView.setOnTouchListener((v1, event1) -> false);
                setItemsClickable(recyclerView, true);
                swipeBack = false;

                if (buttonInstance != null && buttonInstance.contains(event.getX(), event.getY())) {
                    if (buttonShowedState == ButtonState.RIGHT_VISIBLE) {
                        mAdapter.onRowDeleted(viewHolder.getAdapterPosition());
                    }
                }
                buttonShowedState = ButtonState.GONE;
                currentItemViewHolder = null;
            }
            return false;
        });
        recyclerView.performClick();
    }



    private void setItemsClickable(RecyclerView recyclerView, boolean isClickable) {
        for(int i = 0; i < recyclerView.getChildCount(); ++i) {
            recyclerView.getChildAt(i).setClickable(isClickable);
        }
    }
}
