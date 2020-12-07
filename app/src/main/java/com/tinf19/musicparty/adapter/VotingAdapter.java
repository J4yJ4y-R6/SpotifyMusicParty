package com.tinf19.musicparty.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tinf19.musicparty.R;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.HostVoting;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

import org.w3c.dom.Text;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VotingAdapter
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class VotingAdapter extends RecyclerView.Adapter<VotingAdapter.MyViewHolder> {

    private static final String TAG = VotingAdapter.class.getName();
    private List<Voting> mDataset;
    private final VotingAdapterCallback votingAdapterCallback;
    private Context context;


    public interface VotingAdapterCallback {
        Thread getCurrentThread();
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
            this.itemView = itemView;
        }
    }


    /**
     * Constructor to set all currently opened votings in a local ArrayList
     * @param mDataset ArrayList with all currently opened votings
     * @param votingAdapterCallback Communication callback for
     *                              {@link com.tinf19.musicparty.fragments.VotingFragment}
     */
    public VotingAdapter(List<Voting> mDataset, VotingAdapterCallback votingAdapterCallback) {
        this.mDataset = mDataset;
        this.votingAdapterCallback = votingAdapterCallback;
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

        if(songtitleTV != null)
            songtitleTV.setText(mDataset.get(position).getTrack().getName());
        if(artistTV != null)
            artistTV.setText(mDataset.get(position).getTrack().getArtist(0).getName());
        if(coverTV != null)
            coverTV.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.icon_cover_placeholder));
        if(yesButton != null)
            yesButton.setOnClickListener(v -> {
                mDataset.get(position).addVoting(Constants.YES, votingAdapterCallback.getCurrentThread());
                closeVoting(holder, position);
            });
        if(noButton != null)
            noButton.setOnClickListener(v -> {
                mDataset.get(position).addVoting(Constants.NO, votingAdapterCallback.getCurrentThread());
                closeVoting(holder, position);
            });
        if(ignoreVoteButton != null)
            ignoreVoteButton.setOnClickListener(v -> {
                mDataset.get(position).addVoting(Constants.IGNORED, votingAdapterCallback.getCurrentThread());
                closeVoting(holder, position);
            });
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    /**
     * Animating the card upwards after voting
     * @param holder Card View
     * @param position Voting position in the dataset
     */
    private void closeVoting(MyViewHolder holder, int position) {
        holder.itemView.animate()
                .alpha(0f)
                .translationY(-500)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        holder.itemView.setVisibility(View.INVISIBLE);
                        mDataset.remove(position);
                        notifyDataSetChanged();
                    }
                });
    }

    /**
     * Set a List of all currently opened votings for Que and for Skip
     * @param votings List of all currently opened votings
     */
    public void setDataset(List<Voting> votings, Type type) {
        this.mDataset = votings;
    }
}
