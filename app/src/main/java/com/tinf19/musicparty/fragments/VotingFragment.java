package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.CardSnapHelper;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.adapter.VotingAdapter;
import com.tinf19.musicparty.util.Constants;
import com.tinf19.musicparty.util.DisplayMessages;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fragment where the the host or the client can see all currently open votings. Votings are only
 * used when the host selected the party type "Vote". While this type is selected, all queue-
 * request will open a voting and then all clients are able to vote whether they want to listen to
 * the song or not.
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class VotingFragment extends Fragment implements VotingAdapter.VotingAdapterToFragmentCallback {

    private VotingAdapter.VotingAdapterCallback votingAdapterCallback;
    private VotingCallback votingCallback;
    private VotingAdapter votingQueAdapter;
    private VotingAdapter votingSkipAdapter;
    private TextView queueHeaderTextView;
    private TextView skipHeaderTextView;
    private View view;

    public interface VotingCallback {
        List<Voting> getVotings();
        void stopTimer();
    }

    /**
     * Constructor to set the callback
     * @param votingAdapterCallback Communication callback for
     *                              {@link com.tinf19.musicparty.server.HostService}
     */
    public VotingFragment(VotingAdapter.VotingAdapterCallback votingAdapterCallback,
                          VotingCallback votingCallback) {
        this.votingAdapterCallback = votingAdapterCallback;
        this.votingCallback = votingCallback;
    }


    /**
     * Empty-Constructor which is necessary in fragments
     */
    public VotingFragment() { }



    //Android lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        showVotings(votingCallback.getVotings());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_host_voting, container, false);
        RecyclerView skipVotingRecyclerView = view.findViewById(R.id.skipVotingRecyclerview);
        RecyclerView queueVotingRecyclerView = view.findViewById(R.id.queueVotingRecyclerview);
        queueHeaderTextView = view.findViewById(R.id.queueHeaderTextView);
        skipHeaderTextView = view.findViewById(R.id.skipHeaderTextView);
        CardSliderLayoutManager cardSliderLayoutManager = new CardSliderLayoutManager(
                (int) (50 * getContext().getResources().getDisplayMetrics().density),
                (int) (148 * getContext().getResources().getDisplayMetrics().density),
                0);
        if(queueVotingRecyclerView != null) {
            votingQueAdapter = new VotingAdapter(votingCallback.getVotings().stream().filter(v->
                    v.getType().equals(Type.QUE)).collect(Collectors.toList()),
                    votingAdapterCallback, this);
            queueVotingRecyclerView.setAdapter(votingQueAdapter);
            queueVotingRecyclerView.setHasFixedSize(true);
            queueVotingRecyclerView.setLayoutManager(cardSliderLayoutManager);
            new CardSnapHelper().attachToRecyclerView(queueVotingRecyclerView);
        }
        votingSkipAdapter = new VotingAdapter(votingCallback.getVotings().stream().filter(v ->
                v.getType().equals(Type.SKIP)).collect(Collectors.toList()),
                votingAdapterCallback, this);
        if(skipVotingRecyclerView != null) {
            skipVotingRecyclerView.setAdapter(votingSkipAdapter);
            skipVotingRecyclerView.setHasFixedSize(true);
            skipVotingRecyclerView.setLayoutManager(new CardSliderLayoutManager(
                    (int) (50 * getContext().getResources().getDisplayMetrics().density),
                    (int) (148 * getContext().getResources().getDisplayMetrics().density),
                    0));
            new CardSnapHelper().attachToRecyclerView(skipVotingRecyclerView);
        }
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        votingCallback.stopTimer();
    }


    @Override
    public void showVotedSnackbar(int vote) {
        String voteText = "";
        if(vote == Constants.YES) voteText =  getString(R.string.text_yes);
        else if(vote == Constants.NO) voteText = getString(R.string.text_no);
        else voteText = getString(R.string.text_ignored);
        String snackbarText = getString(R.string.snackbar_votingSubmitted, voteText);
        new DisplayMessages(snackbarText, this.requireView()).makeMessage();
    }


    /**
     * Reloading a single voting card after the current result has changed
     * @param id Voting-Id of the specific voting
     * @param type Voting-Type
     */
    public void notifySingleVote(int id, Type type) {
        if(type.equals(Type.QUE)) {
            int position = votingQueAdapter.getVotingPosition(id);
            if (position >= 0) votingQueAdapter.notifyItemChanged(position);
        } else {
            int position = votingSkipAdapter.getVotingPosition(id);
            if (position >= 0) votingSkipAdapter.notifyItemChanged(position);
        }
    }

    public void removeSingleVote(int id, Type type) {
        if(type.equals(Type.QUE)) {
            int position = votingQueAdapter.getVotingPosition(id);
            if (position >= 0) votingQueAdapter.removeItemFromDataset(position);
        } else {
            int position = votingSkipAdapter.getVotingPosition(id);
            if (position >= 0) votingSkipAdapter.removeItemFromDataset(position);
        }
    }

    public void notifyAllVotes(Type type) {
        if(type == Type.QUE) {
            if(votingQueAdapter != null)
                votingQueAdapter.notifyDataSetChanged();
        } else {
            if(votingSkipAdapter != null)
                votingSkipAdapter.notifyDataSetChanged();
        }
    }

    public void addItemToDataset(Voting voting) {
        if(voting.getType() == Type.QUE) {
            if(votingQueAdapter != null)
                votingQueAdapter.addToDataset(voting);
        } else {
            if(votingSkipAdapter != null)
                votingSkipAdapter.addToDataset(voting);
        }
    }


    /**
     * Display the result bar with the current result of all votings where the client or the host
     * has submitted his vote.
     * @param votings List of all votings where the client or the host has submitted his vote.
     */
    public void showVotings(List<Voting> votings) {
        Log.d(VotingFragment.class.getName(), "votings has been updated: " + votings.size());
        if(votingQueAdapter != null) {
            List<Voting> filteredVotings = votings.stream().filter(v->
                    v.getType().equals(Type.QUE)).collect(Collectors.toList());
            votingQueAdapter.setDataset(filteredVotings);
            queueHeaderTextView.setText(view.getContext().getString(filteredVotings.size() == 0 ?
                    R.string.text_noCurrentQueVotings : R.string.text_currentQueVotings));
            votingQueAdapter.notifyDataSetChanged();
        }
        if(votingSkipAdapter != null) {
            List<Voting> filteredVotings = votings.stream().filter(v->
                    v.getType().equals(Type.SKIP)).collect(Collectors.toList());
            votingSkipAdapter.setDataset(filteredVotings);
            skipHeaderTextView.setText(view.getContext().getString(filteredVotings.size() == 0 ?
                    R.string.text_noCurrentSkipVotings : R.string.text_currentSkipVotings));
            votingSkipAdapter.notifyDataSetChanged();
        }
    }
}