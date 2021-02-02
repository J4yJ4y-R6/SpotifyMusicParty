package com.tinf19.musicparty.util;

import android.util.Log;

import com.tinf19.musicparty.music.Track;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Voting class for the client with less information about the voting as the {@link HostVoting}.
 * Every voting has their current results fetched to the local variables after submitting a vote.
 * Also they get updated after someone submitted a voting and the client is currently in the
 * {@link com.tinf19.musicparty.fragments.VotingFragment}.
 */
public class ClientVoting implements Voting {

    private static final String TAG = ClientVoting.class.getName();
    private final Track track;
    private final Type type;
    private final int id;
    private final ClientVotingCallback clientVotingCallback;
    private boolean voted = false;
    private int yes;
    private int no;
    private int grey;

    public interface ClientVotingCallback {
        void submitVoting(int vote, int id);
    }

    /**
     * Constructor to initialize the variables after getting it from the host as a Json-String.
     * @param json Json-String with all information about the voting
     * @param clientVotingCallback Communication callback for
     *                             {@link com.tinf19.musicparty.client.ClientActivity}
     * @throws JSONException when the creation of the JSONObject failed
     */
    public ClientVoting(String json, ClientVotingCallback clientVotingCallback) throws JSONException {
        JSONObject tempObject = new JSONObject(json);
        this.track = new Track(tempObject.getString(Constants.TRACK));
        this.type = tempObject.getString(Constants.TYPE).equals("QUEUE") ? Type.QUEUE : Type.SKIP;
        this.id = tempObject.getInt(Constants.ID);
        this.yes = tempObject.getInt(Constants.YES_VOTE);
        this.no = tempObject.getInt(Constants.NO_VOTE);
        this.grey = tempObject.getInt(Constants.GREY_VOTE);
        this.voted = tempObject.getBoolean(Constants.HAS_VOTED);
        this.clientVotingCallback = clientVotingCallback;
    }

    /**
     * Updating the voting results after fetching them from the server
     * @param yes Count of yes-votes
     * @param no Count of no-votes
     * @param grey Count of all votes or threads who did not voted yet
     */
    public void updateVotingResult(int yes, int no, int grey) {
        this.yes = yes;
        this.no = no;
        this.grey = grey;
    }

    @Override
    public void addVoting(int vote, Thread thread) {
        this.voted = true;
        switch (vote) {
            case Constants.YES:
                yes++;
                grey--;
                break;
            case Constants.NO:
                Log.d(TAG, "No: " + no + " Grey: " + grey);
                no++;
                grey--;
                Log.d(TAG, "No: " + no + " Grey: " + grey);
                break;
        }
        clientVotingCallback.submitVoting(vote, id);
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public boolean isVoted(Thread thread) {
        return voted;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int[] getVoteListSizes() {
        int count = yes + no + grey;
        return new int[]{(100 * yes)/count, (100 * no)/count, (100 * grey)/count};
    }

    @Override
    public int getId() {
        return id;
    }
}
