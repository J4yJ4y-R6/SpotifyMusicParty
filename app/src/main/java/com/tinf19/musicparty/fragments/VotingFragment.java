package com.tinf19.musicparty.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ramotion.cardslider.CardSliderLayoutManager;
import com.ramotion.cardslider.CardSnapHelper;
import com.tinf19.musicparty.R;
import com.tinf19.musicparty.adapter.VotingAdapter;
import com.tinf19.musicparty.util.Type;
import com.tinf19.musicparty.util.Voting;

import java.util.List;
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
public class VotingFragment extends Fragment {

    private VotingAdapter.VotingAdapterCallback votingAdapterCallback;
    private VotingCallback votingCallback;
    private VotingAdapter votingQueAdapter;
    private VotingAdapter votingSkipAdapter;
    private TextView queueHeaderTextView;
    private TextView skipHeaderTextView;
    private View view;
    private CardSliderLayoutManager cardSliderLayoutManager;

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
        cardSliderLayoutManager = new CardSliderLayoutManager(
                (int) (50 * getContext().getResources().getDisplayMetrics().density),
                (int) (148 * getContext().getResources().getDisplayMetrics().density),
                0);
        if(queueVotingRecyclerView != null) {
            votingQueAdapter = new VotingAdapter(votingCallback.getVotings().stream().filter(v->
                    v.getType().equals(Type.QUE)).collect(Collectors.toList()),
                    votingAdapterCallback);
            queueVotingRecyclerView.setAdapter(votingQueAdapter);
            queueVotingRecyclerView.setHasFixedSize(true);
            queueVotingRecyclerView.setLayoutManager(cardSliderLayoutManager);
            new CardSnapHelper().attachToRecyclerView(queueVotingRecyclerView);
        }
        votingSkipAdapter = new VotingAdapter(votingCallback.getVotings().stream().filter(v ->
                v.getType().equals(Type.SKIP)).collect(Collectors.toList()),
                votingAdapterCallback);
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

    public void notifySingleVote(int id) {
        int position = votingQueAdapter.getVotingPosition(id);
        Log.d(VotingFragment.class.getName(), "Id: " + id + ", position: " + position);
        if(position >= 0) votingQueAdapter.notifyItemChanged(id-1);
    }


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

    public int getVotingId() {
        return votingQueAdapter.getVotingId(cardSliderLayoutManager.getActiveCardPosition());
    }
}