package com.tinf19.musicparty.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.ramotion.cardslider.CardSliderLayoutManager;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.DownloadImageTask;
import com.tinf19.musicparty.util.HostVoting;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VotingAdapter
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class VotingAdapter extends RecyclerView.Adapter<VotingAdapter.MyViewHolder> {

    private static final String TAG = VotingAdapter.class.getName();
    private final VotingAdapterCallback votingAdapterCallback;
    private final VotingAdapterToFragmentCallback votingAdapterToFragmentCallback;
    private List<Voting> mDataset;
    private Context context;
    private Map<Integer, Integer> votingPositions = new HashMap<>();

    public interface VotingAdapterCallback {
        Thread getCurrentThread();
    }

    public interface VotingAdapterToFragmentCallback {
        void showVotedSnackbar(int vote);
    }


    /**
     * This ViewHolder is assigning the objects from row_host_voting.xml to the global view-variables
     * @see RecyclerView.ViewHolder
     * @see TextView
     * @see ImageView
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView votingCardSongTitleTextView;
        private final TextView votingCardArtistTextView;
        private final ImageView votingCardCoverImageView;
        private final ImageView voteYesButton;
        private final ImageView voteNoButton;
        private final ImageView ignoreVoteImageView;
        private final LinearLayout votePercentageLinearLayout;
        private final LinearLayout yesVotePercentage;
        private final LinearLayout noVotePercentage;
        private final LinearLayout ignoredVotePercentage;
        private final View itemView;

        /**
         * Constructor to assign the parent view of each row
         * @param itemView parent view from row_host_voting.xml
         */
        public MyViewHolder(View itemView) {
            super(itemView);
            votingCardSongTitleTextView = itemView.findViewById(R.id.votingCardSongTitleTextView);
            votingCardArtistTextView = itemView.findViewById(R.id.votingCardArtistTextView);
            votingCardCoverImageView = itemView.findViewById(R.id.votingCardSongCoverImageView);
            voteYesButton = itemView.findViewById(R.id.yesVoteImageView);
            voteNoButton = itemView.findViewById(R.id.noVoteImageView);
            ignoreVoteImageView = itemView.findViewById(R.id.ignoreVoteImageView);
            votePercentageLinearLayout = itemView.findViewById(R.id.votePercentageLinearLayout);
            yesVotePercentage = itemView.findViewById(R.id.yesVotePercentage);
            noVotePercentage = itemView.findViewById(R.id.noVotePercentage);
            ignoredVotePercentage = itemView.findViewById(R.id.ignoreVotePercentage);
            this.itemView = itemView;
        }
    }


    /**
     * Constructor to set all currently opened votings in a local ArrayList
     * @param mDataset ArrayList with all currently opened votings
     * @param votingAdapterCallback Communication callback for
     *                              {@link com.tinf19.musicparty.fragments.VotingFragment}
     */
    public VotingAdapter(List<Voting> mDataset, VotingAdapterCallback votingAdapterCallback,
                         VotingAdapterToFragmentCallback votingAdapterToFragmentCallback) {
        this.mDataset = mDataset;
        this.votingAdapterCallback = votingAdapterCallback;
        this.votingAdapterToFragmentCallback = votingAdapterToFragmentCallback;
    }



    //Android lifecycle methods

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View textView = inflater.inflate(R.layout.row_host_voting, parent, false);
        return new MyViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        TextView songtitleTV = holder.votingCardSongTitleTextView;
        TextView artistTV = holder.votingCardArtistTextView;
        ImageView coverTV = holder.votingCardCoverImageView;
        ImageView yesButton = holder.voteYesButton;
        ImageView noButton = holder.voteNoButton;
        ImageView ignoreVoteButton = holder.ignoreVoteImageView;

        votingPositions.put(mDataset.get(position).getId(), position);
        Log.d(TAG, "position: " + position + ", name: " + mDataset.get(position).getTrack().getName());

        Log.d(TAG, "name: " + mDataset.get(position).getTrack().getName() + " voted: " + mDataset.get(position).isVoted(votingAdapterCallback.getCurrentThread()));
        if(mDataset.get(position).isVoted(votingAdapterCallback.getCurrentThread()))
            showVotingResult(holder, position);
        else
            hideVotingResult(holder, position);
        if(songtitleTV != null)
            songtitleTV.setText(mDataset.get(position).getTrack().getName());
        if(artistTV != null)
            artistTV.setText(mDataset.get(position).getTrack().getArtist(0).getName());
        if(coverTV != null)
            new DownloadImageTask(coverTV).execute("https://i.scdn.co/image/" + mDataset.get(position).getTrack().getCoverFull());
        if(yesButton != null)
            yesButton.setOnClickListener(v -> {
                mDataset.get(position).addVoting(Constants.YES, votingAdapterCallback.getCurrentThread());
                votingAdapterToFragmentCallback.showVotedSnackbar(Constants.YES);
                showVotingResult(holder, position);
            });
        if(noButton != null)
            noButton.setOnClickListener(v -> {
                mDataset.get(position).addVoting(Constants.NO, votingAdapterCallback.getCurrentThread());
                votingAdapterToFragmentCallback.showVotedSnackbar(Constants.NO);
                showVotingResult(holder, position);
            });
        if(ignoreVoteButton != null)
            ignoreVoteButton.setOnClickListener(v -> {
                mDataset.get(position).addVoting(Constants.IGNORED, votingAdapterCallback.getCurrentThread());
                votingAdapterToFragmentCallback.showVotedSnackbar(Constants.IGNORED);
                setAnimation(holder, position);
            });
    }


    /**
     * Animate the card itemView and remove the item after ignoring it
     * @param holder Card of the item
     * @param position Position of the item
     */
    private void setAnimation(MyViewHolder holder, int position) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.voting_animation);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mDataset.remove(position);
                    VotingAdapter.this.notifyDataSetChanged();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            holder.itemView.startAnimation(animation);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    /**
     * Set a List of all currently opened votings for Que and for Skip
     * @param votings List of all currently opened votings
     */
    public void setDataset(List<Voting> votings) {
        this.mDataset = votings;
    }

    /**
     * Display the voting result in the percentage bar.
     * @param holder Card of the item
     * @param position Position of the item
     */
    private void showVotingResult( MyViewHolder holder, int position) {
        if(mDataset.size() > position) {
            Voting voting = mDataset.get(position);
            LinearLayout votePercentageLL = holder.votePercentageLinearLayout;
            LinearLayout yesVoteLL = holder.yesVotePercentage;
            LinearLayout noVoteLL = holder.noVotePercentage;
            LinearLayout ignoredVoteLL = holder.ignoredVotePercentage;
            holder.voteYesButton.setVisibility(View.GONE);
            holder.voteYesButton.setVisibility(View.GONE);
            holder.voteNoButton.setVisibility(View.GONE);
            votePercentageLL.setVisibility(View.VISIBLE);
            votePercentageLL.setClipToOutline(true);
            yesVoteLL.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    (int) (35 * context.getResources().getDisplayMetrics().density),
                    voting.getVoteListSizes()[0]));
            noVoteLL.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    (int) (35 * context.getResources().getDisplayMetrics().density),
                    voting.getVoteListSizes()[1]));
            ignoredVoteLL.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    (int) (35 * context.getResources().getDisplayMetrics().density),
                    voting.getVoteListSizes()[2]));
        }
    }

    private void hideVotingResult(MyViewHolder holder, int position) {
        holder.votePercentageLinearLayout.setVisibility(View.GONE);
        holder.voteYesButton.setVisibility(View.VISIBLE);
        holder.voteNoButton.setVisibility(View.VISIBLE);
    }

    /**
     * @param id Voting-Id
     * @return Get the position of a voting at the local Map
     */
    public int getVotingPosition(int id) {
        Integer position = votingPositions.get(id);
        return position != null ? position : -1;
    }

    public void removeItemFromDataset(int position) {
        mDataset.remove(position);
        notifyDataSetChanged();
    }
}
